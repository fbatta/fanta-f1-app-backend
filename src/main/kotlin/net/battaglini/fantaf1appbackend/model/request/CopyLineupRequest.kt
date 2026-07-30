package net.battaglini.fantaf1appbackend.model.request

data class CopyLineupRequest(
    val teamId: String,
    val sourceRaceId: String,
    val targetRaceId: String
)
