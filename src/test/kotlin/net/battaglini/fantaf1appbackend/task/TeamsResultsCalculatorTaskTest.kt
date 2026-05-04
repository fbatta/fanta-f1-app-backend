package net.battaglini.fantaf1appbackend.task

import com.google.api.core.ApiFuture
import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Transaction
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.battaglini.fantaf1appbackend.configuration.ChannelConfiguration
import net.battaglini.fantaf1appbackend.configuration.ResultsCalculatorProperties
import net.battaglini.fantaf1appbackend.service.TeamResultsService
import net.battaglini.fantaf1appbackend.service.TeamResultsServiceImpl
import net.battaglini.fantaf1appbackend.enums.UserNotificationType
import net.battaglini.fantaf1appbackend.model.*
import net.battaglini.fantaf1appbackend.repository.LineupRepository
import net.battaglini.fantaf1appbackend.repository.TeamRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

@ExtendWith(MockKExtension::class)
class TeamsResultsCalculatorTaskTest {

    @MockK
    lateinit var resultsCalculatorProperties: ResultsCalculatorProperties

    @MockK
    lateinit var taskChannel: Channel<ChannelConfiguration.Companion.TaskChannelMessage>

    @MockK
    lateinit var userNotificationChannel: Channel<ChannelConfiguration.Companion.UserNotificationChannelMessage>

    @MockK
    lateinit var teamRepository: TeamRepository

    @MockK
    lateinit var lineupRepository: LineupRepository

    @MockK
    lateinit var firestore: Firestore

    @MockK
    lateinit var clock: Clock

    @MockK(relaxed = true)
    lateinit var timeZone: TimeZone

    @MockK
    lateinit var teamResultsService: TeamResultsService

    @InjectMockKs
    lateinit var task: TeamsResultsCalculatorTask

    @InjectMockKs
    lateinit var teamResultsServiceImpl: TeamResultsServiceImpl

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        every { resultsCalculatorProperties.enable } returns true
        every { resultsCalculatorProperties.dryRun } returns false
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
        val raceResult = createRaceWeekendResult()
        val driverPoints = raceResult.results.associate { it.driverAcronym to it.points }
        val lineup = createLineup("team1", "race1")

        val points = teamResultsServiceImpl.calculatePointsPerLineup(lineup, driverPoints)

        // VER (20.0) + HAM (15.0) = 35.0
        assertEquals(35.0, points)
    }

     @Test
    fun `runTask should delegate to service and send notification`() = runTest {
        val raceResult = createRaceWeekendResult()
        val message = ChannelConfiguration.Companion.TaskChannelMessage(
            net.battaglini.fantaf1appbackend.enums.TaskType.RACE_WEEKEND_RESULTS_CALCULATION_COMPLETED,
            raceResult
        )

        every { taskChannel.tryReceive().getOrNull() } returns message
        coEvery { teamResultsService.calculateAndSaveLineupsResults(any()) } returns emptyMap()
        coEvery { userNotificationChannel.send(any()) } just Runs

        task.runTask()
        
        coVerify { teamResultsService.calculateAndSaveLineupsResults(raceResult) }
        coVerify { userNotificationChannel.send(match { it.notificationType == UserNotificationType.RACE_WEEKEND_RESULTS_AVAILABLE }) }
    }

    @Test
    fun `runTask should do nothing when channel is empty`() = runTest {
        every { taskChannel.tryReceive().getOrNull() } returns null
        every { resultsCalculatorProperties.enable } returns true

        task.runTask()
        
        coVerify(exactly = 0) { teamResultsService.calculateAndSaveLineupsResults(any()) }
        coVerify(exactly = 0) { userNotificationChannel.send(any()) }
    }

    @Test
    fun `runTask should skip when disabled`() = runTest {
        every { resultsCalculatorProperties.enable } returns false

        task.runTask()
        
        coVerify(exactly = 0) { taskChannel.tryReceive() }
        coVerify(exactly = 0) { teamResultsService.calculateAndSaveLineupsResults(any()) }
    }
}
