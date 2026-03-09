package net.battaglini.fantaf1appbackend.model.request

data class UpdateDriversCostsRequest(
    val driversCosts: List<DriverCostRequest>
) {
    companion object {
        data class DriverCostRequest(
            val acronym: String,
            val driverCost: Double
        )
    }
}
