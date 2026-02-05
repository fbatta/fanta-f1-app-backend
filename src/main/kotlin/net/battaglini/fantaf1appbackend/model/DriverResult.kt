package net.battaglini.fantaf1appbackend.model

import kotlin.time.Duration

data class DriverResult(
    val raceId: String,
    val driverId: String,
    val driverNumber: Int,
    val driverAcronym: String,
    val bestRaceTime: Duration,
    val finalPosition: Int?,
    val dns: Boolean,
    val dnf: Boolean,
    val dsq: Boolean,
    val numberOfOvertakes: Int,
    val maximumSpeedAtTrap: Double
)
