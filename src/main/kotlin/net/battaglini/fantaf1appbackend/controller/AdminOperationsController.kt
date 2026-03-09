package net.battaglini.fantaf1appbackend.controller

import net.battaglini.fantaf1appbackend.exception.DriverNotFoundException
import net.battaglini.fantaf1appbackend.exception.InvalidRequestException
import net.battaglini.fantaf1appbackend.model.request.UpdateDriversCostsRequest
import net.battaglini.fantaf1appbackend.service.DriverService
import org.springframework.http.HttpEntity
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.ObjectMapper

@RestController
class AdminOperationsController(
    private val driverService: DriverService,
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

}