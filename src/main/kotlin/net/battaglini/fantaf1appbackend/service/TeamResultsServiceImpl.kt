package net.battaglini.fantaf1appbackend.service

import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.battaglini.fantaf1appbackend.configuration.ResultsCalculatorProperties
import net.battaglini.fantaf1appbackend.model.Lineup
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import net.battaglini.fantaf1appbackend.model.Team
import net.battaglini.fantaf1appbackend.repository.LineupRepository
import net.battaglini.fantaf1appbackend.repository.TeamRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import kotlin.time.Clock

@Service
class TeamResultsServiceImpl(
    private val clock: Clock,
    private val timeZone: TimeZone,
    private val teamRepository: TeamRepository,
    private val lineupRepository: LineupRepository,
    private val resultsCalculatorProperties: ResultsCalculatorProperties,
    private val firestore: Firestore
) : TeamResultsService {
    override suspend fun calculateAndSaveLineupsResults(raceWeekendResult: RaceWeekendResult): Map<Team, Lineup?> {
        val driverPoints = raceWeekendResult.results.associate { it.driverAcronym to it.points }

        val teamAndLineupMap = mutableMapOf<Team, Lineup?>()
        var cursor: DocumentSnapshot? = null
        do {
            val teamsPair = teamRepository.getAllTeams(cursor).toList()
            LOGGER.info("Retrieved {} teams", teamsPair.size)
            if (teamsPair.isEmpty()) break
            cursor = teamsPair.last().first

            teamsPair.map { pair ->
                val lineupResults = calculateLineupResult(pair.second.teamId, raceWeekendResult.raceId, driverPoints)
                val updatedTeam = calculateTeamPoints(pair.second, lineupResults?.score ?: 0.0)

                return@map Pair(updatedTeam, lineupResults)
            }.forEach { teamAndLineupMap[it.first] = it.second }
        } while (teamsPair.isNotEmpty())


        if (!resultsCalculatorProperties.dryRun) {
            coroutineScope {
                teamAndLineupMap.forEach { (team, lineup) ->
                    launch {
                        if (lineup != null) {
                            saveTeamAndLineup(team, lineup)
                        }
                    }
                }
            }
        }

        return teamAndLineupMap
    }

    private suspend fun calculateLineupResult(
        teamId: String,
        raceId: String,
        driverPoints: Map<String, Double>
    ): Lineup? {
        val lineup = lineupRepository.getLineup(teamId, raceId)
        if (lineup == null) {
            LOGGER.warn(
                "Could not find a lineup for teamId={}, raceId={}",
                teamId,
                raceId
            )
            return null
        }

        val score = calculatePointsPerLineup(lineup, driverPoints)
        return lineup.copy(
            score = score,
            updatedAt = clock.now(),
            version = lineup.version + 1
        )
    }

    private suspend fun calculateTeamPoints(team: Team, lineupScore: Double): Team {
        val currentYear = clock.now().toLocalDateTime(timeZone).year
        val teamPoints = team.points.toMutableMap()
        teamPoints[currentYear] = teamPoints.getOrDefault(currentYear, 0.0) + lineupScore

        return team.copy(
            points = teamPoints.toMutableMap(),
            updatedAt = clock.now(),
        )
    }

    private suspend fun saveTeamAndLineup(
        team: Team,
        lineup: Lineup,
    ) {
        try {
            withContext(Dispatchers.IO) {
                firestore.runTransaction { transaction ->
                    lineupRepository.updateLineupInTransaction(
                        lineup,
                        transaction
                    )
                    teamRepository.updateTeamInTransaction(
                        team,
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
        private val LOGGER = LoggerFactory.getLogger(TeamResultsServiceImpl::class.java)
    }
}