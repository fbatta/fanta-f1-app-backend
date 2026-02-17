package net.battaglini.fantaf1appbackend.task

import com.google.cloud.firestore.DocumentSnapshot
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.toList
import net.battaglini.fantaf1appbackend.configuration.ChannelConfiguration
import net.battaglini.fantaf1appbackend.enums.UserNotificationType
import net.battaglini.fantaf1appbackend.model.Lineup
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import net.battaglini.fantaf1appbackend.repository.LineupRepository
import net.battaglini.fantaf1appbackend.repository.TeamRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import kotlin.time.Clock

@Component
class TeamsResultsCalculatorTask(
    private val taskChannel: Channel<ChannelConfiguration.Companion.TaskChannelMessage>,
    private val userNotificationChannel: Channel<ChannelConfiguration.Companion.UserNotificationChannelMessage>,
    private val teamRepository: TeamRepository,
    private val lineupRepository: LineupRepository
) {
    @Scheduled(fixedRate = 1000)
    suspend fun runTask() {
        LOGGER.debug("Checking raceWeekend results availability")

        val message = taskChannel.tryReceive().getOrNull()

        if (message == null) {
            LOGGER.debug("No raceWeekend results available")
            return
        }

        val raceWeekendResult = message.data as RaceWeekendResult

        LOGGER.info("Calculating teams results for raceId={}", raceWeekendResult.raceId)
        calculateTeamsResults(raceWeekendResult)
        userNotificationChannel.send(
            ChannelConfiguration.Companion.UserNotificationChannelMessage(
                UserNotificationType.RACE_WEEKEND_CALCULATION_COMPLETED,
                raceWeekendResult
            )
        )
    }

    private suspend fun calculateTeamsResults(raceWeekendResult: RaceWeekendResult) {
        var cursor: DocumentSnapshot? = null

        do {
            val teamsPair = teamRepository.getAllTeams(cursor).toList()
            LOGGER.info("Retrieved {} teams", teamsPair.size)
            cursor = teamsPair.last().first

            for (team in teamsPair.map { it.second }) {
                val lineup = lineupRepository.getLineup(team.teamId, raceWeekendResult.raceId)

                if (lineup == null) {
                    LOGGER.warn(
                        "Could not find a lineup for teamId={}, teamName={}, raceId={}",
                        team.teamId,
                        team.teamName,
                        raceWeekendResult.raceId
                    )
                    continue
                }

                val points = calculatePointsPerLineup(raceWeekendResult, lineup)

                try {
                    lineupRepository.createOrUpdateLineup(
                        lineup.copy(
                            score = points,
                            updatedAt = Clock.System.now(),
                            version = lineup.version + 1
                        )
                    )
                } catch (e: Exception) {
                    LOGGER.error(
                        "Could not save score for teamId={}, teamName={}. Admin will have to enter score manually",
                        team.teamId,
                        team.teamName,
                        e
                    )
                }
            }
        } while (teamsPair.isNotEmpty())
    }

    suspend fun calculatePointsPerLineup(raceWeekendResult: RaceWeekendResult, lineup: Lineup): Double {
        var points = 0.0
        for (driver in lineup.drivers) {
            val result = raceWeekendResult.results.firstOrNull { it.driverAcronym == driver.acronym }
            if (result != null) {
                points += result.points
            }
        }
        return points
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TeamsResultsCalculatorTask::class.java)
    }
}