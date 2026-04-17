package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.Flow
import net.battaglini.fantaf1appbackend.model.Driver
import net.battaglini.fantaf1appbackend.model.DriverSummary
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import net.battaglini.fantaf1appbackend.model.request.UpdateDriversCostsRequest

interface DriverService {
    suspend fun seedDrivers()
    suspend fun updateDriversCosts(costs: UpdateDriversCostsRequest)
    suspend fun updateDriverSummary(driver: Driver): DriverSummary?
    suspend fun updateDriverSummary(acronym: String): DriverSummary?
    suspend fun updateDriverSummaries(acronyms: List<String>): List<DriverSummary>
    suspend fun updateAllDriversSummaries(): List<DriverSummary>
    suspend fun getDriversInSessions(sessionKeys: List<Int>): Flow<Driver>
    suspend fun calculateDriverAverageScore(
        year: Int,
        driverId: String? = null,
        driverAcronym: String? = null
    ): RaceWeekendResult.Companion.Result
}