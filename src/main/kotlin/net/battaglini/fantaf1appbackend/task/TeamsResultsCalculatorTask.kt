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
import net.battaglini.fantaf1appbackend.model.Team
import net.battaglini.fantaf1appbackend.repository.LineupRepository
import net.battaglini.fantaf1appbackend.repository.TeamRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.*
import kotlin.time.Clock

@Component
class TeamsResultsCalculatorTask(
    private val resultsCalculatorProperties: ResultsCalculatorProperties,
    private val taskChannel: Channel<ChannelConfiguration.Companion.TaskChannelMessage>,
    private val userNotificationChannel: Channel<ChannelConfiguration.Companion.UserNotificationChannelMessage>,
    private val teamRepository: TeamRepository,
    private val lineupRepository: LineupRepository,
    private val firestore: Firestore,
    private val clock: Clock,
    private val timeZone: TimeZone
) {
    @Scheduled(fixedRate = 1000)
    internal suspend fun runTask() {
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
                UserNotificationType.RACE_WEEKEND_RESULTS_AVAILABLE,
                raceWeekendResult
            )
        )
    }

    private suspend fun calculateTeamsResults(raceWeekendResult: RaceWeekendResult) {
        val driverPoints = raceWeekendResult.results.associate { it.driverAcronym to it.points }
        val currentYear = clock.now().toLocalDateTime(timeZone).year

        var cursor: DocumentSnapshot? = null
        do {
            val teamsPair = teamRepository.getAllTeams(cursor).toList()
            LOGGER.info("Retrieved {} teams", teamsPair.size)
            if (teamsPair.isEmpty()) break
            cursor = teamsPair.last().first

            for ((_, team) in teamsPair) {
                processTeam(team, raceWeekendResult.raceId, driverPoints, currentYear)
            }
        } while (teamsPair.isNotEmpty())
    }

    private suspend fun processTeam(
        team: Team,
        raceId: String,
        driverPoints: Map<String, Double>,
        currentYear: Int
    ) {
        val lineup = lineupRepository.getLineup(team.teamId, raceId)
        if (lineup == null) {
            LOGGER.warn(
                "Could not find a lineup for teamId={}, teamName={}, raceId={}",
                team.teamId,
                team.teamName,
                raceId
            )
            return
        }

        val score = calculatePointsPerLineup(lineup, driverPoints)
        val teamPoints = team.points.toMutableMap()
        teamPoints[currentYear] = teamPoints.getOrDefault(currentYear, 0.0) + score

        if (resultsCalculatorProperties.dryRun) {
            LOGGER.info("Dry-running team results for ${team.teamName}. Score: $score")
            return
        }

        saveTeamAndLineup(team, teamPoints, lineup, score)
    }

    private suspend fun saveTeamAndLineup(
        team: Team,
        updatedPoints: Map<Int, Double>,
        lineup: Lineup,
        score: Double
    ) {
        try {
            withContext(Dispatchers.IO) {
                firestore.runTransaction { transaction ->
                    lineupRepository.updateLineupInTransaction(
                        lineup.copy(
                            score = score,
                            updatedAt = clock.now(),
                            version = lineup.version + 1
                        ),
                        transaction
                    )
                    teamRepository.updateTeamInTransaction(
                        team.copy(
                            points = updatedPoints.toMutableMap(),
                            updatedAt = clock.now(),
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

    fun calculatePointsPerLineup(lineup: Lineup, driverPoints: Map<String, Double>): Double {
        val points = lineup.drivers.sumOf { driver -> driverPoints[driver.driverAcronym] ?: 0.0 }
        return String.format(Locale.US, "%.1f", points).toDouble()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TeamsResultsCalculatorTask::class.java)
    }
}
