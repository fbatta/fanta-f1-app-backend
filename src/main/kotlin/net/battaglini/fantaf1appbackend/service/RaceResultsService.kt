package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.Flow
import net.battaglini.fantaf1appbackend.model.DriverRaceResult
import net.battaglini.fantaf1appbackend.model.RaceWeekend

interface RaceResultsService {
    suspend fun getResultsForRace(raceWeekend: RaceWeekend, isSprintRace: Boolean): Flow<DriverRaceResult>
}