package net.battaglini.fantaf1appbackend.task

import net.battaglini.fantaf1appbackend.configuration.NotificationProperties
import net.battaglini.fantaf1appbackend.repository.LineupNotificationRepository
import net.battaglini.fantaf1appbackend.repository.RaceRepository
import net.battaglini.fantaf1appbackend.service.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import kotlin.time.Clock
import kotlin.time.toKotlinDuration

@Component
class LineupNotificationTask(
    private val raceRepository: RaceRepository,
    private val notificationRepository: LineupNotificationRepository,
    private val notificationService: NotificationService,
    private val notificationProperties: NotificationProperties,
    private val clock: Clock
) {
    @Scheduled(fixedRate = 600000)
    suspend fun checkLineupNotifications() {
        try {
            val now = clock.now()
            val nextRace = raceRepository.findNextRace(now)

            if (nextRace == null) {
                LOGGER.debug("No next race found")
                return
            }

            // Check for lineup open notification
            if (now >= nextRace.dateLineupOpen && now < nextRace.dateLineupClose) {
                val alreadySent = notificationRepository.isLineupOpenNotificationSent(nextRace.raceId)
                if (!alreadySent) {
                    LOGGER.info(
                        "Sending lineup open notification for race: {} ({})",
                        nextRace.raceName,
                        nextRace.raceId
                    )
                    notificationService.sendLineupOpenNotification(nextRace)
                    notificationRepository.markLineupOpenNotificationAsSent(nextRace.raceId)
                } else {
                    LOGGER.debug("Lineup open notification already sent for race: {}", nextRace.raceName)
                }
            }

            // Check for lineup close reminder notification
            val reminderDuration = notificationProperties.lineup.closeReminderTimeBefore.toKotlinDuration()
            val reminderStartTime = nextRace.dateLineupClose - reminderDuration
            if (now >= reminderStartTime && now < nextRace.dateLineupClose) {
                val alreadySent = notificationRepository.isLineupCloseReminderSent(nextRace.raceId)
                if (!alreadySent) {
                    LOGGER.info(
                        "Sending lineup close reminder notification for race: {} ({})",
                        nextRace.raceName,
                        nextRace.raceId
                    )
                    notificationService.sendLineupCloseReminderNotification(
                        nextRace,
                        reminderDuration.inWholeHours
                    )
                    notificationRepository.markLineupCloseReminderAsSent(nextRace.raceId)
                } else {
                    LOGGER.debug("Lineup close reminder notification already sent for race: {}", nextRace.raceName)
                }
            }

            // Check for lineup closed notification
            if (now >= nextRace.dateLineupClose && now < nextRace.dateEnd) {
                val alreadySent = notificationRepository.isLineupClosedNotificationSent(nextRace.raceId)
                if (!alreadySent) {
                    LOGGER.info(
                        "Sending lineup closed notification for race: {} ({})",
                        nextRace.raceName,
                        nextRace.raceId
                    )
                    notificationService.sendLineupClosedNotification(nextRace)
                    notificationRepository.markLineupClosedNotificationAsSent(nextRace.raceId)
                } else {
                    LOGGER.debug("Lineup closed notification already sent for race: {}", nextRace.raceName)
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error checking or sending lineup notifications", e)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(LineupNotificationTask::class.java)
    }
}
