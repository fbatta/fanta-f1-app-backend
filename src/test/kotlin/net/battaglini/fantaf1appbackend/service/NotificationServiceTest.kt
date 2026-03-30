package net.battaglini.fantaf1appbackend.service

import com.google.api.core.ApiFuture
import com.google.cloud.firestore.DocumentSnapshot
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.battaglini.fantaf1appbackend.model.Lobby
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import net.battaglini.fantaf1appbackend.model.User
import net.battaglini.fantaf1appbackend.repository.LobbyRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.ExecutionException
import kotlin.time.Clock

@ExtendWith(MockKExtension::class)
class NotificationServiceTest {

    @MockK
    lateinit var firebaseMessaging: FirebaseMessaging

    @MockK
    lateinit var lobbyRepository: LobbyRepository

    @MockK
    lateinit var userService: UserService

    @InjectMockKs
    lateinit var notificationService: NotificationService

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    private fun createLobby(id: String) = Lobby(
        lobbyId = id,
        lobbyName = "Lobby $id",
        lobbyPassword = "password",
        ownerId = "ownerId",
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )

    private fun createUser(id: String, tokens: Map<String, String>) = User(
        userId = id,
        deviceRegistrationTokens = tokens
    )

    private fun createRaceWeekendResult() = RaceWeekendResult(
        raceId = "race1",
        raceName = "Test Race",
        openF1MeetingKey = 1,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
        version = 1,
        results = emptyList(),
        summaryParagraphs = null
    )

    @Test
    fun `processRaceWeekendCalculationCompletedNotification should send notifications to all users in lobbies`() =
        runTest {
            val raceResult = createRaceWeekendResult()
            val lobby1 = createLobby("lobby1")
            val lobby2 = createLobby("lobby2")
            val mockSnapshot1 = mockk<DocumentSnapshot>()
            val mockSnapshot2 = mockk<DocumentSnapshot>()

            // Mock lobby repository to return 2 lobbies in first page, then empty in second page
            coEvery { lobbyRepository.getLobbies(null, any()) } returns flowOf(
                Pair(mockSnapshot1, lobby1),
                Pair(mockSnapshot2, lobby2)
            )
            coEvery { lobbyRepository.getLobbies(mockSnapshot2, any()) } returns emptyFlow()

            val user1 = createUser("user1", mapOf("token1" to "token1_val"))
            val user2 = createUser("user2", mapOf("token2" to "token2_val", "token3" to "token3_val"))
            val user3 = createUser("user3", emptyMap()) // No tokens

            coEvery { userService.getUsersByLobbyId("lobby1") } returns flowOf(user1, user2)
            coEvery { userService.getUsersByLobbyId("lobby2") } returns flowOf(user3)

            val mockFuture = mockk<ApiFuture<String>>()
            every { mockFuture.get() } returns "message_id"
            every { firebaseMessaging.sendAsync(any<Message>()) } returns mockFuture

            val sentCount = notificationService.processRaceWeekendCalculationCompletedNotification(raceResult)

            // 1 token for user1, 2 tokens for user2 -> 3 notifications sent
            assertEquals(3, sentCount)

            verify(exactly = 3) { firebaseMessaging.sendAsync(any<Message>()) }
        }

    @Test
    fun `processRaceWeekendCalculationCompletedNotification should handle exceptions during sendAsync gracefully`() =
        runTest {
            val raceResult = createRaceWeekendResult()
            val lobby = createLobby("lobby1")
            val mockSnapshot = mockk<DocumentSnapshot>()

            coEvery { lobbyRepository.getLobbies(null, any()) } returns flowOf(Pair(mockSnapshot, lobby))
            coEvery { lobbyRepository.getLobbies(mockSnapshot, any()) } returns emptyFlow()

            val user = createUser("user1", mapOf("token1" to "token1_val", "token2" to "token2_val"))
            coEvery { userService.getUsersByLobbyId("lobby1") } returns flowOf(user)

            val successFuture = mockk<ApiFuture<String>>()
            every { successFuture.get() } returns "message_id"

            val failureFuture = mockk<ApiFuture<String>>()
            every { failureFuture.get() } throws ExecutionException(RuntimeException("FCM error"))

            // First message fails, second succeeds
            every { firebaseMessaging.sendAsync(any<Message>()) } returns failureFuture andThen successFuture

            val sentCount = notificationService.processRaceWeekendCalculationCompletedNotification(raceResult)

            // 1 notification successfully sent despite the error
            assertEquals(1, sentCount)

            verify(exactly = 2) { firebaseMessaging.sendAsync(any<Message>()) }
        }

    @Test
    fun `processRaceWeekendCalculationCompletedNotification should skip users without tokens`() = runTest {
        val raceResult = createRaceWeekendResult()
        val lobby = createLobby("lobby1")
        val mockSnapshot = mockk<DocumentSnapshot>()

        coEvery { lobbyRepository.getLobbies(null, any()) } returns flowOf(Pair(mockSnapshot, lobby))
        coEvery { lobbyRepository.getLobbies(mockSnapshot, any()) } returns emptyFlow()

        val user = createUser("user1", emptyMap())
        coEvery { userService.getUsersByLobbyId("lobby1") } returns flowOf(user)

        val sentCount = notificationService.processRaceWeekendCalculationCompletedNotification(raceResult)

        assertEquals(0, sentCount)
        verify(exactly = 0) { firebaseMessaging.sendAsync(any<Message>()) }
    }

    @Test
    fun `processRaceWeekendCalculationCompletedNotification should do nothing if no lobbies exist`() = runTest {
        val raceResult = createRaceWeekendResult()

        coEvery { lobbyRepository.getLobbies(null, any()) } returns emptyFlow()

        val sentCount = notificationService.processRaceWeekendCalculationCompletedNotification(raceResult)

        assertEquals(0, sentCount)
        verify(exactly = 0) { firebaseMessaging.sendAsync(any<Message>()) }
    }
}