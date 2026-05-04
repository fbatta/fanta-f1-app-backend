package net.battaglini.fantaf1appbackend.service

import net.battaglini.fantaf1appbackend.model.Lineup
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import net.battaglini.fantaf1appbackend.model.Team

interface TeamResultsService {
    suspend fun calculateAndSaveLineupsResults(raceWeekendResult: RaceWeekendResult): Map<Team, Lineup?>
}