package net.battaglini.fantaf1appbackend.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
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
    private lateinit var driverPricingService: net.battaglini.fantaf1appbackend.service.DriverPricingService

    @MockkBean
    private lateinit var raceWeekendService: RaceWeekendService

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

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `updateDriversPrices should return 200 OK when request is valid`() {
        val request = net.battaglini.fantaf1appbackend.model.request.UpdateDriversPricesRequest(
            acronyms = listOf("VER"),
            updateAllDrivers = false
        )
        val mockUpdate = net.battaglini.fantaf1appbackend.model.response.DriverPriceUpdateDetails(
            driverId = "d1",
            acronym = "VER",
            previousPrice = 40.0,
            newPrice = 45.0,
            percentageChange = 12.5
        )
        coEvery { driverPricingService.calculateAndUpdatePrices(acronyms = any(), updateAll = any()) } returns listOf(mockUpdate)

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/admin/drivers/prices")
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
        val request = net.battaglini.fantaf1appbackend.model.request.UpdateDriversPricesRequest(
            updateAllDrivers = true
        )
        coEvery { driverPricingService.calculateAndUpdatePrices(updateAll = true) } throws RuntimeException("Error")

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/admin/drivers/prices")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().is5xxServerError
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `generateRaceRecaps should return 200 OK when successful`() {
        val request = net.battaglini.fantaf1appbackend.model.request.GenerateRaceRecapRequest(
            raceIds = listOf("race-1", "race-2")
        )
      val mockRecaps = listOf(
            net.battaglini.fantaf1appbackend.model.RaceWeekendRecap("race-1", "Monaco Grand Prix", listOf("Para 1", "Para 2")),
            net.battaglini.fantaf1appbackend.model.RaceWeekendRecap("race-2", "British Grand Prix", listOf("Para A"))
        )
        coEvery { raceWeekendService.generateRaceRecap(any()) } returns mockRecaps

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/admin/race-weekends/recap")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.recapIds").isArray
            .jsonPath("$.recapIds.length()").isEqualTo(2)
            .jsonPath("$.recapIds[0]").isEqualTo("race-1")
            .jsonPath("$.recapIds[1]").isEqualTo("race-2")
            .jsonPath("$.recaps").isArray
            .jsonPath("$.recaps.length()").isEqualTo(2)
            .jsonPath("$.recaps[0].raceId").isEqualTo("race-1")
            .jsonPath("$.recaps[0].raceName").isEqualTo("Monaco Grand Prix")
            .jsonPath("$.recaps[0].recapParagraphs").isArray
            .jsonPath("$.recaps[0].recapParagraphs.length()").isEqualTo(2)
            .jsonPath("$.recaps[1].raceId").isEqualTo("race-2")
            .jsonPath("$.recaps[1].raceName").isEqualTo("British Grand Prix")

        coVerify { raceWeekendService.generateRaceRecap(listOf("race-1", "race-2")) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `generateRaceRecaps should return 500 when service fails`() {
        val request = net.battaglini.fantaf1appbackend.model.request.GenerateRaceRecapRequest(
            raceIds = listOf("race-1")
        )
        coEvery { raceWeekendService.generateRaceRecap(any()) } throws RuntimeException("GenAI error")

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/admin/race-weekends/recap")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().is5xxServerError
    }

  @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `generateRaceRecaps should return empty recapIds and recaps when no races processed`() {
        val request = net.battaglini.fantaf1appbackend.model.request.GenerateRaceRecapRequest(
            raceIds = listOf("nonexistent")
        )
        coEvery { raceWeekendService.generateRaceRecap(any()) } returns emptyList()

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/admin/race-weekends/recap")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.recapIds").isEmpty
            .jsonPath("$.recaps").isEmpty
    }
}
