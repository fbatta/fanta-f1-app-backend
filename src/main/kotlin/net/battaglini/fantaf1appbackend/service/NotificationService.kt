package net.battaglini.fantaf1appbackend.service

import com.google.cloud.firestore.DocumentSnapshot
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import net.battaglini.fantaf1appbackend.enums.UserNotificationType
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import net.battaglini.fantaf1appbackend.repository.LobbyRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

@Service
class NotificationService(
    private val firebaseMessaging: FirebaseMessaging,
    private val lobbyRepository: LobbyRepository,
    private val userService: UserService
) {
    @OptIn(ExperimentalAtomicApi::class)
    suspend fun processRaceWeekendCalculationCompletedNotification(raceWeekendResult: RaceWeekendResult): Int {
        var cursor: DocumentSnapshot? = null
        val notificationsSent = AtomicInt(0)

        do {
            val lobbies = lobbyRepository.getLobbies(cursor).toList()
            LOGGER.info("Retrieved {} lobbies", lobbies.size)
            if (lobbies.isEmpty()) {
                break
            }
            cursor = lobbies.last().first

            for (lobby in lobbies.map { it.second }) {
                val users = userService.getUsersByLobbyId(lobby.lobbyId)

                users.collect { user ->
                    if (user.deviceRegistrationTokens.isEmpty()) {
                        LOGGER.warn(
                            "User {} not found, or no device registration token found. Cannot send notification={}",
                            user.userId,
                            UserNotificationType.RACE_WEEKEND_RESULTS_AVAILABLE
                        )
                        return@collect
                    }
                    for (token in user.deviceRegistrationTokens) {
                        val message = Message.builder()
                            .setToken(token.key)
                            .setNotification(
                                Notification.builder().setTitle("${raceWeekendResult.raceName} results available")
                                    .setBody("Results for ${raceWeekendResult.raceName} are now available, click here to check them out!")
                                    .build()
                            )
                            .putData("type", UserNotificationType.RACE_WEEKEND_RESULTS_AVAILABLE.value)
                            .putData("raceId", raceWeekendResult.raceId)
                            .build()
                        try {
                            withContext(Dispatchers.IO) {
                                firebaseMessaging.sendAsync(message).get()
                            }
                            notificationsSent.incrementAndFetch()
                            LOGGER.info(
                                "Sent notification={} to userId={}",
                                UserNotificationType.RACE_WEEKEND_RESULTS_AVAILABLE,
                                user.userId
                            )
                        } catch (e: Exception) {
                            LOGGER.error(
                                "Error sending notification={} to userId={}",
                                UserNotificationType.RACE_WEEKEND_RESULTS_AVAILABLE,
                                user.userId,
                                e
                            )
                        }
                    }
                }
            }
        } while (lobbies.isNotEmpty())

        return notificationsSent.load()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(NotificationService::class.java)
    }
}