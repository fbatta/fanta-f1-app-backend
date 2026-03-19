package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import net.battaglini.fantaf1appbackend.client.OpenF1Client
import net.battaglini.fantaf1appbackend.configuration.SeedingProperties
import net.battaglini.fantaf1appbackend.exception.DriverNotFoundException
import net.battaglini.fantaf1appbackend.model.Driver
import net.battaglini.fantaf1appbackend.model.DriverCost
import net.battaglini.fantaf1appbackend.model.DriverSummary
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1DriverResponse.Companion.toDriver
import net.battaglini.fantaf1appbackend.model.request.UpdateDriversCostsRequest
import net.battaglini.fantaf1appbackend.repository.DriverCostRepository
import net.battaglini.fantaf1appbackend.repository.DriverRepository
import net.battaglini.fantaf1appbackend.repository.DriverSummaryRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
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
    private val seedingProperties: SeedingProperties
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
            val paragraphs = genAIService.generateDriverSummary(driver.name).toList()

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

    private fun calculateDriverId(driverNumber: Int, driverAcronym: String): String {
        return Uuid.fromULongs(driverAcronym.toByteArray().sum().toULong(), driverNumber.toULong()).toString()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DriverService::class.java)
    }
}