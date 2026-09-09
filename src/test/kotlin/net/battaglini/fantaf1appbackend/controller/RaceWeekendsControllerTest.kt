package net.battaglini.fantaf1appbackend.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.datetime.DateTimeUnit
import net.battaglini.fantaf1appbackend.model.request.GenerateRaceRecapRequest
import net.battaglini.fantaf1appbackend.model.request.OpenRaceWeekendLineupRequest
import net.battaglini.fantaf1appbackend.service.RaceWeekendService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.boot.http.codec.autoconfigure.CodecsAutoConfiguration
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf
import org.springframework.test.web.reactive.server.WebTestClient
import net.battaglini.fantaf1appbackend.configuration.WebfluxSecurityConfiguration

@WebFluxTest(
    controllers = [RaceWeekendsController::class],
    excludeAutoConfiguration = [CodecsAutoConfiguration::class]
)
@Import(WebfluxSecurityConfiguration::class)
class RaceWeekendsControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var raceWeekendService: RaceWeekendService

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `seedRaceWeekends should return 200 OK when successful`() {
        coEvery { raceWeekendService.seedRaceWeekends() } returns Unit

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/race-weekends/seed")
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
            .uri("/race-weekends/seed")
            .exchange()
            .expectStatus().is5xxServerError

        coVerify { raceWeekendService.seedRaceWeekends() }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `generateRaceRecaps should return 200 OK when successful`() {
        val request = GenerateRaceRecapRequest(
            raceIds = listOf("race-1", "race-2")
        )
        val mockRecaps = listOf(
            net.battaglini.fantaf1appbackend.model.RaceWeekendRecap(
                "race-1",
                "Monaco Grand Prix",
                listOf("Para 1", "Para 2")
            ),
            net.battaglini.fantaf1appbackend.model.RaceWeekendRecap("race-2", "British Grand Prix", listOf("Para A"))
        )
        coEvery { raceWeekendService.generateRaceRecap(any()) } returns mockRecaps

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/race-weekends/recap")
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
        val request = GenerateRaceRecapRequest(
            raceIds = listOf("race-1")
        )
        coEvery { raceWeekendService.generateRaceRecap(any()) } throws RuntimeException("GenAI error")

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/race-weekends/recap")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().is5xxServerError
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `generateRaceRecaps should return empty recapIds and recaps when no races processed`() {
        val request = GenerateRaceRecapRequest(
            raceIds = listOf("nonexistent")
        )
        coEvery { raceWeekendService.generateRaceRecap(any()) } returns emptyList()

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/race-weekends/recap")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.recapIds").isEmpty
            .jsonPath("$.recaps").isEmpty
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `seedRaceWeekends should return 403 Forbidden when user has USER role`() {
        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/race-weekends/seed")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    @WithMockUser(roles = ["RACE_WEEKEND_MANAGER"])
    fun `seedRaceWeekends should return 403 Forbidden when user has RACE_WEEKEND_MANAGER role`() {
        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/race-weekends/seed")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `seedRaceWeekends should return 401 Unauthorized when unauthenticated`() {
        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/race-weekends/seed")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `openRaceWeekendLineup should return 200 and race name when opened without delay`() {
        val request = OpenRaceWeekendLineupRequest(raceId = "race-1")
        coEvery { raceWeekendService.setRaceWeekendLineupOpenDateTime("race-1", any()) } returns "Monaco Grand Prix"

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/race-weekends/open-lineup")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .isEqualTo("Monaco Grand Prix")

        coVerify { raceWeekendService.setRaceWeekendLineupOpenDateTime("race-1", any()) }
    }

    @Test
    @WithMockUser(roles = ["RACE_WEEKEND_MANAGER"])
    fun `openRaceWeekendLineup should return 200 and race name when called by RACE_WEEKEND_MANAGER`() {
        val request = OpenRaceWeekendLineupRequest(raceId = "race-1", delay = 2, delayUnit = DateTimeUnit.HOUR)
        coEvery { raceWeekendService.setRaceWeekendLineupOpenDateTime("race-1", any()) } returns "Monaco Grand Prix"

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/race-weekends/open-lineup")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .isEqualTo("Monaco Grand Prix")

        coVerify { raceWeekendService.setRaceWeekendLineupOpenDateTime("race-1", any()) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `openRaceWeekendLineup should return error string when delay is set without delayUnit`() {
        val request = OpenRaceWeekendLineupRequest(raceId = "race-1", delay = 5, delayUnit = null)

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/race-weekends/open-lineup")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .isEqualTo("Could not update lineup date for raceId=race-1. delayUnit cannot be null")
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `openRaceWeekendLineup should return error string when service returns null`() {
        val request = OpenRaceWeekendLineupRequest(raceId = "race-1")
        coEvery { raceWeekendService.setRaceWeekendLineupOpenDateTime("race-1", any()) } returns null

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/race-weekends/open-lineup")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .isEqualTo("Could not update lineup date for raceId=race-1. Could not update lineup date for raceId=race-1")
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `openRaceWeekendLineup should return error string when service throws Exception`() {
        val request = OpenRaceWeekendLineupRequest(raceId = "race-1")
        coEvery { raceWeekendService.setRaceWeekendLineupOpenDateTime("race-1", any()) } throws RuntimeException("Service error")

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/race-weekends/open-lineup")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .isEqualTo("Could not update lineup date for raceId=race-1. Service error")
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `openRaceWeekendLineup should return 403 Forbidden when user has USER role`() {
        val request = OpenRaceWeekendLineupRequest(raceId = "race-1")

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/race-weekends/open-lineup")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `openRaceWeekendLineup should return 401 Unauthorized when unauthenticated`() {
        val request = OpenRaceWeekendLineupRequest(raceId = "race-1")

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/race-weekends/open-lineup")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    @WithMockUser(roles = ["RACE_WEEKEND_MANAGER"])
    fun `generateRaceRecaps should return 200 OK when user has RACE_WEEKEND_MANAGER role`() {
        val request = GenerateRaceRecapRequest(raceIds = listOf("race-1"))
        coEvery { raceWeekendService.generateRaceRecap(any()) } returns emptyList()

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/race-weekends/recap")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `generateRaceRecaps should return 403 Forbidden when user has USER role`() {
        val request = GenerateRaceRecapRequest(raceIds = listOf("race-1"))

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/race-weekends/recap")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `generateRaceRecaps should return 401 Unauthorized when unauthenticated`() {
        val request = GenerateRaceRecapRequest(raceIds = listOf("race-1"))

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/race-weekends/recap")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isUnauthorized
    }
}
