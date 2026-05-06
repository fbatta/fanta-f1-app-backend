package net.battaglini.fantaf1appbackend.controller

import net.battaglini.fantaf1appbackend.model.request.UpdateDriversPricesRequest
import net.battaglini.fantaf1appbackend.model.request.UpdateDriversSummariesRequest
import net.battaglini.fantaf1appbackend.model.response.DriverPriceUpdateResponse
import net.battaglini.fantaf1appbackend.model.response.DriverSummariesResponse
import net.battaglini.fantaf1appbackend.service.DriverPricingService
import net.battaglini.fantaf1appbackend.service.DriverService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/drivers"])
class DriversController(
    private val driverService: DriverService,
    private val driverPricingService: DriverPricingService
) {
    @PostMapping("/seed")
    suspend fun seedDrivers() {
        try {
            driverService.seedDrivers()
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }
    }

    @PostMapping("/summary")
    suspend fun updateDriversSummaries(@RequestBody body: UpdateDriversSummariesRequest): DriverSummariesResponse {
        val summaries = driverService.updateDriverSummaries(body.acronyms)
        return DriverSummariesResponse(summaries)
    }

    @PostMapping("/price")
    suspend fun updateDriversPrices(@RequestBody request: UpdateDriversPricesRequest): DriverPriceUpdateResponse {
        try {
            val updates = driverPricingService.calculateAndUpdatePrices(
                acronyms = request.acronyms,
                updateAll = request.updateAllDrivers
            )
            return DriverPriceUpdateResponse(updates)
        } catch (e: Exception) {
            LOGGER.error("Failed to manually update driver prices", e)
            throw RuntimeException(e.message)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DriversController::class.java)
    }
}