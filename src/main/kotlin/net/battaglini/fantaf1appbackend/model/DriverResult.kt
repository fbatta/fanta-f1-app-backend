package net.battaglini.fantaf1appbackend.model

import net.battaglini.fantaf1appbackend.enums.RaceWeekendSessionType

open class DriverResult(
    open val raceId: String,
    open val driverId: String,
    open val sessionId: String,
    open val sessionType: RaceWeekendSessionType,
    open val driverNumber: Int,
    open val driverAcronym: String
)
