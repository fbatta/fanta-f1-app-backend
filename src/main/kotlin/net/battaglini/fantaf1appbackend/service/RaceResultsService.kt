package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
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
                LOGGER.error("No (sprint)race session found for race ${raceWeekend.raceId}")
                return emptyFlow()
            }

            val raceResults =
                openF1Client.getResults(sessionKeys = listOf(sessionKey.toString()))
                    .toList()
//            val startingGrid = openF1Client.getStartingGrid(sessionKey = sessionKey.toString()).toList()
            val startingGrid = emptyList<OpenF1StartingGridResponse>()
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
//        TODO: fix getting number of overtakes
//        val numberOfOvertakes = openF1Client.getOvertakes(
//            sessionKey = sessionKey.toString(),
//            overtakingDriverNumber = driver.driverNumber
//        ).count()
//        delay(1500)

        var fastestLap = 9_999.0
        var speedAtTrap = 0.0
        openF1Client.getLaps(sessionKey = sessionKey.toString(), driverNumber = driver.driverNumber)
            .collect { lap ->
                if (lap.lapDuration != null && lap.lapDuration < fastestLap)
                    fastestLap = lap.lapDuration
                if (lap.speedTrapSpeed != null && lap.speedTrapSpeed > speedAtTrap)
                    speedAtTrap = lap.speedTrapSpeed
            }
        delay(2000)
        val startPosition = startingGrid.firstOrNull { it.driverNumber == driver.driverNumber }?.position
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
            finalPosition = result.position ?: 22,
            dns = result.dns,
            dnf = result.dnf,
            dsq = result.dsq,
            numberOfOvertakes = 0,
            maximumSpeedAtTrap = speedAtTrap
        )
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(RaceResultsService::class.java)
    }
}