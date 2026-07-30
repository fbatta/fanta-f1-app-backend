package net.battaglini.fantaf1appbackend.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import net.battaglini.fantaf1appbackend.exception.InvalidRequestException
import net.battaglini.fantaf1appbackend.model.request.CopyLineupRequest
import net.battaglini.fantaf1appbackend.model.request.CreateLineupRequest
import net.battaglini.fantaf1appbackend.model.response.CopyLineupResponse
import net.battaglini.fantaf1appbackend.model.response.CreateLineupResponse
import net.battaglini.fantaf1appbackend.service.LineupService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.http.codec.autoconfigure.CodecsAutoConfiguration
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf
import org.springframework.test.web.reactive.server.WebTestClient

@WebFluxTest(
    controllers = [LineupsController::class],
    excludeAutoConfiguration = [CodecsAutoConfiguration::class]
)
class LineupsControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var lineupService: LineupService

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `createLineup should return 200 OK when successful`() {
        val request = CreateLineupRequest(
            teamId = "team1",
            raceId = "race1",
            driverIds = listOf("id-VER", "id-HAM")
        )
        val response = CreateLineupResponse(
            lineupId = "lineup-team1-race1",
            teamId = "team1",
            raceId = "race1",
            drivers = listOf(
                CreateLineupResponse.LineupDriverDto("id-VER", 1, "VER", 10.0),
                CreateLineupResponse.LineupDriverDto("id-HAM", 44, "HAM", 8.0)
            )
        )
        coEvery { lineupService.createLineup(any()) } returns response

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/lineups")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.lineupId").isEqualTo("lineup-team1-race1")
            .jsonPath("$.teamId").isEqualTo("team1")
            .jsonPath("$.raceId").isEqualTo("race1")
            .jsonPath("$.drivers.length()").isEqualTo(2)
            .jsonPath("$.drivers[0].driverId").isEqualTo("id-VER")
            .jsonPath("$.drivers[0].driverNumber").isEqualTo(1)
            .jsonPath("$.drivers[0].driverAcronym").isEqualTo("VER")
            .jsonPath("$.drivers[0].driverCost").isEqualTo(10.0)
            .jsonPath("$.drivers[1].driverId").isEqualTo("id-HAM")
            .jsonPath("$.drivers[1].driverNumber").isEqualTo(44)
            .jsonPath("$.drivers[1].driverAcronym").isEqualTo("HAM")
            .jsonPath("$.drivers[1].driverCost").isEqualTo(8.0)

        coVerify { lineupService.createLineup(any()) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `createLineup should return 400 when validation fails`() {
        val request = CreateLineupRequest(
            teamId = "team1",
            raceId = "race1",
            driverIds = listOf("id-VER")
        )
        coEvery { lineupService.createLineup(any()) } throws InvalidRequestException("Team with teamId=team1 not found")

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/lineups")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `createLineup should return 500 when service fails`() {
        val request = CreateLineupRequest(
            teamId = "team1",
            raceId = "race1",
            driverIds = listOf("id-VER")
        )
        coEvery { lineupService.createLineup(any()) } throws RuntimeException("Unexpected error")

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/lineups")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().is5xxServerError
    }

    @Test
    @WithMockUser(roles = ["DRIVERS_MANAGER"])
    fun `createLineup should allow DRIVERS_MANAGER role`() {
        val request = CreateLineupRequest(
            teamId = "team1",
            raceId = "race1",
            driverIds = listOf("id-VER")
        )
        val response = CreateLineupResponse(
            lineupId = "lineup-team1-race1",
            teamId = "team1",
            raceId = "race1",
            drivers = listOf(
                CreateLineupResponse.LineupDriverDto("id-VER", 1, "VER", 10.0)
            )
        )
        coEvery { lineupService.createLineup(any()) } returns response

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/lineups")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `createLineup should return 403 when user has no role`() {
        val request = CreateLineupRequest(
            teamId = "team1",
            raceId = "race1",
            driverIds = listOf("id-VER")
        )

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/lineups")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `copyLineup should return 200 OK when successful`() {
        val request = CopyLineupRequest(
            teamId = "team1",
            sourceRaceId = "race1",
            targetRaceId = "race2"
        )
        val response = CopyLineupResponse(
            lineupId = "lineup-team1-race2",
            teamId = "team1",
            targetRaceId = "race2",
            drivers = listOf(
                CopyLineupResponse.LineupDriverDto("id-VER", 1, "VER", 12.0)
            )
        )
        coEvery { lineupService.copyLineup(any()) } returns response

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/lineups/copy")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.lineupId").isEqualTo("lineup-team1-race2")
            .jsonPath("$.teamId").isEqualTo("team1")
            .jsonPath("$.targetRaceId").isEqualTo("race2")
            .jsonPath("$.drivers.length()").isEqualTo(1)
            .jsonPath("$.drivers[0].driverId").isEqualTo("id-VER")
            .jsonPath("$.drivers[0].driverCost").isEqualTo(12.0)

        coVerify { lineupService.copyLineup(any()) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `copyLineup should return 400 when validation fails`() {
        val request = CopyLineupRequest(
            teamId = "team1",
            sourceRaceId = "race1",
            targetRaceId = "race2"
        )
        coEvery { lineupService.copyLineup(any()) } throws InvalidRequestException("Team with teamId=team1 not found")

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/lineups/copy")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `copyLineup should return 500 when service fails`() {
        val request = CopyLineupRequest(
            teamId = "team1",
            sourceRaceId = "race1",
            targetRaceId = "race2"
        )
        coEvery { lineupService.copyLineup(any()) } throws RuntimeException("Unexpected error")

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/lineups/copy")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().is5xxServerError
    }

    @Test
    @WithMockUser(roles = ["DRIVERS_MANAGER"])
    fun `copyLineup should allow DRIVERS_MANAGER role`() {
        val request = CopyLineupRequest(
            teamId = "team1",
            sourceRaceId = "race1",
            targetRaceId = "race2"
        )
        val response = CopyLineupResponse(
            lineupId = "lineup-team1-race2",
            teamId = "team1",
            targetRaceId = "race2",
            drivers = listOf(
                CopyLineupResponse.LineupDriverDto("id-VER", 1, "VER", 12.0)
            )
        )
        coEvery { lineupService.copyLineup(any()) } returns response

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/lineups/copy")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `copyLineup should return 401 when user has no role`() {
        val request = CopyLineupRequest(
            teamId = "team1",
            sourceRaceId = "race1",
            targetRaceId = "race2"
        )

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/lineups/copy")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isUnauthorized
    }
}
