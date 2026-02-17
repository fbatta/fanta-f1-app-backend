package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.*
import net.battaglini.fantaf1appbackend.client.OpenF1Client
import net.battaglini.fantaf1appbackend.enums.RaceWeekendSessionType
import net.battaglini.fantaf1appbackend.model.Driver
import net.battaglini.fantaf1appbackend.model.DriverRaceResult
import net.battaglini.fantaf1appbackend.model.RaceWeekend
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1SessionResultResponse
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1StartingGridResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Service
class RaceResultsService(
    private val openF1Client: OpenF1Client,
    private val driverService: DriverService
) {
    suspend fun getResultsForRace(raceWeekend: RaceWeekend, isSprintRace: Boolean): Flow<DriverRaceResult> {
        LOGGER.info("Retrieving results for race={} id={}", raceWeekend.raceName, raceWeekend.raceId)
        try {
            val session = raceWeekend.sessions.firstOrNull { session ->
                if (isSprintRace)
                    session.sessionType == RaceWeekendSessionType.SPRINT_RACE
                else
                    session.sessionType == RaceWeekendSessionType.RACE
            }
            val sessionKey = session?.openF1SessionKey

            if (sessionKey == null) {
                LOGGER.error("No race session found for race ${raceWeekend.raceId}")
                return emptyFlow()
            }

            val raceResults =
                openF1Client.getResults<OpenF1SessionResultResponse>(sessionKeys = listOf(sessionKey.toString()))
                    .toList()
            val startingGrid = openF1Client.getStartingGrid(sessionKey = sessionKey.toString()).toList()
            return driverService.getDriversInSessions(listOf(sessionKey))
                .map { driver ->
                    getDriverResultForRace(sessionKey, driver, startingGrid, raceResults, raceWeekend, session)
                }
        } catch (ex: Exception) {
            LOGGER.error("Error retrieving results for race={} id={}", raceWeekend.raceName, raceWeekend.raceId, ex)
            return emptyFlow()
        }
    }

    private suspend fun getDriverResultForRace(
        sessionKey: Int,
        driver: Driver,
        startingGrid: List<OpenF1StartingGridResponse>,
        results: List<OpenF1SessionResultResponse>,
        raceWeekend: RaceWeekend,
        session: RaceWeekend.Companion.Session,
    ): DriverRaceResult {
        val numberOfOvertakes = openF1Client.getOvertakes(
            sessionKey = sessionKey.toString(),
            overtakingDriverNumber = driver.driverNumber
        ).count()
        var fastestLap = 999_999.00
        var speedAtTrap = 0.0
        openF1Client.getLaps(sessionKey = sessionKey.toString(), driverNumber = driver.driverNumber)
            .collect { lap ->
                if (lap.lapDuration < fastestLap)
                    fastestLap = lap.lapDuration
                if (lap.speedTrapSpeed > speedAtTrap)
                    speedAtTrap = lap.speedTrapSpeed
            }
        val startPosition = startingGrid.first { it.driverNumber == driver.driverNumber }.position
        val result = results.first { it.driverNumber == driver.driverNumber }

        return DriverRaceResult(
            raceId = raceWeekend.raceId,
            driverId = driver.driverId,
            sessionId = session.sessionId,
            sessionType = session.sessionType,
            driverNumber = driver.driverNumber,
            driverAcronym = driver.acronym,
            fastestLap = fastestLap.toDuration(DurationUnit.SECONDS),
            startPosition = startPosition,
            finalPosition = result.position,
            dns = result.dns,
            dnf = result.dnf,
            dsq = result.dsq,
            numberOfOvertakes = numberOfOvertakes,
            maximumSpeedAtTrap = speedAtTrap
        )
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(RaceResultsService::class.java)
    }
}