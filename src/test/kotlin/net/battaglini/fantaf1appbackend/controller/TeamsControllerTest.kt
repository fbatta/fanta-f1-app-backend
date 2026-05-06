package net.battaglini.fantaf1appbackend.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import net.battaglini.fantaf1appbackend.model.Lineup
import net.battaglini.fantaf1appbackend.model.RaceWeekend
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import net.battaglini.fantaf1appbackend.model.Team
import net.battaglini.fantaf1appbackend.model.request.CalculateTeamsResultsRequest
import net.battaglini.fantaf1appbackend.service.RaceWeekendService
import net.battaglini.fantaf1appbackend.service.TeamResultsService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.http.codec.autoconfigure.CodecsAutoConfiguration
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf
import org.springframework.test.web.reactive.server.WebTestClient

@WebFluxTest(
    controllers = [TeamsController::class],
    excludeAutoConfiguration = [CodecsAutoConfiguration::class]
)
class TeamsControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var raceWeekendService: RaceWeekendService

    @MockkBean
    private lateinit var teamResultsService: TeamResultsService

    private fun createRaceWeekend(raceId: String, raceName: String): RaceWeekend {
        return RaceWeekend(
            raceId = raceId,
            openF1MeetingKey = 100,
            raceName = raceName,
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
    }

    private fun createTeam(teamId: String): Team {
        return Team(
            teamId = teamId,
            teamName = "Red Bull Racing",
            teamAvatarUrl = null,
            ownerId = "owner1",
            lobbyId = "lobby1",
            createdAt = kotlin.time.Instant.fromEpochMilliseconds(0),
            updatedAt = kotlin.time.Instant.fromEpochMilliseconds(0),
            points = mutableMapOf(2025 to 100.0)
        )
    }

    private fun createLineup(teamId: String, raceId: String, score: Double): Lineup {
        return Lineup(
            lineupId = "lineup1",
            teamId = teamId,
            ownerId = "owner1",
            raceId = raceId,
            drivers = listOf(
                Lineup.Companion.LineupDriver("d1", 1, "VER", 10.0)
            ),
            createdAt = kotlin.time.Instant.fromEpochMilliseconds(0),
            updatedAt = kotlin.time.Instant.fromEpochMilliseconds(0),
            version = 1,
            score = score
        )
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `calculateTeamsResults should return 200 OK when successful`() {
        val request = CalculateTeamsResultsRequest(raceId = "race-1")
        val raceWeekend = createRaceWeekend("race-1", "Monaco Grand Prix")
        val team = createTeam("team1")
        val lineup = createLineup("team1", "race-1", 25.0)
        val raceWeekendResult = RaceWeekendResult(
            raceId = "race-1",
            raceName = "Monaco Grand Prix",
            openF1MeetingKey = 100,
            createdAt = kotlin.time.Instant.fromEpochMilliseconds(0),
            updatedAt = kotlin.time.Instant.fromEpochMilliseconds(0),
            version = 1,
            results = listOf(
                RaceWeekendResult.Companion.Result("d1", 1, "VER", 25.0)
            )
        )
        coEvery { raceWeekendService.getRaceWeekend("race-1") } returns raceWeekend
        coEvery { raceWeekendService.getRaceWeekendResults("race-1") } returns raceWeekendResult
        coEvery { teamResultsService.calculateAndSaveLineupsResults(any()) } returns mapOf(team to lineup)

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/teams/results")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.raceId").isEqualTo("race-1")
            .jsonPath("$.raceName").isEqualTo("Monaco Grand Prix")
            .jsonPath("$.scores").isArray
            .jsonPath("$.scores.length()").isEqualTo(1)
            .jsonPath("$.scores[0].team.teamName").isEqualTo("Red Bull Racing")
            .jsonPath("$.scores[0].lineup.score").isEqualTo(25.0)

        coVerify { teamResultsService.calculateAndSaveLineupsResults(any()) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `calculateTeamsResults should return 500 when race weekend not found`() {
        val request = CalculateTeamsResultsRequest(raceId = "nonexistent")
        coEvery { raceWeekendService.getRaceWeekend("nonexistent") } returns null

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/teams/results")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().is5xxServerError
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `calculateTeamsResults should return 500 when race weekend results not found`() {
        val request = CalculateTeamsResultsRequest(raceId = "race-1")
        val raceWeekend = createRaceWeekend("race-1", "Monaco Grand Prix")
        coEvery { raceWeekendService.getRaceWeekend("race-1") } returns raceWeekend
        coEvery { raceWeekendService.getRaceWeekendResults("race-1") } returns null

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/teams/results")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().is5xxServerError
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `calculateTeamsResults should return 500 when service fails`() {
        val request = CalculateTeamsResultsRequest(raceId = "race-1")
        val raceWeekend = createRaceWeekend("race-1", "Monaco Grand Prix")
        val raceWeekendResult = RaceWeekendResult(
            raceId = "race-1",
            raceName = "Monaco Grand Prix",
            openF1MeetingKey = 100,
            createdAt = kotlin.time.Instant.fromEpochMilliseconds(0),
            updatedAt = kotlin.time.Instant.fromEpochMilliseconds(0),
            version = 1,
            results = emptyList()
        )
        coEvery { raceWeekendService.getRaceWeekend("race-1") } returns raceWeekend
        coEvery { raceWeekendService.getRaceWeekendResults("race-1") } returns raceWeekendResult
        coEvery { teamResultsService.calculateAndSaveLineupsResults(any()) } throws RuntimeException("DB error")

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/teams/results")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().is5xxServerError
    }
}
