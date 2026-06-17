package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.toList
import net.battaglini.fantaf1appbackend.model.*
import net.battaglini.fantaf1appbackend.repository.DriverRepository
import org.springframework.stereotype.Service
import java.util.*
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Service
class RaceWeekendResultsCalculator(
    private val driverRepository: DriverRepository,
    private val clock: Clock
) {
    suspend fun calculateRaceWeekendResults(
        driverPracticeResults: List<DriverPracticeResult>,
        driverQualifyingResults: List<DriverQualifyingResult>,
        driverSprintQualifyingResults: List<DriverQualifyingResult>,
        driverRaceResults: List<DriverRaceResult>,
        driverSprintRaceResults: List<DriverRaceResult>,
        raceWeekend: RaceWeekend
    ): RaceWeekendResult {
        val drivers = driverRepository.getDrivers().toList()

        val practicePoints = driverPracticeResults.sortedBy { it.fastestLap }.mapToPoints()
        val qualifyingPoints =
            driverQualifyingResults.sortedWith(compareBy(nullsLast()) { it.finalPosition }).mapToPoints()
        val sprintQualifyingPoints =
            driverSprintQualifyingResults.sortedWith(compareBy(nullsLast()) { it.finalPosition }).mapToPoints()
        val racePoints =
            driverRaceResults.sortedWith(compareBy(nullsLast()) { it.finalPosition }).mapToPoints()
        val sprintRacePoints =
            driverSprintRaceResults.sortedWith(compareBy(nullsLast()) { it.finalPosition }).mapToPoints()

        val results = drivers.map { driver ->
            val points = calculateMean(
                practicePoints[driver.acronym],
                qualifyingPoints[driver.acronym],
                sprintQualifyingPoints[driver.acronym],
                racePoints[driver.acronym],
                sprintRacePoints[driver.acronym]
            )
            RaceWeekendResult.Companion.Result(
                driverId = driver.driverId,
                driverNumber = driver.driverNumber,
                driverAcronym = driver.acronym,
                points = points,
            )
        }

        return RaceWeekendResult(
            raceId = raceWeekend.raceId,
            raceName = raceWeekend.raceName,
            openF1MeetingKey = raceWeekend.openF1MeetingKey,
            createdAt = clock.now(),
            updatedAt = clock.now(),
            version = 1,
            results = results
        )
    }

    private fun <T : DriverResult> List<T>.mapToPoints(): Map<String, Double> {
        return this.mapIndexed { index, result -> result.driverAcronym to mapIndexToPoints(index) }.toMap()
    }

    private fun mapIndexToPoints(index: Int): Double {
        val points =
            doubleArrayOf(20.0, 17.0, 15.0, 13.0, 11.0, 10.0, 9.0, 8.0, 7.0, 6.0, 5.0, 4.0, 3.0, 2.0, 1.0)
        return points.getOrElse(index) { 0.0 }
    }

    private fun calculateMean(vararg results: Double?): Double {
        val validResults = results.filterNotNull()
        if (validResults.isEmpty()) return 0.0
        val mean = validResults.average()
        return String.format(Locale.US, "%.1f", mean).toDouble()
    }
}
