package net.battaglini.fantaf1appbackend.task

import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.battaglini.fantaf1appbackend.configuration.NotificationProperties
import net.battaglini.fantaf1appbackend.model.RaceWeekend
import net.battaglini.fantaf1appbackend.repository.LineupNotificationRepository
import net.battaglini.fantaf1appbackend.repository.RaceRepository
import net.battaglini.fantaf1appbackend.service.NotificationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@ExtendWith(MockKExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
class LineupNotificationTaskTest {

    @MockK
    lateinit var raceRepository: RaceRepository

    @MockK
    lateinit var notificationRepository: LineupNotificationRepository

    @MockK
    lateinit var notificationService: NotificationService

    @MockK
    lateinit var clock: Clock

    private val notificationProperties = NotificationProperties(
        lineup = NotificationProperties.LineupNotificationProperties(
            closeReminderTimeBefore = java.time.Duration.ofHours(12)
        )
    )

    @InjectMockKs
    lateinit var lineupNotificationTask: LineupNotificationTask

    private val now = Instant.parse("2024-03-01T10:00:00Z")

    private val raceWeekend = RaceWeekend(
        raceId = "2024-bahrain",
        openF1MeetingKey = 1,
        raceName = "Bahrain Grand Prix",
        dateStart = Instant.parse("2024-03-01T00:00:00Z"),
        dateEnd = Instant.parse("2024-03-03T00:00:00Z"),
        sessions = emptyList(),
        circuitImage = "url",
        countryName = "Bahrain",
        countryFlag = "flag",
        circuitType = "Permanent",
        dateLineupOpen = Instant.parse("2024-02-29T00:00:00Z"),
        dateLineupClose = Instant.parse("2024-03-02T00:00:00Z")
    )

    @BeforeEach
    fun setUp() {
        every { clock.now() } returns now
    }

    @Test
    fun `checkLineupNotifications sends open notification when within window and not already sent`() = runTest {
        coEvery { raceRepository.findNextRace(now) } returns raceWeekend
        coEvery { notificationRepository.isLineupOpenNotificationSent(raceWeekend.raceId) } returns false
        coEvery { notificationRepository.isLineupCloseReminderSent(raceWeekend.raceId) } returns false
        coEvery { notificationService.sendLineupOpenNotification(raceWeekend) } returns 1
        coEvery { notificationRepository.markLineupOpenNotificationAsSent(raceWeekend.raceId) } just Runs

        lineupNotificationTask.checkLineupNotifications()

        coVerify(exactly = 1) { notificationService.sendLineupOpenNotification(raceWeekend) }
        coVerify(exactly = 1) { notificationRepository.markLineupOpenNotificationAsSent(raceWeekend.raceId) }
    }

    @Test
    fun `checkLineupNotifications sends reminder notification when within window and not already sent`() = runTest {
        val reminderNow = raceWeekend.dateLineupClose - 6.hours
        every { clock.now() } returns reminderNow
        coEvery { raceRepository.findNextRace(reminderNow) } returns raceWeekend
        coEvery { notificationRepository.isLineupOpenNotificationSent(raceWeekend.raceId) } returns true
        coEvery { notificationRepository.isLineupCloseReminderSent(raceWeekend.raceId) } returns false
        coEvery { notificationService.sendLineupCloseReminderNotification(raceWeekend, 12) } returns 1
        coEvery { notificationRepository.markLineupCloseReminderAsSent(raceWeekend.raceId) } just Runs

        lineupNotificationTask.checkLineupNotifications()

        coVerify(exactly = 1) { notificationService.sendLineupCloseReminderNotification(raceWeekend, 12) }
        coVerify(exactly = 1) { notificationRepository.markLineupCloseReminderAsSent(raceWeekend.raceId) }
    }

    @Test
    fun `checkLineupNotifications sends closed notification when within window and not already sent`() = runTest {
        val closedNow = raceWeekend.dateLineupClose + 1.hours
        every { clock.now() } returns closedNow
        coEvery { raceRepository.findNextRace(closedNow) } returns raceWeekend
        coEvery { notificationRepository.isLineupOpenNotificationSent(raceWeekend.raceId) } returns true
        coEvery { notificationRepository.isLineupCloseReminderSent(raceWeekend.raceId) } returns true
        coEvery { notificationRepository.isLineupClosedNotificationSent(raceWeekend.raceId) } returns false
        coEvery { notificationService.sendLineupClosedNotification(raceWeekend) } returns 1
        coEvery { notificationRepository.markLineupClosedNotificationAsSent(raceWeekend.raceId) } just Runs

        lineupNotificationTask.checkLineupNotifications()

        coVerify(exactly = 1) { notificationService.sendLineupClosedNotification(raceWeekend) }
        coVerify(exactly = 1) { notificationRepository.markLineupClosedNotificationAsSent(raceWeekend.raceId) }
    }

    @Test
    fun `checkLineupNotifications does not send open notification when within window but already sent`() = runTest {
        coEvery { raceRepository.findNextRace(now) } returns raceWeekend
        coEvery { notificationRepository.isLineupOpenNotificationSent(raceWeekend.raceId) } returns true
        coEvery { notificationRepository.isLineupCloseReminderSent(raceWeekend.raceId) } returns false

        lineupNotificationTask.checkLineupNotifications()

        coVerify(exactly = 0) { notificationService.sendLineupOpenNotification(any()) }
        coVerify(exactly = 0) { notificationRepository.markLineupOpenNotificationAsSent(any()) }
    }

    @Test
    fun `checkLineupNotifications does not send reminder notification when within window but already sent`() = runTest {
        val reminderNow = raceWeekend.dateLineupClose - 6.hours
        every { clock.now() } returns reminderNow
        coEvery { raceRepository.findNextRace(reminderNow) } returns raceWeekend
        coEvery { notificationRepository.isLineupOpenNotificationSent(raceWeekend.raceId) } returns true
        coEvery { notificationRepository.isLineupCloseReminderSent(raceWeekend.raceId) } returns true

        lineupNotificationTask.checkLineupNotifications()

        coVerify(exactly = 0) { notificationService.sendLineupCloseReminderNotification(any(), any()) }
        coVerify(exactly = 0) { notificationRepository.markLineupCloseReminderAsSent(any()) }
    }

    @Test
    fun `checkLineupNotifications does not send notification when too early`() = runTest {
        val earlyNow = raceWeekend.dateLineupOpen - 1.hours
        every { clock.now() } returns earlyNow
        coEvery { raceRepository.findNextRace(earlyNow) } returns raceWeekend

        lineupNotificationTask.checkLineupNotifications()

        coVerify(exactly = 0) { notificationService.sendLineupOpenNotification(any()) }
        coVerify(exactly = 0) { notificationService.sendLineupCloseReminderNotification(any(), any()) }
    }

    @Test
    fun `checkLineupNotifications does not send notification when too late`() = runTest {
        val lateNow = raceWeekend.dateLineupClose + 1.hours
        every { clock.now() } returns lateNow
        coEvery { raceRepository.findNextRace(lateNow) } returns raceWeekend

        lineupNotificationTask.checkLineupNotifications()

        coVerify(exactly = 0) { notificationService.sendLineupOpenNotification(any()) }
        coVerify(exactly = 0) { notificationService.sendLineupCloseReminderNotification(any(), any()) }
    }

    @Test
    fun `checkLineupNotifications handles no next race gracefully`() = runTest {
        coEvery { raceRepository.findNextRace(now) } returns null

        lineupNotificationTask.checkLineupNotifications()

        coVerify(exactly = 0) { notificationService.sendLineupOpenNotification(any()) }
        coVerify(exactly = 0) { notificationService.sendLineupCloseReminderNotification(any(), any()) }
    }
}
