package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.Flow
import net.battaglini.fantaf1appbackend.model.DriverQualifyingResult
import net.battaglini.fantaf1appbackend.model.RaceWeekend

interface QualifyingResultsService {
    suspend fun getDriversResultsForQualifying(
        raceWeekend: RaceWeekend,
        isSprintQualifying: Boolean
    ): Flow<DriverQualifyingResult>
}