package net.battaglini.fantaf1appbackend.model

import net.battaglini.fantaf1appbackend.enums.RaceWeekendSessionType
import kotlin.time.Duration

class DriverQualifyingResult(
    val fastestLapQ1: Duration,
    val fastestLapQ2: Duration,
    val fastestLapQ3: Duration,
    val finalPosition: Int?,
    val dns: Boolean,
    val dnf: Boolean,
    val dsq: Boolean,
    override val raceId: String,
    override val driverId: String,
    override val sessionId: String,
    override val sessionType: RaceWeekendSessionType,
    override val driverNumber: Int,
    override val driverAcronym: String
) : DriverResult(
    raceId = raceId,
    sessionId = sessionId,
    driverId = driverId,
    driverNumber = driverNumber,
    driverAcronym = driverAcronym,
    sessionType = sessionType,
)
