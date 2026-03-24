package net.battaglini.fantaf1appbackend.task

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import net.battaglini.fantaf1appbackend.client.OpenF1Client
import net.battaglini.fantaf1appbackend.configuration.ChannelConfiguration
import net.battaglini.fantaf1appbackend.configuration.ResultsCalculatorProperties
import net.battaglini.fantaf1appbackend.enums.TaskType
import net.battaglini.fantaf1appbackend.model.*
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1MeetingResponse.Companion.toRace
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1SessionResponse.Companion.toRaceWeekendSession
import net.battaglini.fantaf1appbackend.repository.DriverRepository
import net.battaglini.fantaf1appbackend.repository.RaceWeekendResultRepository
import net.battaglini.fantaf1appbackend.service.PracticeResultsService
import net.battaglini.fantaf1appbackend.service.QualifyingResultsService
import net.battaglini.fantaf1appbackend.service.RaceResultsService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Component
class RaceWeekendResultsCalculatorTask(
    private val resultsCalculatorProperties: ResultsCalculatorProperties,
    private val practiceResultsService: PracticeResultsService,
    private val qualifyingResultsService: QualifyingResultsService,
    private val raceResultsService: RaceResultsService,
    private val raceWeekendResultRepository: RaceWeekendResultRepository,
    private val openF1Client: OpenF1Client,
    private val driverRepository: DriverRepository,
    private val taskChannel: Channel<ChannelConfiguration.Companion.TaskChannelMessage>
) {
    @Scheduled(fixedRate = 180_000, initialDelay = 15_000)
    suspend fun runTask() {
        if (!resultsCalculatorProperties.enable) {
            LOGGER.info("Skipping race weekend results calculation because it is disabled in app config")
            return
        }
        LOGGER.info("Calculating race weekend results")

        val now = Clock.System.now()
        val nowLocal = now.toLocalDateTime(TimeZone.currentSystemDefault())
        try {
            val meeting = openF1Client.getRaces(year = nowLocal.year).firstOrNull { meeting ->
                val endInstant = meeting.dateEnd.toInstant(meeting.gmtOffset)
                val difference = now - endInstant
                difference >= 0.toDuration(DurationUnit.MINUTES) && difference < 6.toDuration(DurationUnit.DAYS)
            }
            if (meeting == null) {
                LOGGER.info("No race weekends found within 0 and 6 days before today")
                return
            }
            val existingResults =
                raceWeekendResultRepository.findRaceWeekendResult(openF1MeetingKey = meeting.meetingKey)

            if (existingResults != null) {
                LOGGER.info(
                    "Found raceWeekend result for raceId={} raceName={}. Exiting...",
                    existingResults.raceId,
                    meeting.meetingName
                )
                return
            }
            LOGGER.info("No results found for raceName={}. Starting calculation...", meeting.meetingName)

            val sessions = openF1Client.getSessions(meetingKey = meeting.meetingKey).map {
                it.toRaceWeekendSession(
                    sessionId = generateSessionId(meeting.meetingKey, it.sessionKey)
                )
            }.toList()
            val raceWeekend = meeting.toRace(
                raceId = generateRaceId(meeting.meetingKey, meeting.year), sessions = sessions
            )
            val combinedPracticeResults =
                practiceResultsService.getDriversResultsForCombinedPractice(raceWeekend).toList()
            delay(2000)
            val qualifyingResults = qualifyingResultsService
                .getDriversResultsForQualifying(raceWeekend, false)
                .toList()
            delay(2000)
            val sprintQualifyingResults = qualifyingResultsService
                .getDriversResultsForQualifying(raceWeekend, true)
                .toList()
            delay(2000)
            val raceResults = raceResultsService.getResultsForRace(raceWeekend, false).toList()
            delay(2000)
            val sprintRaceResults = raceResultsService.getResultsForRace(raceWeekend, true).toList()

            if (combinedPracticeResults.isEmpty() || qualifyingResults.isEmpty() || raceResults.isEmpty()) {
                LOGGER.warn(
                    "Could not calculate minimum set of results for raceId={}, raceName={}. This could be due to not all results being available yet",
                    raceWeekend.raceId,
                    raceWeekend.raceName
                )
                return
            }

            val raceWeekendResult = calculateRaceWeekendResults(
                combinedPracticeResults,
                qualifyingResults,
                sprintQualifyingResults,
                raceResults,
                sprintRaceResults,
                raceWeekend
            )

            if (resultsCalculatorProperties.dryRun) {
                LOGGER.info(
                    """
                    DRY RUN: race weekend results for ${raceWeekend.raceName}
                    ${raceWeekendResult.toString()}
                """.trimIndent()
                )
            } else {
                raceWeekendResultRepository.saveRaceWeekendResult(raceWeekendResult)
            }

            taskChannel.send(
                ChannelConfiguration.Companion.TaskChannelMessage(
                    TaskType.RACE_WEEKEND_RESULTS_CALCULATION_COMPLETED,
                    raceWeekendResult
                )
            )

            LOGGER.info("Finished calculating race weekend results")
        } catch (e: Exception) {
            LOGGER.error("Error calculating race weekend results", e)
        }
    }

    suspend fun calculateRaceWeekendResults(
        driverPracticeResults: List<DriverPracticeResult>,
        driverQualifyingResults: List<DriverQualifyingResult>,
        driverSprintQualifyingResults: List<DriverQualifyingResult>,
        driverRaceResults: List<DriverRaceResult>,
        driverSprintRaceResults: List<DriverRaceResult>,
        raceWeekend: RaceWeekend
    ): RaceWeekendResult {
        val drivers = driverRepository.getDrivers().toList()

        val practiceResults = driverPracticeResults.sortedBy { it.fastestLap }.mapIndexed { index, result ->
            Pair(result.driverAcronym, mapIndexToPoints(index))
        }
        val qualifyingResults =
            driverQualifyingResults.sortedBy { it.finalPosition }.mapIndexed { index, result ->
                Pair(result.driverAcronym, mapIndexToPoints(index))
            }
        val sprintQualifyingResults =
            driverSprintQualifyingResults.sortedBy { it.finalPosition }.mapIndexed { index, result ->
                Pair(result.driverAcronym, mapIndexToPoints(index))
            }
        val raceResults = driverRaceResults.sortedBy { it.finalPosition }.mapIndexed { index, result ->
            Pair(result.driverAcronym, mapIndexToPoints(index))
        }
        val sprintRaceResults = driverSprintRaceResults.sortedBy { it.finalPosition }.mapIndexed { index, result ->
            Pair(result.driverAcronym, mapIndexToPoints(index))
        }

        val results = drivers.map { driver ->
            val practiceResult = practiceResults.firstOrNull { it.first == driver.acronym }
            val qualifyingResult = qualifyingResults.firstOrNull { it.first == driver.acronym }
            val sprintQualifyingResult =
                sprintQualifyingResults.firstOrNull { it.first == driver.acronym }
            val raceResult = raceResults.firstOrNull { it.first == driver.acronym }
            val sprintRaceResult = sprintRaceResults.firstOrNull { it.first == driver.acronym }
            RaceWeekendResult.Companion.Result(
                driverId = driver.driverId,
                driverNumber = driver.driverNumber,
                driverAcronym = driver.acronym,
                points = calculateMean(
                    practiceResult,
                    qualifyingResult,
                    sprintQualifyingResult,
                    raceResult,
                    sprintRaceResult
                ),
            )
        }

        return RaceWeekendResult(
            raceId = raceWeekend.raceId,
            raceName = raceWeekend.raceName,
            openF1MeetingKey = raceWeekend.openF1MeetingKey,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            version = 1,
            results = results,
            // TODO: change this, passing actual race summary paragraphs
            summaryParagraphs = null
        )
    }

    private fun mapIndexToPoints(index: Int): Double {
        return when (index) {
            0 -> 20.0
            1 -> 17.0
            2 -> 15.0
            3 -> 13.0
            4 -> 11.0
            5 -> 10.0
            6 -> 9.0
            7 -> 8.0
            8 -> 7.0
            9 -> 6.0
            10 -> 5.0
            11 -> 4.0
            12 -> 3.0
            13 -> 2.0
            14 -> 1.0
            else -> 0.0
        }
    }

    private fun calculateMean(
        practiceResult: Pair<String, Double>?,
        qualifyingResult: Pair<String, Double>?,
        sprintQualifyingResult: Pair<String, Double>?,
        raceResult: Pair<String, Double>?,
        sprintRaceResult: Pair<String, Double>?
    ): Double {
        var sum = 0.0
        var dividend = 0.0
        practiceResult?.let {
            sum += it.second
            dividend++
        }
        qualifyingResult?.let {
            sum += it.second
            dividend++
        }
        sprintQualifyingResult?.let {
            sum += it.second
            dividend++
        }
        raceResult?.let {
            sum += it.second
            dividend++
        }
        sprintRaceResult?.let {
            sum += it.second
            dividend++
        }
        return String.format("%.1f", sum / dividend).toDouble()
    }

    fun generateSessionId(meetingKey: Int, sessionKey: Int): String {
        return Uuid.fromULongs(sessionKey.toULong(), meetingKey.toULong()).toString()
    }

    private fun generateRaceId(meetingKey: Int, year: Int): String {
        return Uuid.fromULongs(meetingKey.toULong(), year.toULong()).toString()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(RaceWeekendResultsCalculatorTask::class.java)
    }
}