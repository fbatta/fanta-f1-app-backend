package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.Flow
import net.battaglini.fantaf1appbackend.model.Driver
import net.battaglini.fantaf1appbackend.model.DriverSummary
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import net.battaglini.fantaf1appbackend.model.request.UpdateDriversCostsRequest

/**
 * Service responsible for managing Formula 1 drivers' data, including seeding,
 * cost updates, and AI-generated performance summaries.
 */
interface DriverService {
    /**
     * Fetches current F1 drivers from external sources and seeds them into the repository.
     */
    suspend fun seedDrivers()

    /**
     * Updates the fantasy costs for a list of drivers.
     *
     * @param costs The request containing acronyms and new costs for drivers.
     */
    suspend fun updateDriversCosts(costs: UpdateDriversCostsRequest)

    /**
     * Generates and saves a performance summary for a specific driver.
     *
     * @param driver The driver object to generate a summary for.
     * @return The generated [DriverSummary], or null if generation failed.
     */
    suspend fun updateDriverSummary(driver: Driver): DriverSummary?

    /**
     * Generates and saves a performance summary for a driver identified by their acronym.
     *
     * @param acronym The 3-letter acronym of the driver (e.g., "VER").
     * @return The generated [DriverSummary], or null if driver not found or generation failed.
     */
    suspend fun updateDriverSummary(acronym: String): DriverSummary?

    /**
     * Generates and saves performance summaries for a list of driver acronyms.
     *
     * @param acronyms List of driver acronyms.
     * @return A list of successfully generated [DriverSummary] objects.
     */
    suspend fun updateDriverSummaries(acronyms: List<String>): List<DriverSummary>

    /**
     * Generates and saves performance summaries for all active drivers.
     *
     * @return A list of all successfully generated [DriverSummary] objects.
     */
    suspend fun updateAllDriversSummaries(): List<DriverSummary>

    /**
     * Retrieves drivers who participated in the specified F1 sessions.
     *
     * @param sessionKeys List of OpenF1 session keys.
     * @return A [Flow] of [Driver] objects.
     */
    suspend fun getDriversInSessions(sessionKeys: List<Int>): Flow<Driver>

    /**
     * Calculates the average fantasy score of a driver for a specific year.
     *
     * @param year The year to calculate the average for.
     * @param driverId The unique identifier of the driver (optional).
     * @param driverAcronym The acronym of the driver (optional).
     * @return A [RaceWeekendResult.Companion.Result] containing the average points.
     * @throws net.battaglini.fantaf1appbackend.exception.DriverNotFoundException if the driver cannot be found.
     */
    suspend fun calculateDriverAverageScore(
        year: Int,
        driverId: String? = null,
        driverAcronym: String? = null
    ): RaceWeekendResult.Companion.Result
}
