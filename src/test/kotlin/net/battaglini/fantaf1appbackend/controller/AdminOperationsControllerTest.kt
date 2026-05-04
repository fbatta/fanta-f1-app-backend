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

    @MockkBean
    private lateinit var teamResultsService: net.battaglini.fantaf1appbackend.service.TeamResultsService

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

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `calculateTeamsResults should return 200 OK when successful`() {
        val request = net.battaglini.fantaf1appbackend.model.request.CalculateTeamsResultsRequest(
            raceId = "race-1"
        )
        val raceWeekend = net.battaglini.fantaf1appbackend.model.RaceWeekend(
            raceId = "race-1",
            openF1MeetingKey = 100,
            raceName = "Monaco Grand Prix",
            dateStart = kotlin.time.Instant.fromEpochMilliseconds(0),
            dateEnd = kotlin.time.Instant.fromEpochMilliseconds(0),
            sessions = emptyList(),
            circuitImage = "circuit.png",
            countryName = "Monaco",
            countryFlag = "🇲🇨",
            circuitType = "street",
            dateLineupOpen = kotlin.time.Instant.fromEpochMilliseconds(0),
            dateLineupClose = kotlin.time.Instant.fromEpochMilliseconds(0)
        )
        val team = net.battaglini.fantaf1appbackend.model.Team(
            teamId = "team1",
            teamName = "Red Bull Racing",
            teamAvatarUrl = null,
            ownerId = "owner1",
            lobbyId = "lobby1",
            createdAt = kotlin.time.Instant.fromEpochMilliseconds(0),
            updatedAt = kotlin.time.Instant.fromEpochMilliseconds(0),
            points = mutableMapOf(2025 to 100.0)
        )
        val lineup = net.battaglini.fantaf1appbackend.model.Lineup(
            lineupId = "lineup1",
            teamId = "team1",
            ownerId = "owner1",
            raceId = "race-1",
            drivers = listOf(
                net.battaglini.fantaf1appbackend.model.Lineup.Companion.LineupDriver("d1", 1, "VER", 10.0)
            ),
            createdAt = kotlin.time.Instant.fromEpochMilliseconds(0),
            updatedAt = kotlin.time.Instant.fromEpochMilliseconds(0),
            version = 1,
            score = 25.0
        )
        coEvery { raceWeekendService.getRaceWeekend("race-1") } returns raceWeekend
        coEvery { raceWeekendService.getRaceWeekendResults("race-1") } returns net.battaglini.fantaf1appbackend.model.RaceWeekendResult(
            raceId = "race-1",
            raceName = "Monaco Grand Prix",
            openF1MeetingKey = 100,
            createdAt = kotlin.time.Instant.fromEpochMilliseconds(0),
            updatedAt = kotlin.time.Instant.fromEpochMilliseconds(0),
            version = 1,
            results = listOf(
                net.battaglini.fantaf1appbackend.model.RaceWeekendResult.Companion.Result("d1", 1, "VER", 25.0)
            )
        )
        coEvery { teamResultsService.calculateAndSaveLineupsResults(any()) } returns mapOf(team to lineup)

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/admin/teams/results")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.raceId").isEqualTo("race-1")
            .jsonPath("$.raceName").isEqualTo("Monaco Grand Prix")
            .jsonPath("$.scores").isArray
            .jsonPath("$.scores.length()").isEqualTo(1)
            .jsonPath("$.scores[0].lineup.score").isEqualTo(25.0)

        coVerify { teamResultsService.calculateAndSaveLineupsResults(any()) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `calculateTeamsResults should return 500 when race weekend not found`() {
        val request = net.battaglini.fantaf1appbackend.model.request.CalculateTeamsResultsRequest(
            raceId = "nonexistent"
        )
        coEvery { raceWeekendService.getRaceWeekend("nonexistent") } returns null

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/admin/teams/results")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().is5xxServerError
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `calculateTeamsResults should return 500 when race weekend results not found`() {
        val request = net.battaglini.fantaf1appbackend.model.request.CalculateTeamsResultsRequest(
            raceId = "race-1"
        )
        val raceWeekend = net.battaglini.fantaf1appbackend.model.RaceWeekend(
            raceId = "race-1",
            openF1MeetingKey = 100,
            raceName = "Monaco Grand Prix",
            dateStart = kotlin.time.Instant.fromEpochMilliseconds(0),
            dateEnd = kotlin.time.Instant.fromEpochMilliseconds(0),
            sessions = emptyList(),
            circuitImage = "circuit.png",
            countryName = "Monaco",
            countryFlag = "🇲🇨",
            circuitType = "street",
            dateLineupOpen = kotlin.time.Instant.fromEpochMilliseconds(0),
            dateLineupClose = kotlin.time.Instant.fromEpochMilliseconds(0)
        )
        coEvery { raceWeekendService.getRaceWeekend("race-1") } returns raceWeekend
        coEvery { raceWeekendService.getRaceWeekendResults("race-1") } returns null

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/admin/teams/results")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().is5xxServerError
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `calculateTeamsResults should return 500 when service fails`() {
        val request = net.battaglini.fantaf1appbackend.model.request.CalculateTeamsResultsRequest(
            raceId = "race-1"
        )
        val raceWeekend = net.battaglini.fantaf1appbackend.model.RaceWeekend(
            raceId = "race-1",
            openF1MeetingKey = 100,
            raceName = "Monaco Grand Prix",
            dateStart = kotlin.time.Instant.fromEpochMilliseconds(0),
            dateEnd = kotlin.time.Instant.fromEpochMilliseconds(0),
            sessions = emptyList(),
            circuitImage = "circuit.png",
            countryName = "Monaco",
            countryFlag = "🇲🇨",
            circuitType = "street",
            dateLineupOpen = kotlin.time.Instant.fromEpochMilliseconds(0),
            dateLineupClose = kotlin.time.Instant.fromEpochMilliseconds(0)
        )
        coEvery { raceWeekendService.getRaceWeekend("race-1") } returns raceWeekend
        coEvery { raceWeekendService.getRaceWeekendResults("race-1") } returns net.battaglini.fantaf1appbackend.model.RaceWeekendResult(
            raceId = "race-1",
            raceName = "Monaco Grand Prix",
            openF1MeetingKey = 100,
            createdAt = kotlin.time.Instant.fromEpochMilliseconds(0),
            updatedAt = kotlin.time.Instant.fromEpochMilliseconds(0),
            version = 1,
            results = emptyList()
        )
        coEvery { teamResultsService.calculateAndSaveLineupsResults(any()) } throws RuntimeException("DB error")

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/admin/teams/results")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().is5xxServerError
    }
}
