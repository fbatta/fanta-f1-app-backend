package net.battaglini.fantaf1appbackend.model.response

data class CopyLineupResponse(
    val lineupId: String,
    val teamId: String,
    val targetRaceId: String,
    val drivers: List<LineupDriverDto>
) {
    data class LineupDriverDto(
        val driverId: String,
        val driverNumber: Int,
        val driverAcronym: String,
        val driverCost: Double
    )
}
