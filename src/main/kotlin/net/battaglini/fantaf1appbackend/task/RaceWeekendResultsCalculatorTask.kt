package net.battaglini.fantaf1appbackend.task

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1MeetingResponse
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
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
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
    private val taskChannel: Channel<ChannelConfiguration.Companion.TaskChannelMessage>,
    private val clock: Clock
) {
    @Scheduled(fixedRate = 180_000, initialDelay = 15_000)
    suspend fun runTask() {
        if (!resultsCalculatorProperties.enable) {
            LOGGER.info("Skipping race weekend results calculation because it is disabled in app config")
            return
        }

        try {
            val meeting = findCurrentMeeting() ?: return

            if (raceWeekendResultRepository.findRaceWeekendResult(openF1MeetingKey = meeting.meetingKey) != null) {
                LOGGER.info(
                    "Found raceWeekend result for raceId={} raceName={}. Exiting...",
                    meeting.meetingKey,
                    meeting.meetingName
                )
                return
            }

            LOGGER.info("No results found for raceName={}. Starting calculation...", meeting.meetingName)

            val raceWeekend = createRaceWeekend(meeting)

            val combinedPracticeResults = fetchResults { practiceResultsService.getDriversResultsForCombinedPractice(raceWeekend) }
            val qualifyingResults = fetchResults { qualifyingResultsService.getDriversResultsForQualifying(raceWeekend, false) }
            val sprintQualifyingResults = fetchResults { qualifyingResultsService.getDriversResultsForQualifying(raceWeekend, true) }
            val raceResults = fetchResults { raceResultsService.getResultsForRace(raceWeekend, false) }
            val sprintRaceResults = fetchResults { raceResultsService.getResultsForRace(raceWeekend, true) }

            if (combinedPracticeResults.isEmpty() || qualifyingResults.isEmpty() || raceResults.isEmpty()) {
                LOGGER.warn(
                    "Could not calculate minimum set of results for raceId={}, raceName={}. Results might not be available yet",
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

            saveAndNotify(raceWeekendResult)
            LOGGER.info("Finished calculating race weekend results for {}", raceWeekend.raceName)
        } catch (e: Exception) {
            LOGGER.error("Error calculating race weekend results", e)
        }
    }

    private suspend fun findCurrentMeeting(): OpenF1MeetingResponse? {
        val now = clock.now()
        val nowLocal = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val meeting = openF1Client.getRaces(year = nowLocal.year).firstOrNull { meeting ->
            val endInstant = meeting.dateEnd.toInstant(meeting.gmtOffset)
            val difference = now - endInstant
            difference >= 0.minutes && difference < 6.days
        }
        if (meeting == null) {
            LOGGER.info("No race weekends found within 0 and 6 days before today")
        }
        return meeting
    }

    private suspend fun createRaceWeekend(meeting: OpenF1MeetingResponse): RaceWeekend {
        val sessions = openF1Client.getSessions(meetingKey = meeting.meetingKey).map {
            it.toRaceWeekendSession(sessionId = generateSessionId(meeting.meetingKey, it.sessionKey))
        }.toList()
        return meeting.toRace(raceId = generateRaceId(meeting.meetingKey, meeting.year), sessions = sessions)
    }

    private suspend fun <T> fetchResults(fetcher: suspend () -> Flow<T>): List<T> {
        val results = fetcher().toList()
        delay(2.seconds)
        return results
    }

    private suspend fun saveAndNotify(raceWeekendResult: RaceWeekendResult) {
        if (resultsCalculatorProperties.dryRun) {
            LOGGER.info("DRY RUN: race weekend results for ${raceWeekendResult.raceName}\n$raceWeekendResult")
        } else {
            raceWeekendResultRepository.saveRaceWeekendResult(raceWeekendResult)
            taskChannel.send(
                ChannelConfiguration.Companion.TaskChannelMessage(
                    TaskType.RACE_WEEKEND_RESULTS_CALCULATION_COMPLETED,
                    raceWeekendResult
                )
            )
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

        val practicePoints = driverPracticeResults.sortedBy { it.fastestLap }.mapToPoints()
        val qualifyingPoints =
            driverQualifyingResults.sortedWith(compareBy(nullsLast()) { it.finalPosition }).mapToPoints()
        val sprintQualifyingPoints =
            driverSprintQualifyingResults.sortedWith(compareBy(nullsLast()) { it.finalPosition }).mapToPoints()
        val racePoints =
            driverRaceResults.sortedWith(compareBy(nullsLast()) { it.finalPosition }).mapToPoints()
        val sprintRacePoints =
            driverSprintRaceResults.sortedWith(compareBy(nullsLast()) { it.finalPosition }).mapToPoints()

        val results = drivers.map { driver ->
            val points = calculateMean(
                practicePoints[driver.acronym],
                qualifyingPoints[driver.acronym],
                sprintQualifyingPoints[driver.acronym],
                racePoints[driver.acronym],
                sprintRacePoints[driver.acronym]
            )
            RaceWeekendResult.Companion.Result(
                driverId = driver.driverId,
                driverNumber = driver.driverNumber,
                driverAcronym = driver.acronym,
                points = points,
            )
        }

        return RaceWeekendResult(
            raceId = raceWeekend.raceId,
            raceName = raceWeekend.raceName,
            openF1MeetingKey = raceWeekend.openF1MeetingKey,
            createdAt = clock.now(),
            updatedAt = clock.now(),
            version = 1,
            results = results,
            summaryParagraphs = null
        )
    }

    private fun <T : DriverResult> List<T>.mapToPoints(): Map<String, Double> {
        return this.mapIndexed { index, result -> result.driverAcronym to mapIndexToPoints(index) }.toMap()
    }

    private fun mapIndexToPoints(index: Int): Double {
        val points =
            doubleArrayOf(20.0, 17.0, 15.0, 13.0, 11.0, 10.0, 9.0, 8.0, 7.0, 6.0, 5.0, 4.0, 3.0, 2.0, 1.0)
        return points.getOrElse(index) { 0.0 }
    }

    private fun calculateMean(vararg results: Double?): Double {
        val validResults = results.filterNotNull()
        if (validResults.isEmpty()) return 0.0
        val mean = validResults.average()
        return String.format(Locale.US, "%.1f", mean).toDouble()
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
