package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.any
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import net.battaglini.fantaf1appbackend.client.OpenF1Client
import net.battaglini.fantaf1appbackend.model.Driver
import net.battaglini.fantaf1appbackend.repository.DriverRepository
import org.springframework.stereotype.Service

@Service
class DriverService(
    private val openF1Client: OpenF1Client,
    private val driverRepository: DriverRepository
) {
    suspend fun getDriversInSession(sessionKey: Int): Flow<Driver> {
        val openF1Drivers = openF1Client.getDrivers(sessionKey = sessionKey)
        val driversInRepo = driverRepository.getDrivers()

        return driversInRepo.filter { driver ->
            openF1Drivers.map { it.driverNumber }.any { it == driver.driverNumber }
        }
    }
}