package net.battaglini.fantaf1appbackend.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "notifications")
data class NotificationProperties(
    val lineup: LineupNotificationProperties = LineupNotificationProperties()
) {
    data class LineupNotificationProperties(
        val closeReminderTimeBefore: Duration = Duration.ofHours(12)
    )
}
