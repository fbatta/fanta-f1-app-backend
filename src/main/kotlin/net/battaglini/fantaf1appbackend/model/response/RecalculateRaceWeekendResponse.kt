package net.battaglini.fantaf1appbackend.model.response

import net.battaglini.fantaf1appbackend.model.RaceWeekendResult

data class RecalculateRaceWeekendResponse(
    val raceId: String,
    val raceName: String,
    val version: Int,
    val oldResults: RaceWeekendResult?,
    val newResults: RaceWeekendResult?,
)