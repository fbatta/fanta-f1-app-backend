package net.battaglini.fantaf1appbackend.configuration

import kotlinx.coroutines.channels.Channel
import net.battaglini.fantaf1appbackend.enums.TaskType
import net.battaglini.fantaf1appbackend.enums.UserNotificationType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ChannelConfiguration {
    // TODO: choose a better limit for the queue length
    @Bean("userNotificationChannel")
    fun userNotificationChannel() = Channel<UserNotificationChannelMessage>(Channel.UNLIMITED)

    // TODO: choose a better limit for the queue length
    @Bean("taskChannel")
    fun defaultChannel() = Channel<TaskChannelMessage>(Channel.UNLIMITED)

    companion object {
        data class UserNotificationChannelMessage(
            val notificationType: UserNotificationType,
            val data: Any
        )

        data class TaskChannelMessage(
            val taskType: TaskType,
            val data: Any
        )
    }
}