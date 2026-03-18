package net.battaglini.fantaf1appbackend.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import net.battaglini.fantaf1appbackend.exception.DriverNotFoundException
import net.battaglini.fantaf1appbackend.model.request.UpdateDriversCostsRequest
import net.battaglini.fantaf1appbackend.service.DriverService
import net.battaglini.fantaf1appbackend.service.RaceWeekendService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.http.codec.autoconfigure.CodecsAutoConfiguration
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf
import org.springframework.test.web.reactive.server.WebTestClient
import tools.jackson.databind.ObjectMapper

@WebFluxTest(
    controllers = [AdminOperationsController::class],
    excludeAutoConfiguration = [CodecsAutoConfiguration::class]
)
class AdminOperationsControllerTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var driverService: DriverService

    @MockkBean
    private lateinit var raceWeekendService: RaceWeekendService

    @MockkBean
    private lateinit var objectMapper: ObjectMapper


    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `updateDriversCosts should return 200 OK when request is valid`() {
        val requestBody = """
            {
                "driversCosts": [
                    {
                        "acronym": "VER",
                        "driverCost": 40.0
                    }
                ]
            }
        """.trimIndent()

        val parsedCosts = UpdateDriversCostsRequest(
            driversCosts = listOf(
                UpdateDriversCostsRequest.Companion.DriverCostRequest(
                    acronym = "VER",
                    driverCost = 40.0
                )
            )
        )

        every { objectMapper.readValue(any<String>(), UpdateDriversCostsRequest::class.java) } returns parsedCosts
        coEvery { driverService.updateDriversCosts(any()) } returns Unit

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/admin/drivers/costs")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus().isOk

        coVerify { driverService.updateDriversCosts(parsedCosts) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `updateDriversCosts should return 400 Bad Request when DriverNotFoundException is thrown`() {
        val requestBody = """
            {
                "driversCosts": [
                    {
                        "acronym": "UNKNOWN",
                        "driverCost": 40.0
                    }
                ]
            }
        """.trimIndent()

        val parsedCosts = UpdateDriversCostsRequest(
            driversCosts = listOf(
                UpdateDriversCostsRequest.Companion.DriverCostRequest(
                    acronym = "UNKNOWN",
                    driverCost = 40.0
                )
            )
        )

        every { objectMapper.readValue(any<String>(), UpdateDriversCostsRequest::class.java) } returns parsedCosts
        coEvery { driverService.updateDriversCosts(any()) } throws DriverNotFoundException("Driver not found")

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/admin/drivers/costs")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus().isBadRequest

        coVerify { driverService.updateDriversCosts(parsedCosts) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `updateDriversCosts should return 500 Internal Server Error when generic Exception is thrown`() {
        val requestBody = """
            {
                "driversCosts": [
                    {
                        "acronym": "VER",
                        "driverCost": 40.0
                    }
                ]
            }
        """.trimIndent()

        val parsedCosts = UpdateDriversCostsRequest(
            driversCosts = listOf(
                UpdateDriversCostsRequest.Companion.DriverCostRequest(
                    acronym = "VER",
                    driverCost = 40.0
                )
            )
        )

        every { objectMapper.readValue(any<String>(), UpdateDriversCostsRequest::class.java) } returns parsedCosts
        coEvery { driverService.updateDriversCosts(any()) } throws Exception("Something went wrong")

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/admin/drivers/costs")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus().is5xxServerError

        coVerify { driverService.updateDriversCosts(parsedCosts) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `seedDrivers should return 200 OK when successful`() {
        coEvery { driverService.seedDrivers() } returns Unit

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/admin/drivers/seed")
            .exchange()
            .expectStatus().isOk

        coVerify { driverService.seedDrivers() }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `seedDrivers should return 500 Internal Server Error when Exception is thrown`() {
        coEvery { driverService.seedDrivers() } throws Exception("Failed to seed")

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/admin/drivers/seed")
            .exchange()
            .expectStatus().is5xxServerError

        coVerify { driverService.seedDrivers() }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `seedRaceWeekends should return 200 OK when successful`() {
        coEvery { raceWeekendService.seedRaceWeekends() } returns Unit

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/admin/race-weekends/seed")
            .exchange()
            .expectStatus().isOk

        coVerify { raceWeekendService.seedRaceWeekends() }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `seedRaceWeekends should return 500 Internal Server Error when Exception is thrown`() {
        coEvery { raceWeekendService.seedRaceWeekends() } throws Exception("Failed to seed")

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/admin/race-weekends/seed")
            .exchange()
            .expectStatus().is5xxServerError

        coVerify { raceWeekendService.seedRaceWeekends() }
    }
}
