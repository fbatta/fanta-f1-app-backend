package net.battaglini.fantaf1appbackend.task

import net.battaglini.fantaf1appbackend.repository.LineupNotificationRepository
import net.battaglini.fantaf1appbackend.repository.RaceRepository
import net.battaglini.fantaf1appbackend.service.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import kotlin.time.Clock

@Component
class LineupOpenTask(
    private val raceRepository: RaceRepository,
    private val notificationRepository: LineupNotificationRepository,
    private val notificationService: NotificationService,
    private val clock: Clock
) {
    @Scheduled(fixedRate = 600000)
    suspend fun checkLineupOpen() {
        val now = clock.now()
        val nextRace = raceRepository.findNextRace(now)

        if (nextRace == null) {
            LOGGER.debug("No next race found")
            return
        }

        if (now >= nextRace.dateLineupOpen && now < nextRace.dateLineupClose) {
            val alreadySent = notificationRepository.isLineupOpenNotificationSent(nextRace.raceId)
            if (!alreadySent) {
                LOGGER.info("Sending lineup open notification for race: {} ({})", nextRace.raceName, nextRace.raceId)
                notificationService.sendLineupOpenNotification(nextRace)
                notificationRepository.markLineupOpenNotificationAsSent(nextRace.raceId)
            } else {
                LOGGER.debug("Lineup open notification already sent for race: {}", nextRace.raceName)
            }
        } else {
            LOGGER.debug("Not within lineup window for race: {}. Current: {}, Open: {}, Close: {}", 
                nextRace.raceName, now, nextRace.dateLineupOpen, nextRace.dateLineupClose)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(LineupOpenTask::class.java)
    }
}
