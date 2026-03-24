package net.battaglini.fantaf1appbackend.controller

import net.battaglini.fantaf1appbackend.exception.InvalidRequestException
import net.battaglini.fantaf1appbackend.exception.NotFoundException
import net.battaglini.fantaf1appbackend.model.request.SendNotificationRequest
import net.battaglini.fantaf1appbackend.repository.RaceWeekendResultRepository
import net.battaglini.fantaf1appbackend.service.NotificationService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/notifications"])
class NotificationsController(
    private val notificationService: NotificationService,
    private val raceWeekendResultRepository: RaceWeekendResultRepository
) {
    @PostMapping(path = ["/race-weekend-results-available/send"])
    suspend fun sendRaceWeekendResultsAvailableNotification(@RequestBody body: SendNotificationRequest): String {
        if (body.raceId == null) {
            throw InvalidRequestException("raceId is required")
        }

        val result = raceWeekendResultRepository.findRaceWeekendResult(body.raceId)
            ?: throw NotFoundException("Race weekend results for ${body.raceId} not found")

        val notificationsSent = notificationService.processRaceWeekendCalculationCompletedNotification(result)

        return "Sent $notificationsSent notifications"
    }
}