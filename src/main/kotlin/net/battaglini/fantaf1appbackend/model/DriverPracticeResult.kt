package net.battaglini.fantaf1appbackend.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import kotlin.time.Duration

@JsonIgnoreProperties(ignoreUnknown = true)
data class DriverPracticeCombinedResult(
    val raceId: String,
    val driverId: String,
    val sessionId: String,
    val driverNumber: Int,
    val driverAcronym: String,
    val fastestLap: Duration
)
