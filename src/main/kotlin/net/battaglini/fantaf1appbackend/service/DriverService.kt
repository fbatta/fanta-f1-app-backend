package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.*
import net.battaglini.fantaf1appbackend.client.OpenF1Client
import net.battaglini.fantaf1appbackend.model.Driver
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1DriverResponse.Companion.toDriver
import net.battaglini.fantaf1appbackend.repository.DriverRepository
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
    private val driverRepository: DriverRepository
) {
    @EventListener(ApplicationStartedEvent::class)
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

    suspend fun getDriversInSessions(sessionKeys: List<Int>): Flow<Driver> {
        val openF1Drivers = openF1Client.getDrivers(sessionKeys = sessionKeys.map { it.toString() })
        val driversInRepo = driverRepository.getDrivers()

        return driversInRepo.filter { driver ->
            openF1Drivers.map { it.driverNumber }.any { it == driver.driverNumber }
        }
    }

    private fun calculateDriverId(driverNumber: Int, driverAcronym: String): String {
        return Uuid.fromULongs(driverAcronym.toByteArray().sum().toULong(), driverNumber.toULong()).toString()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DriverService::class.java)
    }
}