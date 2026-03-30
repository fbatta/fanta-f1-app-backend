package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.battaglini.fantaf1appbackend.client.OpenF1Client
import net.battaglini.fantaf1appbackend.configuration.SeedingProperties
import net.battaglini.fantaf1appbackend.exception.DriverNotFoundException
import net.battaglini.fantaf1appbackend.model.Driver
import net.battaglini.fantaf1appbackend.model.DriverCost
import net.battaglini.fantaf1appbackend.model.DriverSummary
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1DriverResponse.Companion.toDriver
import net.battaglini.fantaf1appbackend.model.request.UpdateDriversCostsRequest
import net.battaglini.fantaf1appbackend.repository.*
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Service
class DriverService(
    private val openF1Client: OpenF1Client,
    private val genAIService: GenAIService,
    private val driverRepository: DriverRepository,
    private val driverCostRepository: DriverCostRepository,
    private val driverSummaryRepository: DriverSummaryRepository,
    private val raceWeekendResultRepository: RaceWeekendResultRepository,
    private val raceRepository: RaceRepository,
    private val seedingProperties: SeedingProperties,
    private val clock: Clock,
    private val timeZone: TimeZone
) {
    @EventListener(ApplicationStartedEvent::class)
    suspend fun onStart() {
        if (!seedingProperties.drivers) {
            LOGGER.info("Skipping F1 drivers' seeding because it is disabled in app config")
        } else {
            seedDrivers()
        }
    }

    suspend fun seedDrivers() {
        try {
            LOGGER.info("Seeding F1 drivers...")
            val drivers = openF1Client.getDrivers(sessionKeys = listOf("latest")).map {
                it.toDriver(
                    calculateDriverId(it.driverNumber, it.nameAcronym)
                )
            }.toList()
            driverRepository.createOrUpdateDrivers(drivers)
            LOGGER.info("Seeded {} drivers into Firebase", drivers.size)
        } catch (e: Exception) {
            LOGGER.error("Failed to seed drivers to Firebase", e)
        }
    }

    suspend fun updateDriversCosts(costs: UpdateDriversCostsRequest) {
        val drivers = driverRepository.getDrivers().toList()
        val driversCosts = costs.driversCosts.map { cost ->
            val driver = drivers.find { it.acronym.equals(cost.acronym, ignoreCase = true) }
            if (driver != null) {
                DriverCost(driver.driverId, cost.driverCost)
            } else {
                throw DriverNotFoundException("Driver with acronym ${cost.acronym} not found")
            }
        }
        driverCostRepository.createOrUpdateDriversCosts(driversCosts)
    }

    suspend fun updateDriverSummary(acronym: String) {
        val driver = driverRepository.findDriverByAcronym(acronym)
        if (driver != null) {
            LOGGER.debug("Updating F1 driver summary for driver={}", driver.name)
            val averageScore =
                calculateDriverAverageScore(
                    clock.now().toLocalDateTime(timeZone).year,
                    driver.driverId
                ).points
            val paragraphs = genAIService.generateDriverSummary(driver.name, averageScore).toList()

            if (paragraphs.isEmpty()) {
                LOGGER.warn("Could not generate summary for driver={}", driver.name)
                return
            }
            val driverSummary =
                DriverSummary(driver.driverId, driver.name, driver.acronym, driver.driverNumber, paragraphs)

            driverSummaryRepository.createOrUpdateDriverSummary(driverSummary)
        }
    }

    suspend fun getDriversInSessions(sessionKeys: List<Int>): Flow<Driver> {
        val openF1Drivers = openF1Client.getDrivers(sessionKeys = sessionKeys.map { it.toString() }).toList()
        val driversInRepo = driverRepository.getDrivers()

        return driversInRepo.filter { driver ->
            openF1Drivers.any { it.nameAcronym == driver.acronym }
        }
    }

    /**
     * Calculates the average score of a driver for a specific year.
     *
     * @param year The year to calculate the average for.
     * @param driverId The unique identifier of the driver (optional if driverAcronym is provided).
     * @param driverAcronym The acronym of the driver (optional if driverId is provided).
     * @return A [RaceWeekendResult.Companion.Result] containing the average points.
     * @throws DriverNotFoundException if the driver cannot be found.
     */
    suspend fun calculateDriverAverageScore(
        year: Int,
        driverId: String? = null,
        driverAcronym: String? = null
    ): RaceWeekendResult.Companion.Result {
        if (driverId == null && driverAcronym == null) {
            throw IllegalArgumentException("Either driverId or driverAcronym must be provided")
        }

        val driver =
            driverId?.let { id -> driverRepository.findDriverById(id) } ?: driverRepository.findDriverByAcronym(
                driverAcronym!!
            )

        if (driver == null) {
            LOGGER.error(
                "Could not calculate driver average score for driverId={} or driverAcronym={}",
                driverId,
                driverAcronym
            )
            throw DriverNotFoundException("Driver with id=$driverId or acronym=$driverAcronym")
        }

        val races = raceRepository.getRacesByYear(year).toList()
        if (races.isEmpty()) {
            LOGGER.error("No races found for year={}", year)
            throw IllegalStateException("No races found for year=$year")
        }

        val results = raceWeekendResultRepository.getRaceWeekendResults(races.map { it.raceId }).toList()
        val average = results
            .map { result ->
                result.results.find { it.driverId == driver.driverId }
            }
            .foldRight(0.0) { driverResult, acc -> (driverResult?.points ?: 0.0) + acc } / results.size

        return RaceWeekendResult.Companion.Result(
            driverId = driver.driverId,
            driverNumber = driver.driverNumber,
            driverAcronym = driver.acronym,
            points = average,
        )
    }

    private fun calculateDriverId(driverNumber: Int, driverAcronym: String): String {
        return Uuid.fromULongs(driverAcronym.toByteArray().sum().toULong(), driverNumber.toULong()).toString()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DriverService::class.java)
    }
}