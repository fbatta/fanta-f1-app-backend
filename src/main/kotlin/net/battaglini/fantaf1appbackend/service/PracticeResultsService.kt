package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.Flow
import net.battaglini.fantaf1appbackend.model.DriverPracticeResult
import net.battaglini.fantaf1appbackend.model.RaceWeekend

/**
 * Service responsible for retrieving and processing practice session results.
 */
interface PracticeResultsService {
    /**
     * Retrieves the results for all drivers across all practice sessions in a race weekend,
     * identifying the best performance for each driver.
     *
     * @param raceWeekend The race weekend to fetch practice results for.
     * @return A [Flow] of [DriverPracticeResult] objects.
     */
    suspend fun getDriversResultsForCombinedPractice(raceWeekend: RaceWeekend): Flow<DriverPracticeResult>
}