package net.battaglini.fantaf1appbackend.task

import com.google.cloud.firestore.DocumentSnapshot
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import net.battaglini.fantaf1appbackend.configuration.ChannelConfiguration
import net.battaglini.fantaf1appbackend.enums.UserNotificationType
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import net.battaglini.fantaf1appbackend.repository.LobbyRepository
import net.battaglini.fantaf1appbackend.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class NotificationsTask(
    private val userNotificationChannel: Channel<ChannelConfiguration.Companion.UserNotificationChannelMessage>,
    private val firebaseMessaging: FirebaseMessaging,
    private val lobbyRepository: LobbyRepository,
    private val userRepository: UserRepository
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
            UserNotificationType.RACE_WEEKEND_CALCULATION_COMPLETED -> processRaceWeekendCalculationCompletedNotification(
                notification.data as RaceWeekendResult
            )
        }
    }

    private suspend fun processRaceWeekendCalculationCompletedNotification(raceWeekendResult: RaceWeekendResult) {
        var cursor: DocumentSnapshot? = null

        do {
            val lobbies = lobbyRepository.getLobbies(cursor).toList()
            LOGGER.info("Retrieved {} lobbies", lobbies.size)
            cursor = lobbies.last().first

            for (lobby in lobbies.map { it.second }) {
                val ownerId = lobby.ownerId
                val userPair = userRepository.getUser(ownerId)

                if (userPair == null || userPair.second.deviceRegistrationTokens.isEmpty()) {
                    LOGGER.warn(
                        "User {} not found, or no device registration token found. Cannot send notification={}",
                        ownerId,
                        UserNotificationType.RACE_WEEKEND_CALCULATION_COMPLETED
                    )
                    continue
                }

                val user = userPair.second
                for (token in user.deviceRegistrationTokens) {
                    val message = Message.builder()
                        .setToken(token.value)
                        .putData("notificationType", UserNotificationType.RACE_WEEKEND_CALCULATION_COMPLETED.name)
                        .putData("lobbyId", lobby.lobbyId)
                        .putData("lobbyName", lobby.lobbyName)
                        .build()

                    try {
                        withContext(Dispatchers.IO) {
                            firebaseMessaging.sendAsync(message).get()
                        }
                        LOGGER.info(
                            "Sent notification={} to userId={}",
                            UserNotificationType.RACE_WEEKEND_CALCULATION_COMPLETED,
                            user.userId
                        )
                    } catch (e: Exception) {
                        LOGGER.error(
                            "Error sending notification={} to userId={}",
                            UserNotificationType.RACE_WEEKEND_CALCULATION_COMPLETED,
                            user.userId,
                            e
                        )
                    }
                }
            }
        } while (lobbies.isNotEmpty())
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(NotificationsTask::class.java)
    }
}