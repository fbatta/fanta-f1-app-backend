package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.Flow
import net.battaglini.fantaf1appbackend.model.DriverQualifyingResult
import net.battaglini.fantaf1appbackend.model.RaceWeekend

/**
 * Service responsible for retrieving and processing qualifying session results.
 */
interface QualifyingResultsService {
    /**
     * Retrieves the qualifying results for all drivers in a specific race weekend.
     *
     * @param raceWeekend The race weekend to fetch qualifying results for.
     * @param isSprintQualifying Whether to fetch results for the sprint qualifying session.
     * @return A [Flow] of [DriverQualifyingResult] objects.
     */
    suspend fun getDriversResultsForQualifying(
        raceWeekend: RaceWeekend,
        isSprintQualifying: Boolean
    ): Flow<DriverQualifyingResult>
}