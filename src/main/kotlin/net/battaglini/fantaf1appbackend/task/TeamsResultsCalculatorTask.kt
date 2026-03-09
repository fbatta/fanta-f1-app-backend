package net.battaglini.fantaf1appbackend.task

import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.battaglini.fantaf1appbackend.configuration.ChannelConfiguration
import net.battaglini.fantaf1appbackend.configuration.ResultsCalculatorProperties
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
    private val resultsCalculatorProperties: ResultsCalculatorProperties,
    private val taskChannel: Channel<ChannelConfiguration.Companion.TaskChannelMessage>,
    private val userNotificationChannel: Channel<ChannelConfiguration.Companion.UserNotificationChannelMessage>,
    private val teamRepository: TeamRepository,
    private val lineupRepository: LineupRepository,
    private val firestore: Firestore
) {
    @Scheduled(fixedRate = 1000)
    suspend fun runTask() {
        if (!resultsCalculatorProperties.enable) {
            LOGGER.debug("Skipping checking raceWeekend results availability because it is disabled in app config")
            return
        }
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
            if (teamsPair.isEmpty()) {
                break
            }
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

                val score = calculatePointsPerLineup(raceWeekendResult, lineup)
                val currentYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
                val currentPoints = team.points
                val teamPointsForYear = currentPoints.getOrDefault(currentYear, 0.0) + score
                currentPoints[currentYear] = teamPointsForYear

                if (resultsCalculatorProperties.dryRun) {
                    LOGGER.info(
                        """
                        Dry-running team results for ${team.teamName}. Score: $score
                        """.trimIndent()
                    )
                    continue
                }
                try {
                    withContext(Dispatchers.IO) {
                        firestore.runTransaction { transaction ->
                            lineupRepository.updateLineupInTransaction(
                                lineup.copy(
                                    score = score,
                                    updatedAt = Clock.System.now(),
                                    version = lineup.version + 1
                                ),
                                transaction
                            )
                            teamRepository.updateTeamInTransaction(
                                team.copy(
                                    points = currentPoints,
                                    updatedAt = Clock.System.now(),
                                ),
                                transaction
                            )
                        }.get()
                    }
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
            val result = raceWeekendResult.results.firstOrNull { it.driverAcronym == driver.driverAcronym }
            if (result != null) {
                points += result.points
            }
        }
        return String.format("%.1f", points).toDouble()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TeamsResultsCalculatorTask::class.java)
    }
}