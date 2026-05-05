package net.battaglini.fantaf1appbackend.task

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import net.battaglini.fantaf1appbackend.configuration.ChannelConfiguration
import net.battaglini.fantaf1appbackend.configuration.ResultsCalculatorProperties
import net.battaglini.fantaf1appbackend.enums.TaskType
import net.battaglini.fantaf1appbackend.enums.UserNotificationType
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import net.battaglini.fantaf1appbackend.service.TeamResultsService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import javax.annotation.PreDestroy

@Component
class TeamsResultsCalculatorTask(
    private val resultsCalculatorProperties: ResultsCalculatorProperties,
    private val taskChannel: Channel<ChannelConfiguration.Companion.TaskChannelMessage>,
    private val userNotificationChannel: Channel<ChannelConfiguration.Companion.UserNotificationChannelMessage>,
    private val teamResultsService: TeamResultsService,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    @EventListener(ApplicationStartedEvent::class)
    internal suspend fun onStart() {
        scope.launch {
            if (!resultsCalculatorProperties.enable) {
                LOGGER.debug("Skipping checking raceWeekend results availability because it is disabled in app config")
            } else {
                LOGGER.debug("Checking raceWeekend results availability")

                for (message in taskChannel) {
                    if (message.taskType == TaskType.CALCULATE_LINEUP_RESULTS) {
                        val raceWeekendResult = message.data as RaceWeekendResult

                        LOGGER.info("Calculating teams results for raceId={}", raceWeekendResult.raceId)
                        try {
                            teamResultsService.calculateAndSaveLineupsResults(raceWeekendResult)
                            userNotificationChannel.send(
                                ChannelConfiguration.Companion.UserNotificationChannelMessage(
                                    UserNotificationType.RACE_WEEKEND_RESULTS_AVAILABLE,
                                    raceWeekendResult
                                )
                            )
                        } catch (ex: Exception) {
                            LOGGER.error("Error calculating teams results for raceId={}", raceWeekendResult.raceId, ex)
                        }
                    } else {
                        // re-send message so that it can be consumed by the appropriate service
                        taskChannel.send(message)
                    }
                }
            }
        }
    }

    @PreDestroy
    fun onDestroy() {
        scope.cancel()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TeamsResultsCalculatorTask::class.java)
    }
}
