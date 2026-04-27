package net.battaglini.fantaf1appbackend.task

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import net.battaglini.fantaf1appbackend.configuration.ChannelConfiguration
import net.battaglini.fantaf1appbackend.enums.TaskType
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import net.battaglini.fantaf1appbackend.service.DriverPricingService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import jakarta.annotation.PreDestroy

@Component
class PricingTask(
    private val taskChannel: Channel<ChannelConfiguration.Companion.TaskChannelMessage>,
    private val driverPricingService: DriverPricingService
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @EventListener(ApplicationStartedEvent::class)
    fun onStart() {
        scope.launch {
            LOGGER.info("PricingTask started, listening for results calculation completed events")
            for (message in taskChannel) {
                if (message.type == TaskType.RACE_WEEKEND_RESULTS_CALCULATION_COMPLETED) {
                    try {
                        val result = message.payload as RaceWeekendResult
                        LOGGER.info("Received results for {}, triggering price update", result.raceName)
                        driverPricingService.calculateAndUpdatePrices(result.raceId)
                    } catch (e: Exception) {
                        LOGGER.error("Error processing pricing update for message: $message", e)
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
        private val LOGGER = LoggerFactory.getLogger(PricingTask::class.java)
    }
}
