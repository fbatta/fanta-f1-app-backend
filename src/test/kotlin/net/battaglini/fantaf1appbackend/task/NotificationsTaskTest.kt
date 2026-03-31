package net.battaglini.fantaf1appbackend.task

import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.test.runTest
import net.battaglini.fantaf1appbackend.configuration.ChannelConfiguration.Companion.UserNotificationChannelMessage
import net.battaglini.fantaf1appbackend.enums.UserNotificationType
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import net.battaglini.fantaf1appbackend.service.DriverService
import net.battaglini.fantaf1appbackend.service.NotificationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.time.Clock

@ExtendWith(MockKExtension::class)
@OptIn(ExperimentalCoroutinesApi::class, InternalCoroutinesApi::class)
class NotificationsTaskTest {

    @MockK
    lateinit var userNotificationChannel: Channel<UserNotificationChannelMessage>

    @MockK(relaxed = true)
    lateinit var notificationService: NotificationService

    @MockK(relaxed = true)
    lateinit var driverService: DriverService

    @InjectMockKs
    lateinit var notificationsTask: NotificationsTask

    private val raceWeekendResult: RaceWeekendResult by lazy {
        RaceWeekendResult(
            raceId = "race1",
            raceName = "Test Race",
            openF1MeetingKey = 1,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            version = 1,
            results = emptyList(),
            summaryParagraphs = null
        )
    }

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `checkNotificationsChannel processes race weekend results notification`() = runTest {
        val notification = UserNotificationChannelMessage(
            notificationType = UserNotificationType.RACE_WEEKEND_RESULTS_AVAILABLE,
            data = raceWeekendResult
        )

        coEvery { driverService.updateAllDriversSummaries() } returns Unit
        coEvery { notificationService.processRaceWeekendCalculationCompletedNotification(any()) } returns 1

        every { userNotificationChannel.tryReceive() } coAnswers { ChannelResult.success(notification) }

        notificationsTask.checkNotificationsChannel()

        coVerify(exactly = 1) { driverService.updateAllDriversSummaries() }
        coVerify(exactly = 1) {
            notificationService.processRaceWeekendCalculationCompletedNotification(raceWeekendResult)
        }
    }

    @Test
    fun `checkNotificationsChannel skips unsupported notification types`() = runTest {
        val unsupportedNotification = UserNotificationChannelMessage(
            notificationType = UserNotificationType.UNKNOWN,
            data = "Unsupported notification"
        )

        every { userNotificationChannel.tryReceive() } coAnswers { ChannelResult.success(unsupportedNotification) }

        notificationsTask.checkNotificationsChannel()

        coVerify(exactly = 0) { driverService.updateAllDriversSummaries() }
        coVerify(exactly = 0) { notificationService.processRaceWeekendCalculationCompletedNotification(any()) }
    }

    @Test
    fun `checkNotificationsChannel handles null notification gracefully`() = runTest {
        every { userNotificationChannel.tryReceive() } coAnswers {
            ChannelResult.failure()
        }

        notificationsTask.checkNotificationsChannel()

        coVerify(exactly = 0) { driverService.updateAllDriversSummaries() }
        coVerify(exactly = 0) { notificationService.processRaceWeekendCalculationCompletedNotification(any()) }
    }

    @Test
    fun `checkNotificationsChannel handles exceptions in driver service update gracefully`() = runTest {
        val notification = UserNotificationChannelMessage(
            notificationType = UserNotificationType.RACE_WEEKEND_RESULTS_AVAILABLE,
            data = raceWeekendResult
        )

        every { userNotificationChannel.tryReceive() } coAnswers { ChannelResult.success(notification) }

        coEvery { driverService.updateAllDriversSummaries() } throws RuntimeException("Test exception")
        coEvery {
            notificationService.processRaceWeekendCalculationCompletedNotification(any())
        } returns 1

        notificationsTask.checkNotificationsChannel()

        coVerify(exactly = 1) { driverService.updateAllDriversSummaries() }
        coVerify(exactly = 1) {
            notificationService.processRaceWeekendCalculationCompletedNotification(raceWeekendResult)
        }
    }

    @Test
    fun `checkNotificationsChannel handles exceptions in notification service gracefully`() = runTest {
        val notification = UserNotificationChannelMessage(
            notificationType = UserNotificationType.RACE_WEEKEND_RESULTS_AVAILABLE,
            data = raceWeekendResult
        )

        every { userNotificationChannel.tryReceive() } coAnswers { ChannelResult.success(notification) }

        coEvery { driverService.updateAllDriversSummaries() } returns Unit
        coEvery {
            notificationService.processRaceWeekendCalculationCompletedNotification(any())
        } throws RuntimeException("Test exception")

        notificationsTask.checkNotificationsChannel()

        coVerify(exactly = 1) { driverService.updateAllDriversSummaries() }
        coVerify(exactly = 1) {
            notificationService.processRaceWeekendCalculationCompletedNotification(raceWeekendResult)
        }
    }

    @Test
    fun `checkNotificationsChannel executes in order driver service then notification service`() = runTest {
        val notification = UserNotificationChannelMessage(
            notificationType = UserNotificationType.RACE_WEEKEND_RESULTS_AVAILABLE,
            data = raceWeekendResult
        )
        every { userNotificationChannel.tryReceive() } coAnswers { ChannelResult.success(notification) }

        val executionOrder = mutableListOf<String>()

        coEvery { driverService.updateAllDriversSummaries() } coAnswers {
            executionOrder.add("driverService")
        }

        val originalProcessNotification =
            notificationService.processRaceWeekendCalculationCompletedNotification(raceWeekendResult)
        coEvery {
            notificationService.processRaceWeekendCalculationCompletedNotification(any())
        } coAnswers {
            executionOrder.add("notificationService")
            originalProcessNotification
        }

        notificationsTask.checkNotificationsChannel()

        verify(exactly = 1) { userNotificationChannel.tryReceive() }

        assertEquals(listOf("driverService", "notificationService"), executionOrder)
    }
}
