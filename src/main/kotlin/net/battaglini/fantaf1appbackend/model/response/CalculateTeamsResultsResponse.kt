package net.battaglini.fantaf1appbackend.model.response

import net.battaglini.fantaf1appbackend.model.Lineup
import net.battaglini.fantaf1appbackend.model.Team

data class CalculateTeamsResultsResponse(
    val raceId: String,
    val raceName: String,
    val scores: List<TeamScore>
) {
    companion object {
        data class TeamScore(
            val team: Team,
            val lineup: Lineup?
        )
    }
}
