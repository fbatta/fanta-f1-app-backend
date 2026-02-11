package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import net.battaglini.fantaf1appbackend.client.OpenF1Client
import net.battaglini.fantaf1appbackend.enums.RaceWeekendSessionType
import net.battaglini.fantaf1appbackend.model.DriverPracticeResult
import net.battaglini.fantaf1appbackend.model.RaceWeekend
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1SessionResultResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Service
class PracticeResultsService(
    val openF1Client: OpenF1Client,
    val driverService: DriverService
) {
    suspend fun getDriversResultsForCombinedPractice(raceWeekend: RaceWeekend): Flow<DriverPracticeResult> {
        val sessions = raceWeekend.sessions.filter {
            when (it.sessionType) {
                RaceWeekendSessionType.PRACTICE_1 -> true
                RaceWeekendSessionType.PRACTICE_2 -> true
                RaceWeekendSessionType.PRACTICE_3 -> true
                else -> false
            }
        }.map { Pair(it.sessionId, it.openF1SessionKey) }

        if (sessions.isEmpty()) {
            LOGGER.warn(
                "No practice sessions found for raceId={}, raceName={}",
                raceWeekend.raceId,
                raceWeekend.raceName
            )
            return emptyFlow()
        }

        val results = openF1Client.getResults<OpenF1SessionResultResponse>(
            meetingKey = raceWeekend.openF1MeetingKey,
            sessionKeys = sessions.map { it.second }
        ).toList()
        return driverService.getDriversInSessions(sessions.map { it.second }).map { driver ->
            val fastestLap = results.filter { it.driverNumber == driver.driverNumber }.fold(999_999.9, { acc, result ->
                if (result.duration < acc) result.duration else acc
            })

            DriverPracticeResult(
                raceId = raceWeekend.raceId,
                driverId = driver.driverId,
                sessionId = sessions.first().first,
                sessionType = RaceWeekendSessionType.PRACTICE_COMBINED,
                driverNumber = driver.driverNumber,
                driverAcronym = driver.acronym,
                fastestLap = fastestLap.toDuration(DurationUnit.SECONDS)
            )
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(PracticeResultsService::class.java)
    }
}