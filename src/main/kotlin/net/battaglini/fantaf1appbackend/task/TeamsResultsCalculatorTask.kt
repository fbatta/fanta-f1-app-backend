package net.battaglini.fantaf1appbackend.task

import kotlinx.coroutines.channels.Channel
import net.battaglini.fantaf1appbackend.configuration.ChannelConfiguration
import net.battaglini.fantaf1appbackend.configuration.ResultsCalculatorProperties
import net.battaglini.fantaf1appbackend.enums.UserNotificationType
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import net.battaglini.fantaf1appbackend.service.TeamResultsService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class TeamsResultsCalculatorTask(
    private val resultsCalculatorProperties: ResultsCalculatorProperties,
    private val taskChannel: Channel<ChannelConfiguration.Companion.TaskChannelMessage>,
    private val userNotificationChannel: Channel<ChannelConfiguration.Companion.UserNotificationChannelMessage>,
    private val teamResultsService: TeamResultsService
) {
    @Scheduled(fixedRate = 1000)
    internal suspend fun runTask() {
        if (!resultsCalculatorProperties.enable) {
            LOGGER.debug("Skipping checking raceWeekend results availability because it is disabled in app config")
            return
        }
        LOGGER.debug("Checking raceWeekend results availability")

        val message = taskChannel.tryReceive().getOrNull()

        if (message == null) {
            LOGGER.debug("No raceWeekend results available")
            return
        }

        val raceWeekendResult = message.data as RaceWeekendResult

        LOGGER.info("Calculating teams results for raceId={}", raceWeekendResult.raceId)
        teamResultsService.calculateAndSaveLineupsResults(raceWeekendResult)
        userNotificationChannel.send(
            ChannelConfiguration.Companion.UserNotificationChannelMessage(
                UserNotificationType.RACE_WEEKEND_RESULTS_AVAILABLE,
                raceWeekendResult
            )
        )
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TeamsResultsCalculatorTask::class.java)
    }
}
