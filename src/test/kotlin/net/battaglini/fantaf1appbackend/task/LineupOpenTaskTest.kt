package net.battaglini.fantaf1appbackend.task

import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.battaglini.fantaf1appbackend.model.RaceWeekend
import net.battaglini.fantaf1appbackend.repository.LineupNotificationRepository
import net.battaglini.fantaf1appbackend.repository.RaceRepository
import net.battaglini.fantaf1appbackend.service.NotificationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class LineupOpenTaskTest {

    @MockK
    lateinit var raceRepository: RaceRepository

    @MockK
    lateinit var notificationRepository: LineupNotificationRepository

    @MockK
    lateinit var notificationService: NotificationService

    @MockK
    lateinit var clock: Clock

    @InjectMockKs
    lateinit var task: LineupOpenTask

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    private fun createRaceWeekend(
        raceId: String,
        dateLineupOpen: Instant,
        dateLineupClose: Instant
    ) = RaceWeekend(
        raceId = raceId,
        openF1MeetingKey = 1,
        raceName = "Race $raceId",
        dateStart = dateLineupOpen,
        dateEnd = dateLineupClose,
        sessions = emptyList(),
        circuitImage = "url",
        countryName = "Italy",
        countryFlag = "IT",
        circuitType = "street",
        dateLineupOpen = dateLineupOpen,
        dateLineupClose = dateLineupClose
    )

    @Test
    fun `checkLineupOpen should send notification when within window and not sent`() = runTest {
        val now = Instant.parse("2024-03-22T12:00:00Z")
        val dateOpen = Instant.parse("2024-03-22T10:00:00Z")
        val dateClose = Instant.parse("2024-03-22T14:00:00Z")
        val race = createRaceWeekend("race1", dateOpen, dateClose)

        every { clock.now() } returns now
        coEvery { raceRepository.findNextRace(any()) } returns race
        coEvery { notificationRepository.isLineupOpenNotificationSent("race1") } returns false
        coEvery { notificationService.sendLineupOpenNotification(any()) } returns 1
        coEvery { notificationRepository.markLineupOpenNotificationAsSent("race1") } just Runs

        task.checkLineupOpen()

        coVerify { notificationService.sendLineupOpenNotification(race) }
        coVerify { notificationRepository.markLineupOpenNotificationAsSent("race1") }
    }

    @Test
    fun `checkLineupOpen should not send notification when already sent`() = runTest {
        val now = Instant.parse("2024-03-22T12:00:00Z")
        val dateOpen = Instant.parse("2024-03-22T10:00:00Z")
        val dateClose = Instant.parse("2024-03-22T14:00:00Z")
        val race = createRaceWeekend("race1", dateOpen, dateClose)

        every { clock.now() } returns now
        coEvery { raceRepository.findNextRace(any()) } returns race
        coEvery { notificationRepository.isLineupOpenNotificationSent("race1") } returns true

        task.checkLineupOpen()

        coVerify(exactly = 0) { notificationService.sendLineupOpenNotification(any()) }
    }

    @Test
    fun `checkLineupOpen should not send notification when outside window`() = runTest {
        val now = Instant.parse("2024-03-22T08:00:00Z")
        val dateOpen = Instant.parse("2024-03-22T10:00:00Z")
        val dateClose = Instant.parse("2024-03-22T14:00:00Z")
        val race = createRaceWeekend("race1", dateOpen, dateClose)

        every { clock.now() } returns now
        coEvery { raceRepository.findNextRace(any()) } returns race

        task.checkLineupOpen()

        coVerify(exactly = 0) { notificationService.sendLineupOpenNotification(any()) }
    }

    @Test
    fun `checkLineupOpen should handle no next race`() = runTest {
        val now = Instant.parse("2024-03-22T12:00:00Z")
        every { clock.now() } returns now
        coEvery { raceRepository.findNextRace(any()) } returns null

        task.checkLineupOpen()

        coVerify(exactly = 0) { notificationRepository.isLineupOpenNotificationSent(any()) }
    }
}
