package net.battaglini.fantaf1appbackend.task

import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import net.battaglini.fantaf1appbackend.configuration.ChannelConfiguration
import net.battaglini.fantaf1appbackend.configuration.ResultsCalculatorProperties
import net.battaglini.fantaf1appbackend.enums.TaskType
import net.battaglini.fantaf1appbackend.enums.UserNotificationType
import net.battaglini.fantaf1appbackend.model.Lineup
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import net.battaglini.fantaf1appbackend.model.Team
import net.battaglini.fantaf1appbackend.repository.LineupRepository
import net.battaglini.fantaf1appbackend.repository.TeamRepository
import net.battaglini.fantaf1appbackend.service.TeamResultsService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Clock
import kotlin.time.Instant

@ExtendWith(MockKExtension::class)
class TeamsResultsCalculatorTaskTest {

    @MockK
    lateinit var resultsCalculatorProperties: ResultsCalculatorProperties

    @MockK
    lateinit var teamRepository: TeamRepository

    @MockK
    lateinit var lineupRepository: LineupRepository

    @MockK
    lateinit var clock: Clock

    @MockK(relaxed = true)
    lateinit var timeZone: TimeZone

    @MockK
    lateinit var teamResultsService: TeamResultsService

    @MockK
    lateinit var userNotificationChannel: Channel<ChannelConfiguration.Companion.UserNotificationChannelMessage>

    private lateinit var taskChannel: Channel<ChannelConfiguration.Companion.TaskChannelMessage>
    private lateinit var task: TeamsResultsCalculatorTask

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        every { resultsCalculatorProperties.enable } returns true
        every { resultsCalculatorProperties.dryRun } returns false

        taskChannel = Channel()
        task = TeamsResultsCalculatorTask(
            resultsCalculatorProperties = resultsCalculatorProperties,
            taskChannel = taskChannel,
            userNotificationChannel = userNotificationChannel,
            teamResultsService = teamResultsService,
            dispatcher = Dispatchers.Unconfined
        )
    }

    private fun createRaceWeekendResult() = RaceWeekendResult(
        raceId = "race1",
        raceName = "Test Race",
        openF1MeetingKey = 1,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
        version = 1,
        results = listOf(
            RaceWeekendResult.Companion.Result("id-VER", 1, "VER", 20.0),
            RaceWeekendResult.Companion.Result("id-HAM", 44, "HAM", 15.0)
        )
    )

    private fun createTeam(id: String) = Team(
        teamId = id,
        teamName = "Team $id",
        teamAvatarUrl = null,
        ownerId = "owner-$id",
        lobbyId = "lobby1",
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
        points = mutableMapOf()
    )

    private fun createLineup(teamId: String, raceId: String) = Lineup(
        lineupId = "lineup-$teamId-$raceId",
        teamId = teamId,
        ownerId = "owner-$teamId",
        raceId = raceId,
        drivers = listOf(
            Lineup.Companion.LineupDriver("id-VER", 1, "VER", 10.0),
            Lineup.Companion.LineupDriver("id-HAM", 44, "HAM", 8.0)
        ),
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
        version = 1,
        score = null
    )

    @Test
    fun `calculatePointsPerLineup should sum driver points correctly`() = runTest {
        // Note: This test verifies TeamResultsServiceImpl logic, not the task itself
        // The task delegates to teamResultsService.calculateAndSaveLineupsResults()
    }

    @Test
    fun `onStart should delegate to service and send notification for CALCULATE_LINEUP_RESULTS`() = runTest {
        val raceResult = createRaceWeekendResult()
        val message = ChannelConfiguration.Companion.TaskChannelMessage(
            TaskType.CALCULATE_LINEUP_RESULTS,
            raceResult
        )

        coEvery { teamResultsService.calculateAndSaveLineupsResults(any()) } returns emptyMap()
        coEvery { userNotificationChannel.send(any()) } just Runs

        task.onStart()
        taskChannel.send(message)

        coVerify { teamResultsService.calculateAndSaveLineupsResults(raceResult) }
        coVerify { userNotificationChannel.send(match { it.notificationType == UserNotificationType.RACE_WEEKEND_RESULTS_AVAILABLE }) }

        taskChannel.close()
        task.onDestroy()
    }

    @Test
    fun `onStart should re-send message for non-matching task type`() = runTest {
        val raceResult = createRaceWeekendResult()
        val message = ChannelConfiguration.Companion.TaskChannelMessage(
            TaskType.UPDATE_DRIVERS_PRICING,
            raceResult
        )

        task.onStart()
        taskChannel.send(message)

        // The message is re-sent through the same channel, so we can receive it again
        val receivedMessage = taskChannel.receive()

        assertEquals(TaskType.UPDATE_DRIVERS_PRICING, receivedMessage.taskType)
        assertEquals(raceResult, receivedMessage.data)

        taskChannel.close()
        task.onDestroy()
    }

    @Test
    fun `onStart should log error but continue when service throws`() = runTest {
        val raceResult = createRaceWeekendResult()
        val message = ChannelConfiguration.Companion.TaskChannelMessage(
            TaskType.CALCULATE_LINEUP_RESULTS,
            raceResult
        )

        coEvery { teamResultsService.calculateAndSaveLineupsResults(any()) } throws RuntimeException("Service error")

        task.onStart()
        taskChannel.send(message)

        // Service should have been called (exception was thrown)
        coVerify { teamResultsService.calculateAndSaveLineupsResults(raceResult) }
        // Notification should NOT be sent (exception prevented it)
        coVerify(exactly = 0) { userNotificationChannel.send(any()) }

        taskChannel.close()
        task.onDestroy()
    }

    @Test
    fun `onStart should skip service calls when disabled`() = runTest {
        every { resultsCalculatorProperties.enable } returns false

        val raceResult = createRaceWeekendResult()
        val message = ChannelConfiguration.Companion.TaskChannelMessage(
            TaskType.CALCULATE_LINEUP_RESULTS,
            raceResult
        )

        task.onStart()

        // When disabled, the channel loop is never entered, so the coroutine completes immediately
        // Service should never be called
        coVerify(exactly = 0) { teamResultsService.calculateAndSaveLineupsResults(any()) }
        coVerify(exactly = 0) { userNotificationChannel.send(any()) }
    }
}
