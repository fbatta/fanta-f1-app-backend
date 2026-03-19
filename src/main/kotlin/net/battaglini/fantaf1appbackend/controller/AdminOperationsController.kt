package net.battaglini.fantaf1appbackend.controller

import net.battaglini.fantaf1appbackend.exception.DriverNotFoundException
import net.battaglini.fantaf1appbackend.exception.InvalidRequestException
import net.battaglini.fantaf1appbackend.model.request.UpdateDriversCostsRequest
import net.battaglini.fantaf1appbackend.model.request.UpdateDriversSummariesRequest
import net.battaglini.fantaf1appbackend.service.DriverService
import net.battaglini.fantaf1appbackend.service.RaceWeekendService
import org.springframework.http.HttpEntity
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.ObjectMapper

@RestController
class AdminOperationsController(
    private val driverService: DriverService,
    private val raceWeekendService: RaceWeekendService,
    private val objectMapper: ObjectMapper
) {
    @PostMapping("/admin/drivers/costs", consumes = [MediaType.APPLICATION_JSON_VALUE])
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

    @PostMapping("/admin/drivers/seed")
    suspend fun seedDrivers() {
        try {
            driverService.seedDrivers()
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }
    }

    @PostMapping("/admin/race-weekends/seed")
    suspend fun seedRaceWeekends() {
        try {
            raceWeekendService.seedRaceWeekends()
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }
    }
    @PostMapping("/admin/drivers/summaries")
    suspend fun updateDriversSummaries(@RequestBody body: UpdateDriversSummariesRequest) {
        for (acronym in body.acronyms) {
            driverService.updateDriverSummary(acronym)
        }
    }
}