package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.Flow
import net.battaglini.fantaf1appbackend.model.DriverPracticeResult
import net.battaglini.fantaf1appbackend.model.RaceWeekend

interface PracticeResultsService {
    suspend fun getDriversResultsForCombinedPractice(raceWeekend: RaceWeekend): Flow<DriverPracticeResult>
}