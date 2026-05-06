package net.battaglini.fantaf1appbackend.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import net.battaglini.fantaf1appbackend.model.DriverSummary
import net.battaglini.fantaf1appbackend.model.request.UpdateDriversPricesRequest
import net.battaglini.fantaf1appbackend.model.request.UpdateDriversSummariesRequest
import net.battaglini.fantaf1appbackend.model.response.DriverPriceUpdateDetails
import net.battaglini.fantaf1appbackend.service.DriverPricingService
import net.battaglini.fantaf1appbackend.service.DriverService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.http.codec.autoconfigure.CodecsAutoConfiguration
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf
import org.springframework.test.web.reactive.server.WebTestClient

@WebFluxTest(
    controllers = [DriversController::class],
    excludeAutoConfiguration = [CodecsAutoConfiguration::class]
)
class DriversControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var driverService: DriverService

    @MockkBean
    private lateinit var driverPricingService: DriverPricingService

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `seedDrivers should return 200 OK when successful`() {
        coEvery { driverService.seedDrivers() } returns Unit

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/drivers/seed")
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
            .uri("/drivers/seed")
            .exchange()
            .expectStatus().is5xxServerError

        coVerify { driverService.seedDrivers() }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `updateDriversSummaries should return 200 OK when request is valid`() {
        val request = UpdateDriversSummariesRequest(acronyms = listOf("VER", "LEC"))
        val mockSummary = DriverSummary(
            driverId = "d1",
            driverName = "Max Verstappen",
            driverAcronym = "VER",
            driverNumber = 1,
            summaryParagraphs = listOf("Great season so far.")
        )
        coEvery { driverService.updateDriverSummaries(listOf("VER", "LEC")) } returns listOf(mockSummary)

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/drivers/summary")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.summaries[0].driverId").isEqualTo("d1")
            .jsonPath("$.summaries[0].driverAcronym").isEqualTo("VER")
            .jsonPath("$.summaries[0].summaryParagraphs[0]").isEqualTo("Great season so far.")

        coVerify { driverService.updateDriverSummaries(listOf("VER", "LEC")) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `updateDriversSummaries should return 500 when service fails`() {
        val request = UpdateDriversSummariesRequest(acronyms = listOf("VER"))
        coEvery { driverService.updateDriverSummaries(any()) } throws RuntimeException("GenAI error")

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/drivers/summary")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().is5xxServerError
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `updateDriversPrices should return 200 OK when request is valid`() {
        val request = UpdateDriversPricesRequest(
            acronyms = listOf("VER"),
            updateAllDrivers = false
        )
        val mockUpdate = DriverPriceUpdateDetails(
            driverId = "d1",
            acronym = "VER",
            previousPrice = 40.0,
            newPrice = 45.0,
            percentageChange = 12.5
        )
        coEvery { driverPricingService.calculateAndUpdatePrices(acronyms = any(), updateAll = any()) } returns listOf(
            mockUpdate
        )

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/drivers/price")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.updates[0].driverId").isEqualTo("d1")
            .jsonPath("$.updates[0].acronym").isEqualTo("VER")
            .jsonPath("$.updates[0].newPrice").isEqualTo(45.0)

        coVerify { driverPricingService.calculateAndUpdatePrices(acronyms = listOf("VER"), updateAll = false) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `updateDriversPrices should return 500 when service fails`() {
        val request = UpdateDriversPricesRequest(
            updateAllDrivers = true
        )
        coEvery { driverPricingService.calculateAndUpdatePrices(updateAll = true) } throws RuntimeException("Error")

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/drivers/price")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().is5xxServerError
    }
}
