package net.battaglini.fantaf1appbackend.controller

import net.battaglini.fantaf1appbackend.exception.DriverNotFoundException
import net.battaglini.fantaf1appbackend.exception.InvalidRequestException
import net.battaglini.fantaf1appbackend.model.request.UpdateDriversCostsRequest
import net.battaglini.fantaf1appbackend.model.request.UpdateDriversPricesRequest
import net.battaglini.fantaf1appbackend.model.request.UpdateDriversSummariesRequest
import net.battaglini.fantaf1appbackend.model.response.DriverSummariesResponse
import net.battaglini.fantaf1appbackend.service.DriverPricingService
import net.battaglini.fantaf1appbackend.service.DriverService
import net.battaglini.fantaf1appbackend.service.RaceWeekendService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.ObjectMapper

@RestController
@RequestMapping(path = ["/admin"])
class AdminOperationsController(
    private val driverService: DriverService,
    private val driverPricingService: DriverPricingService,
    private val raceWeekendService: RaceWeekendService,
    private val objectMapper: ObjectMapper
) {
    @PostMapping("/drivers/costs", consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun updateDriversCosts(requestEntity: HttpEntity<String>) {
        try {
            val request = requestEntity.body
            val costs = objectMapper.readValue(request, UpdateDriversCostsRequest::class.java)
            driverService.updateDriversCosts(costs)
        } catch (e: DriverNotFoundException) {
            throw InvalidRequestException(e.message)
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }
    }

    @PostMapping("/drivers/seed")
    suspend fun seedDrivers() {
        try {
            driverService.seedDrivers()
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }
    }

    @PostMapping("/race-weekends/seed")
    suspend fun seedRaceWeekends() {
        try {
            raceWeekendService.seedRaceWeekends()
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }
    }

    @PostMapping("/drivers/summaries")
    suspend fun updateDriversSummaries(@RequestBody body: UpdateDriversSummariesRequest): DriverSummariesResponse {
        val summaries = driverService.updateDriverSummaries(body.acronyms)
        return DriverSummariesResponse(summaries)
    }

    @PostMapping("/drivers/prices")
    suspend fun updateDriversPrices(@RequestBody request: net.battaglini.fantaf1appbackend.model.request.UpdateDriversPricesRequest) {
        try {
            driverPricingService.calculateAndUpdatePrices(
                acronyms = request.acronyms,
                updateAll = request.updateAllDrivers
            )
        } catch (e: Exception) {
            LOGGER.error("Failed to manually update driver prices", e)
            throw RuntimeException(e.message)
        }
    }

    companion object {
        private val LOGGER = org.slf4j.LoggerFactory.getLogger(AdminOperationsController::class.java)
    }
}