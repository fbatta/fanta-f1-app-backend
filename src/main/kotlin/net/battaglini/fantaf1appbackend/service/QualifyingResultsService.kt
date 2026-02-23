package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import net.battaglini.fantaf1appbackend.client.OpenF1Client
import net.battaglini.fantaf1appbackend.enums.RaceWeekendSessionType
import net.battaglini.fantaf1appbackend.model.DriverQualifyingResult
import net.battaglini.fantaf1appbackend.model.RaceWeekend
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1QualifyingSessionResultResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Service
class QualifyingResultsService(
    val openF1Client: OpenF1Client,
    val driverService: DriverService
) {
    suspend fun getDriversResultsForQualifying(
        raceWeekend: RaceWeekend,
        isSprintQualifying: Boolean
    ): Flow<DriverQualifyingResult> {
        val session = raceWeekend.sessions.firstOrNull { session ->
            if (isSprintQualifying)
                session.sessionType == RaceWeekendSessionType.SPRINT_QUALIFYING
            else
                session.sessionType == RaceWeekendSessionType.QUALIFYING
        }
        val sessionKey = session?.openF1SessionKey

        if (sessionKey == null) {
            LOGGER.error(
                "No (sprint)qualifying session found for raceId={}, raceName={}",
                raceWeekend.raceId,
                raceWeekend.raceName
            )
            return emptyFlow()
        }

        val results =
            openF1Client.getResults<OpenF1QualifyingSessionResultResponse>(sessionKeys = listOf(sessionKey.toString()))
                .toList()
        return driverService.getDriversInSessions(listOf(sessionKey)).map { driver ->
            val result = results.first { it.driverNumber == driver.driverNumber }
            DriverQualifyingResult(
                raceId = raceWeekend.raceId,
                driverId = driver.driverId,
                sessionId = session.sessionId,
                sessionType = session.sessionType,
                driverNumber = driver.driverNumber,
                driverAcronym = driver.acronym,
                fastestLapQ1 = result.duration[0].toDuration(DurationUnit.SECONDS),
                fastestLapQ2 = result.duration[1].toDuration(DurationUnit.SECONDS),
                fastestLapQ3 = result.duration[2].toDuration(DurationUnit.SECONDS),
                finalPosition = result.position,
                dns = result.dns,
                dnf = result.dnf,
                dsq = result.dsq
            )
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(QualifyingResultsService::class.java)
    }
}