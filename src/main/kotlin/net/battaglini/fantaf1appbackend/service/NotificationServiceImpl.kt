package net.battaglini.fantaf1appbackend.service

import com.google.cloud.firestore.DocumentSnapshot
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                                sendNotificationsToUser(user, raceWeekendResult, notificationsSent)
                            }
                        }
                    }
                }
            }
        } while (lobbies.isNotEmpty())

        return notificationsSent.load()
    }

    @OptIn(ExperimentalAtomicApi::class)
    override suspend fun sendLineupOpenNotification(raceWeekend: RaceWeekend): Int {
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
                                sendLineupOpenToUser(user, raceWeekend, notificationsSent)
                            }
                        }
                    }
                }
            }
        } while (lobbies.isNotEmpty())

        return notificationsSent.load()
    }

    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun sendNotificationsToUser(
        user: net.battaglini.fantaf1appbackend.model.User,
        raceWeekendResult: RaceWeekendResult,
        notificationsSent: AtomicInt
    ) {
        if (user.deviceRegistrationTokens.isEmpty()) {
            LOGGER.warn(
                "User {} not found, or no device registration token found. Cannot send notification={}",
                user.userId,
                UserNotificationType.RACE_WEEKEND_RESULTS_AVAILABLE
            )
            return
        }

        coroutineScope {
            for (token in user.deviceRegistrationTokens) {
                launch {
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
    }

    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun sendLineupOpenToUser(
        user: net.battaglini.fantaf1appbackend.model.User,
        raceWeekend: RaceWeekend,
        notificationsSent: AtomicInt
    ) {
        if (user.deviceRegistrationTokens.isEmpty()) {
            LOGGER.warn(
                "User {} not found, or no device registration token found. Cannot send notification={}",
                user.userId,
                UserNotificationType.LINEUP_OPEN
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
                                .setTitle("Lineup for ${raceWeekend.raceName} is now OPEN!")
                                .setBody("The lineup for the ${raceWeekend.raceName} is now open. Don't forget to set your team before it closes!")
                                .build()
                        )
                        .putData("type", UserNotificationType.LINEUP_OPEN.value)
                        .putData("raceId", raceWeekend.raceId)
                        .build()
                    try {
                        withContext(Dispatchers.IO) {
                            firebaseMessaging.sendAsync(message).get()
                        }
                        notificationsSent.incrementAndFetch()
                        LOGGER.info(
                            "Sent notification={} to userId={}",
                            UserNotificationType.LINEUP_OPEN,
                            user.userId
                        )
                    } catch (e: Exception) {
                        LOGGER.error(
                            "Error sending notification={} to userId={}",
                            UserNotificationType.LINEUP_OPEN,
                            user.userId,
                            e
                        )
                    }
                }
            }
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(NotificationServiceImpl::class.java)
    }
}