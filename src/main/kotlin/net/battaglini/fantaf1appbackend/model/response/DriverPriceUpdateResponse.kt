package net.battaglini.fantaf1appbackend.model.response

data class DriverPriceUpdateResponse(
    val updates: List<DriverPriceUpdateDetails>
)

data class DriverPriceUpdateDetails(
    val driverId: String,
    val acronym: String,
    val previousPrice: Double,
    val newPrice: Double,
    val percentageChange: Double
)
