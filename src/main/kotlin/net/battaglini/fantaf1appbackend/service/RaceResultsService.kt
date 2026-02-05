package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.*
import net.battaglini.fantaf1appbackend.client.OpenF1Client
import net.battaglini.fantaf1appbackend.enums.openf1.OpenF1SessionType
import net.battaglini.fantaf1appbackend.model.DriverResult
import net.battaglini.fantaf1appbackend.model.Race
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1SessionResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RaceResultsService(
    private val openF1Client: OpenF1Client,
    private val driverService: DriverService
) {
    suspend fun getResultsForRace(race: Race): Flow<DriverResult> {
        LOGGER.info("Retrieving results for race={} id={}", race.raceName, race.raceId)
        try {
            val sessions = openF1Client.getSessions(meetingKey = race.openF1MeetingKey)
            val raceSessionId = findRaceSessionId(sessions)

            if (raceSessionId == null) {
                LOGGER.error("No race session found for race ${race.raceId}")
                return emptyFlow()
            }

            val results = openF1Client.getResults(sessionKey = raceSessionId)
            val drivers = driverService.getDriversInSession(raceSessionId)
                .map { driver ->
                    val driverOvertakes = openF1Client.getOvertakes(
                        sessionKey = raceSessionId,
                        overtakingDriverNumber = driver.driverNumber
                    )
                    val driverStints =
                        openF1Client.getStints(sessionKey = raceSessionId, driverNumber = driver.driverNumber)
                    val driverLaps =
                        openF1Client.getLaps(sessionKey = raceSessionId, driverNumber = driver.driverNumber)

                    combineTransform(driverOvertakes, driverStints, driverLaps)
                }
        } catch (ex: Exception) {
            LOGGER.error("Error retrieving results for race={} id={}", race.raceName, race.raceId, ex)
            return emptyFlow()
        }
    }

    private suspend fun findRaceSessionId(sessions: Flow<OpenF1SessionResponse>): Int? {
        return sessions.firstOrNull { session ->
            session.sessionType == OpenF1SessionType.RACE
        }?.sessionKey
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(RaceResultsService::class.java)
    }
}