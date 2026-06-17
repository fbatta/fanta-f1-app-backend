package net.battaglini.fantaf1appbackend.task

import kotlinx.coroutines.channels.Channel
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
import net.battaglini.fantaf1appbackend.model.RaceWeekend
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1MeetingResponse
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1MeetingResponse.Companion.toRace
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1SessionResponse.Companion.toRaceWeekendSession
import net.battaglini.fantaf1appbackend.repository.DriverRepository
import net.battaglini.fantaf1appbackend.repository.RaceWeekendResultRepository
import net.battaglini.fantaf1appbackend.service.RaceWeekendResultsCalculator
import net.battaglini.fantaf1appbackend.service.RaceWeekendService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Component
class RaceWeekendResultsCalculatorTask(
    private val resultsCalculatorProperties: ResultsCalculatorProperties,
    private val raceWeekendResultRepository: RaceWeekendResultRepository,
    private val openF1Client: OpenF1Client,
    private val driverRepository: DriverRepository,
    private val taskChannel: Channel<ChannelConfiguration.Companion.TaskChannelMessage>,
    private val clock: Clock,
    private val timeZone: TimeZone,
    private val raceWeekendResultsCalculator: RaceWeekendResultsCalculator,
    private val raceWeekendService: RaceWeekendService
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

            val combinedDriversResults = raceWeekendService.fetchDriversResults(raceWeekend) ?: return

            val raceWeekendResult = raceWeekendResultsCalculator.calculateRaceWeekendResults(
                combinedDriversResults.combinedPracticeResults,
                combinedDriversResults.qualifyingResults,
                combinedDriversResults.sprintQualifyingResults,
                combinedDriversResults.raceResults,
                combinedDriversResults.sprintRaceResults,
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
        val nowLocal = now.toLocalDateTime(timeZone)
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

    private suspend fun saveAndNotify(raceWeekendResult: RaceWeekendResult) {
        if (resultsCalculatorProperties.dryRun) {
            LOGGER.info("DRY RUN: race weekend results for ${raceWeekendResult.raceName}\n$raceWeekendResult")
        } else {
            raceWeekendResultRepository.saveRaceWeekendResult(raceWeekendResult)
            taskChannel.send(
                ChannelConfiguration.Companion.TaskChannelMessage(
                    TaskType.UPDATE_DRIVERS_PRICING,
                    raceWeekendResult
                )
            )
            taskChannel.send(
                ChannelConfiguration.Companion.TaskChannelMessage(
                    TaskType.CALCULATE_LINEUP_RESULTS,
                    raceWeekendResult
                )
            )
        }
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
