package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.Flow
import net.battaglini.fantaf1appbackend.model.DriverRaceResult
import net.battaglini.fantaf1appbackend.model.RaceWeekend

/**
 * Service responsible for retrieving and processing race session results.
 */
interface RaceResultsService {
    /**
     * Retrieves the race results for all drivers in a specific race weekend.
     *
     * @param raceWeekend The race weekend to fetch race results for.
     * @param isSprintRace Whether to fetch results for the sprint race session.
     * @return A [Flow] of [DriverRaceResult] objects.
     */
    suspend fun getResultsForRace(raceWeekend: RaceWeekend, isSprintRace: Boolean): Flow<DriverRaceResult>
}