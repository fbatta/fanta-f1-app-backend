package net.battaglini.fantaf1appbackend.model.request

data class CreateLineupRequest(
    val teamId: String,
    val raceId: String,
    val driverIds: List<String>
)
