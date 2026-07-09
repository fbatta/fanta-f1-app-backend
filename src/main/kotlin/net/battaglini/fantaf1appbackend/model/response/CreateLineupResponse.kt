package net.battaglini.fantaf1appbackend.model.response

data class CreateLineupResponse(
    val lineupId: String,
    val teamId: String,
    val raceId: String,
    val drivers: List<LineupDriverDto>
) {
    data class LineupDriverDto(
        val driverId: String,
        val driverNumber: Int,
        val driverAcronym: String,
        val driverCost: Double
    )
}
