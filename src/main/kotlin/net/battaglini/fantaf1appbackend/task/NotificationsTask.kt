package net.battaglini.fantaf1appbackend.task

import kotlinx.coroutines.channels.Channel
import net.battaglini.fantaf1appbackend.configuration.ChannelConfiguration
import net.battaglini.fantaf1appbackend.enums.UserNotificationType
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import net.battaglini.fantaf1appbackend.service.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class NotificationsTask(
    private val userNotificationChannel: Channel<ChannelConfiguration.Companion.UserNotificationChannelMessage>,
    private val notificationService: NotificationService
) {
    @Scheduled(fixedRate = 1000)
    suspend fun checkNotificationsChannel() {
        LOGGER.debug("Checking for user notifications to send")
        val notification = userNotificationChannel.tryReceive().getOrNull()

        if (notification == null) {
            LOGGER.debug("No new notifications to process")
            return
        }

        when (notification.notificationType) {
            UserNotificationType.RACE_WEEKEND_RESULTS_AVAILABLE -> notificationService.processRaceWeekendCalculationCompletedNotification(
                notification.data as RaceWeekendResult
            )

            else -> {
                LOGGER.error("Received a request for ${notification.notificationType}, which is not supported")
            }
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(NotificationsTask::class.java)
    }
}