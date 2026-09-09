package net.battaglini.fantaf1appbackend.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import net.battaglini.fantaf1appbackend.enums.UserNotificationType
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import net.battaglini.fantaf1appbackend.model.request.SendNotificationRequest
import net.battaglini.fantaf1appbackend.repository.RaceWeekendResultRepository
import net.battaglini.fantaf1appbackend.service.NotificationService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.http.codec.autoconfigure.CodecsAutoConfiguration
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf
import org.springframework.test.web.reactive.server.WebTestClient

import org.springframework.context.annotation.Import
import net.battaglini.fantaf1appbackend.configuration.WebfluxSecurityConfiguration

@WebFluxTest(
    controllers = [NotificationsController::class],
    excludeAutoConfiguration = [CodecsAutoConfiguration::class]
)
@Import(WebfluxSecurityConfiguration::class)
class NotificationsControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var notificationService: NotificationService

    @MockkBean
    private lateinit var raceWeekendResultRepository: RaceWeekendResultRepository

    private fun createRaceWeekendResult(raceId: String = "race-1", raceName: String = "Monaco Grand Prix"): RaceWeekendResult {
        return RaceWeekendResult(
            raceId = raceId,
            raceName = raceName,
            openF1MeetingKey = 1234,
            createdAt = kotlin.time.Clock.System.now(),
            updatedAt = kotlin.time.Clock.System.now(),
            version = 1,
            results = emptyList()
        )
    }

    @Test
    @WithMockUser
    fun `sendRaceWeekendResultsAvailableNotification should return 200 OK with count when results found`() {
        val raceWeekendResult = createRaceWeekendResult()
        val request = SendNotificationRequest(
            type = UserNotificationType.RACE_WEEKEND_RESULTS_AVAILABLE,
            raceId = "race-1"
        )

        coEvery { raceWeekendResultRepository.findRaceWeekendResult(raceId = "race-1") } returns raceWeekendResult
        coEvery { notificationService.processRaceWeekendCalculationCompletedNotification(raceWeekendResult) } returns 5

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/notifications/race-weekend-results-available/send")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .isEqualTo("Sent 5 notifications")

        coVerify { raceWeekendResultRepository.findRaceWeekendResult(raceId = "race-1") }
        coVerify { notificationService.processRaceWeekendCalculationCompletedNotification(raceWeekendResult) }
    }

    @Test
    @WithMockUser
    fun `sendRaceWeekendResultsAvailableNotification should return 404 Not Found when results not found`() {
        val request = SendNotificationRequest(
            type = UserNotificationType.RACE_WEEKEND_RESULTS_AVAILABLE,
            raceId = "nonexistent-race"
        )

        coEvery { raceWeekendResultRepository.findRaceWeekendResult(raceId = "nonexistent-race") } returns null

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/notifications/race-weekend-results-available/send")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isNotFound

        coVerify { raceWeekendResultRepository.findRaceWeekendResult(raceId = "nonexistent-race") }
        coVerify(exactly = 0) { notificationService.processRaceWeekendCalculationCompletedNotification(any()) }
    }

    @Test
    fun `sendRaceWeekendResultsAvailableNotification should return 401 Unauthorized when unauthenticated`() {
        val request = SendNotificationRequest(
            type = UserNotificationType.RACE_WEEKEND_RESULTS_AVAILABLE,
            raceId = "race-1"
        )

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/notifications/race-weekend-results-available/send")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isUnauthorized
    }
}
