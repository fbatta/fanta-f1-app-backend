package net.battaglini.fantaf1appbackend.model

data class DriverSummary(
    val driverId: String,
    val driverName: String,
    val driverAcronym: String,
    val driverNumber: Int,
    val summaryParagraphs: List<String>
)
