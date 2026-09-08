package net.battaglini.fantaf1appbackend.model.request

import kotlinx.datetime.DateTimeUnit

data class OpenRaceWeekendLineupRequest(
    val raceId: String,
    val delay: Long? = null,
    val delayUnit: DateTimeUnit.TimeBased? = null,
)