package net.battaglini.fantaf1appbackend.service

import com.google.cloud.firestore.DocumentSnapshot
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import net.battaglini.fantaf1appbackend.enums.UserNotificationType
import net.battaglini.fantaf1appbackend.model.RaceWeekend
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import net.battaglini.fantaf1appbackend.repository.LobbyRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

@Service
class NotificationServiceImpl(
    private val firebaseMessaging: FirebaseMessaging,
    private val lobbyRepository: LobbyRepository,
    private val userService: UserService
) : NotificationService {
    @OptIn(ExperimentalAtomicApi::class)
    override suspend fun processRaceWeekendCalculationCompletedNotification(raceWeekendResult: RaceWeekendResult): Int {
        return broadcastNotification {
            NotificationContent(
                title = "${raceWeekendResult.raceName} results available",
                body = "Results for ${raceWeekendResult.raceName} are now available, click here to check them out!",
                type = UserNotificationType.RACE_WEEKEND_RESULTS_AVAILABLE,
                data = mapOf("raceId" to raceWeekendResult.raceId)
            )
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    override suspend fun sendLineupOpenNotification(raceWeekend: RaceWeekend): Int {
        return broadcastNotification {
            NotificationContent(
                title = "Lineup for ${raceWeekend.raceName} is now OPEN!",
                body = "The lineup for the ${raceWeekend.raceName} is now open. Don't forget to set your team before it closes!",
                type = UserNotificationType.LINEUP_OPEN,
                data = mapOf("raceId" to raceWeekend.raceId)
            )
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    override suspend fun sendLineupCloseReminderNotification(raceWeekend: RaceWeekend, hoursBefore: Long): Int {
        return broadcastNotification {
            NotificationContent(
                title = "Lineup for ${raceWeekend.raceName} is closing soon!",
                body = "Lineup for ${raceWeekend.raceName} closes in $hoursBefore hours! Don't forget to set your team!",
                type = UserNotificationType.LINEUP_CLOSE_REMINDER,
                data = mapOf("raceId" to raceWeekend.raceId)
            )
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    override suspend fun sendLineupClosedNotification(raceWeekend: RaceWeekend): Int {
        return broadcastNotification {
            NotificationContent(
                title = "Lineup for ${raceWeekend.raceName} is now CLOSED!",
                body = "The lineup for the ${raceWeekend.raceName} is now closed. Good luck to your team!",
                type = UserNotificationType.LINEUP_CLOSED,
                data = mapOf("raceId" to raceWeekend.raceId)
            )
        }
    }

    @OptIn(ExperimentalAtomicApi::class, ExperimentalCoroutinesApi::class)
    private suspend fun broadcastNotification(
        createContent: (net.battaglini.fantaf1appbackend.model.User) -> NotificationContent
    ): Int {
        var cursor: DocumentSnapshot? = null
        val notificationsSent = AtomicInt(0)

        do {
            val lobbies = lobbyRepository.getLobbies(cursor, null).toList()
            LOGGER.info("Retrieved {} lobbies", lobbies.size)
            if (lobbies.isEmpty()) {
                break
            }
            cursor = lobbies.last().first

            coroutineScope {
                for (lobby in lobbies.map { it.second }) {
                    launch {
                        userService.getUsersByLobbyId(lobby.lobbyId).collect { user ->
                            launch {
                                val content = createContent(user)
                                sendToUserTokens(user, content, notificationsSent)
                            }
                        }
                    }
                }
            }
        } while (lobbies.isNotEmpty())

        return notificationsSent.load()
    }

    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun sendToUserTokens(
        user: net.battaglini.fantaf1appbackend.model.User,
        content: NotificationContent,
        notificationsSent: AtomicInt,
    ) {
        if (user.deviceRegistrationTokens.isEmpty()) {
            LOGGER.warn(
                "User {} not found, or no device registration token found. Cannot send notification={}",
                user.userId,
                content.type
            )
            return
        }

        coroutineScope {
            for (token in user.deviceRegistrationTokens) {
                launch {
                    val message = Message.builder()
                        .setToken(token.key)
                        .setNotification(
                            Notification.builder()
                                .setTitle(content.title)
                                .setBody(content.body)
                                .build()
                        )
                        .putData("type", content.type.value)
                        .putAllData(content.data)
                        .build()
                    try {
                        withContext(Dispatchers.IO) {
                            firebaseMessaging.sendAsync(message).get()
                        }
                        notificationsSent.incrementAndFetch()
                        LOGGER.info(
                            "Sent notification={} to userId={}",
                            content.type,
                            user.userId
                        )
                    } catch (e: Exception) {
                        LOGGER.error(
                            "Error sending notification={} to userId={}",
                            content.type,
                            user.userId,
                            e
                        )
                    }
                }
            }
        }
    }

    private data class NotificationContent(
        val title: String,
        val body: String,
        val type: UserNotificationType,
        val data: Map<String, String> = emptyMap()
    )

    companion object {
        private val LOGGER = LoggerFactory.getLogger(NotificationServiceImpl::class.java)
    }
}
