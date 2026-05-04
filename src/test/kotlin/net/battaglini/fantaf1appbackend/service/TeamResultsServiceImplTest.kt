package net.battaglini.fantaf1appbackend.service

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
import kotlinx.datetime.TimeZone
import net.battaglini.fantaf1appbackend.configuration.ChannelConfiguration
import net.battaglini.fantaf1appbackend.configuration.ResultsCalculatorProperties
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

@ExtendWith(MockKExtension::class)
class TeamResultsServiceImplTest {

    @MockK
    lateinit var clock: Clock

    @MockK(relaxed = true)
    lateinit var timeZone: TimeZone

    @MockK
    lateinit var teamRepository: TeamRepository

    @MockK
    lateinit var lineupRepository: LineupRepository

    @MockK
    lateinit var resultsCalculatorProperties: ResultsCalculatorProperties

    @MockK
    lateinit var firestore: Firestore

    @InjectMockKs
    lateinit var service: TeamResultsServiceImpl

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        every { resultsCalculatorProperties.dryRun } returns false
    }

    private fun createRaceWeekendResult(
        raceId: String = "race1",
        results: List<RaceWeekendResult.Companion.Result> = listOf(
            RaceWeekendResult.Companion.Result("id-VER", 1, "VER", 25.0),
            RaceWeekendResult.Companion.Result("id-HAM", 44, "HAM", 18.0)
        )
    ) = RaceWeekendResult(
        raceId = raceId,
        raceName = "Test Race",
        openF1MeetingKey = 1,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
        version = 1,
        results = results
    )

    private fun createTeam(
        id: String = "team1",
        yearPoints: Double = 0.0
    ) = Team(
        teamId = id,
        teamName = "Team $id",
        teamAvatarUrl = null,
        ownerId = "owner-$id",
        lobbyId = "lobby1",
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
        points = mutableMapOf(2025 to yearPoints)
    )

    private fun createLineup(
        teamId: String = "team1",
        raceId: String = "race1",
        drivers: List<Lineup.Companion.LineupDriver> = listOf(
            Lineup.Companion.LineupDriver("id-VER", 1, "VER", 10.0),
            Lineup.Companion.LineupDriver("id-HAM", 44, "HAM", 8.0)
        )
    ) = Lineup(
        lineupId = "lineup-$teamId-$raceId",
        teamId = teamId,
        ownerId = "owner-$teamId",
        raceId = raceId,
        drivers = drivers,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
        version = 1,
        score = null
    )

    @Test
    fun `calculatePointsPerLineup should sum driver points from race results`() = runTest {
        val lineup = createLineup(
            drivers = listOf(
                Lineup.Companion.LineupDriver("id-VER", 1, "VER", 10.0),
                Lineup.Companion.LineupDriver("id-HAM", 44, "HAM", 8.0)
            )
        )
        val driverPoints = mapOf("VER" to 25.0, "HAM" to 18.0)

        val points = service.calculatePointsPerLineup(lineup, driverPoints)

        // VER (25.0) + HAM (18.0) = 43.0
        assertEquals(43.0, points)
    }

    @Test
    fun `calculatePointsPerLineup should return 0 for drivers not in results`() = runTest {
        val lineup = createLineup(
            drivers = listOf(
                Lineup.Companion.LineupDriver("id-VER", 1, "VER", 10.0),
                Lineup.Companion.LineupDriver("id-ALT", 3, "ALT", 12.0)
            )
        )
        // Only VER is in results, ALT is not
        val driverPoints = mapOf("VER" to 25.0)

        val points = service.calculatePointsPerLineup(lineup, driverPoints)

        assertEquals(25.0, points)
    }

    @Test
    fun `calculatePointsPerLineup should handle empty lineup`() = runTest {
        val lineup = createLineup(drivers = emptyList())
        val driverPoints = mapOf("VER" to 25.0)

        val points = service.calculatePointsPerLineup(lineup, driverPoints)

        assertEquals(0.0, points)
    }

    @Test
    fun `calculateAndSaveLineupsResults should calculate and save for all teams`() = runTest {
        val raceResult = createRaceWeekendResult()
        val now = Instant.parse("2025-05-04T12:00:00Z")
        every { clock.now() } returns now

        val team1 = createTeam("team1", 100.0)
        val mockSnapshot1 = mockk<DocumentSnapshot>()
        val mockSnapshot2 = mockk<DocumentSnapshot>()

        // Pagination: first page returns team1, second page is empty
        coEvery { teamRepository.getAllTeams(null) } returns flowOf(Pair(mockSnapshot1, team1))
        coEvery { teamRepository.getAllTeams(mockSnapshot1) } returns emptyFlow()

        val lineup1 = createLineup("team1", "race1")
        coEvery { lineupRepository.getLineup("team1", "race1") } returns lineup1

        val mockTransaction = mockk<Transaction>()
        val mockApiFuture = mockk<ApiFuture<Void>>()
        every { mockApiFuture.get() } returns null
        every { firestore.runTransaction<Void>(any()) } answers {
            val updateFunction = firstArg<Transaction.Function<Void>>()
            updateFunction.updateCallback(mockTransaction)
            mockApiFuture
        }
        coEvery { lineupRepository.updateLineupInTransaction(any(), any()) } just Runs
        coEvery { teamRepository.updateTeamInTransaction(any(), any()) } just Runs

        val result = service.calculateAndSaveLineupsResults(raceResult)

        assertEquals(1, result.size)
        val entry = result.entries.first()
        assertEquals("team1", entry.key.teamId)
        val savedLineup = entry.value
        assertNotNull(savedLineup)
        assertEquals(43.0, savedLineup!!.score)
        assertEquals(2, savedLineup.version)
        assertEquals(now, savedLineup.updatedAt)

        // Team should have updated points: 100.0 + 43.0 = 143.0
        coVerify {
            teamRepository.updateTeamInTransaction(
                match { it.teamId == "team1" && it.points[2025] == 143.0 },
                any()
            )
        }
        coVerify {
            lineupRepository.updateLineupInTransaction(
                match { it.score == 43.0 },
                any()
            )
        }
    }

    @Test
    fun `calculateAndSaveLineupsResults should skip saving when lineup not found`() = runTest {
        val raceResult = createRaceWeekendResult()
        val now = Instant.parse("2025-05-04T12:00:00Z")
        every { clock.now() } returns now

        val team1 = createTeam("team1", 100.0)
        val mockSnapshot = mockk<DocumentSnapshot>()
        coEvery { teamRepository.getAllTeams(null) } returns flowOf(Pair(mockSnapshot, team1))
        coEvery { teamRepository.getAllTeams(mockSnapshot) } returns emptyFlow()

        // No lineup found
        coEvery { lineupRepository.getLineup("team1", "race1") } returns null

        val result = service.calculateAndSaveLineupsResults(raceResult)

        assertEquals(1, result.size)
        val entry = result.entries.first()
        assertEquals("team1", entry.key.teamId)
        assertNull(entry.value)

        // Should NOT save anything since lineup was null
        coVerify(exactly = 0) { firestore.runTransaction<Void>(any()) }
    }

    @Test
    fun `calculateAndSaveLineupsResults should not save to DB in dryRun mode`() = runTest {
        every { resultsCalculatorProperties.dryRun } returns true
        val raceResult = createRaceWeekendResult()
        val now = Instant.parse("2025-05-04T12:00:00Z")
        every { clock.now() } returns now

        val team1 = createTeam("team1", 100.0)
        val mockSnapshot = mockk<DocumentSnapshot>()
        coEvery { teamRepository.getAllTeams(null) } returns flowOf(Pair(mockSnapshot, team1))
        coEvery { teamRepository.getAllTeams(mockSnapshot) } returns emptyFlow()

        val lineup1 = createLineup("team1", "race1")
        coEvery { lineupRepository.getLineup("team1", "race1") } returns lineup1

        val result = service.calculateAndSaveLineupsResults(raceResult)

        // Should still calculate but not save
        assertEquals(1, result.size)
        val entry = result.entries.first()
        assertEquals("team1", entry.key.teamId)
        assertNotNull(entry.value)
        assertEquals(43.0, entry.value!!.score)

        // No transaction should be executed
        coVerify(exactly = 0) { firestore.runTransaction<Void>(any()) }
    }

    @Test
    fun `calculateAndSaveLineupsResults should handle transaction failure gracefully`() = runTest {
        val raceResult = createRaceWeekendResult()
        val now = Instant.parse("2025-05-04T12:00:00Z")
        every { clock.now() } returns now

        val team1 = createTeam("team1", 100.0)
        val mockSnapshot = mockk<DocumentSnapshot>()
        coEvery { teamRepository.getAllTeams(null) } returns flowOf(Pair(mockSnapshot, team1))
        coEvery { teamRepository.getAllTeams(mockSnapshot) } returns emptyFlow()

        val lineup1 = createLineup("team1", "race1")
        coEvery { lineupRepository.getLineup("team1", "race1") } returns lineup1

        val mockTransaction = mockk<Transaction>()
        val mockApiFuture = mockk<ApiFuture<Void>>()
        every { mockApiFuture.get() } throws RuntimeException("Transaction failed")
        every { firestore.runTransaction<Void>(any()) } answers {
            val updateFunction = firstArg<Transaction.Function<Void>>()
            updateFunction.updateCallback(mockTransaction)
            mockApiFuture
        }

        // Should not throw - exception is caught internally
        val result = service.calculateAndSaveLineupsResults(raceResult)

        // Should still return calculated results
        assertEquals(1, result.size)
        val entry = result.entries.first()
        assertEquals("team1", entry.key.teamId)
        assertNotNull(entry.value)
    }

    @Test
    fun `calculateAndSaveLineupsResults should add points for current year`() = runTest {
        val raceResult = createRaceWeekendResult()
        val now = Instant.parse("2025-05-04T12:00:00Z")
        every { clock.now() } returns now

        // Team with no 2025 points
        val team1 = createTeam("team1", 0.0)
        team1.points.clear()
        val mockSnapshot = mockk<DocumentSnapshot>()
        coEvery { teamRepository.getAllTeams(null) } returns flowOf(Pair(mockSnapshot, team1))
        coEvery { teamRepository.getAllTeams(mockSnapshot) } returns emptyFlow()

        val lineup1 = createLineup("team1", "race1")
        coEvery { lineupRepository.getLineup("team1", "race1") } returns lineup1

        val mockTransaction = mockk<Transaction>()
        val mockApiFuture = mockk<ApiFuture<Void>>()
        every { mockApiFuture.get() } returns null
        every { firestore.runTransaction<Void>(any()) } answers {
            val updateFunction = firstArg<Transaction.Function<Void>>()
            updateFunction.updateCallback(mockTransaction)
            mockApiFuture
        }
        coEvery { lineupRepository.updateLineupInTransaction(any(), any()) } just Runs
        coEvery { teamRepository.updateTeamInTransaction(any(), any()) } just Runs

        service.calculateAndSaveLineupsResults(raceResult)

        coVerify {
            teamRepository.updateTeamInTransaction(
                match { it.points.containsKey(2025) && it.points[2025] == 43.0 },
                any()
            )
        }
    }

    @Test
    fun `calculateAndSaveLineupsResults should preserve existing year points and add new`() = runTest {
        val raceResult = createRaceWeekendResult()
        val now = Instant.parse("2025-05-04T12:00:00Z")
        every { clock.now() } returns now

        // Team with existing 2025 points
        val team1 = createTeam("team1", 200.0)
        val mockSnapshot = mockk<DocumentSnapshot>()
        coEvery { teamRepository.getAllTeams(null) } returns flowOf(Pair(mockSnapshot, team1))
        coEvery { teamRepository.getAllTeams(mockSnapshot) } returns emptyFlow()

        val lineup1 = createLineup("team1", "race1")
        coEvery { lineupRepository.getLineup("team1", "race1") } returns lineup1

        val mockTransaction = mockk<Transaction>()
        val mockApiFuture = mockk<ApiFuture<Void>>()
        every { mockApiFuture.get() } returns null
        every { firestore.runTransaction<Void>(any()) } answers {
            val updateFunction = firstArg<Transaction.Function<Void>>()
            updateFunction.updateCallback(mockTransaction)
            mockApiFuture
        }
        coEvery { lineupRepository.updateLineupInTransaction(any(), any()) } just Runs
        coEvery { teamRepository.updateTeamInTransaction(any(), any()) } just Runs

        service.calculateAndSaveLineupsResults(raceResult)

        coVerify {
            teamRepository.updateTeamInTransaction(
                match { it.points[2025] == 243.0 },
                any()
            )
        }
    }

    @Test
    fun `calculateAndSaveLineupsResults should handle multiple teams with pagination`() = runTest {
        val raceResult = createRaceWeekendResult()
        val now = Instant.parse("2025-05-04T12:00:00Z")
        every { clock.now() } returns now

        val team1 = createTeam("team1", 100.0)
        val team2 = createTeam("team2", 150.0)
        val mockSnapshot1 = mockk<DocumentSnapshot>()
        val mockSnapshot2 = mockk<DocumentSnapshot>()

        // Pagination: first page team1, second page team2, third page empty
        coEvery { teamRepository.getAllTeams(null) } returns flowOf(Pair(mockSnapshot1, team1))
        coEvery { teamRepository.getAllTeams(mockSnapshot1) } returns flowOf(Pair(mockSnapshot2, team2))
        coEvery { teamRepository.getAllTeams(mockSnapshot2) } returns emptyFlow()

        val lineup1 = createLineup("team1", "race1")
        val lineup2 = createLineup("team2", "race1")
        coEvery { lineupRepository.getLineup("team1", "race1") } returns lineup1
        coEvery { lineupRepository.getLineup("team2", "race1") } returns lineup2

        val mockTransaction = mockk<Transaction>()
        val mockApiFuture = mockk<ApiFuture<Void>>()
        every { mockApiFuture.get() } returns null
        every { firestore.runTransaction<Void>(any()) } answers {
            val updateFunction = firstArg<Transaction.Function<Void>>()
            updateFunction.updateCallback(mockTransaction)
            mockApiFuture
        }
        coEvery { lineupRepository.updateLineupInTransaction(any(), any()) } just Runs
        coEvery { teamRepository.updateTeamInTransaction(any(), any()) } just Runs

        val result = service.calculateAndSaveLineupsResults(raceResult)

        assertEquals(2, result.size)
        val teamIds = result.keys.map { it.teamId }
        assertTrue(teamIds.contains("team1"))
        assertTrue(teamIds.contains("team2"))
        val team1Entry = result.entries.find { it.key.teamId == "team1" }
        val team2Entry = result.entries.find { it.key.teamId == "team2" }
        assertNotNull(team1Entry?.value)
        assertNotNull(team2Entry?.value)
    }

    @Test
    fun `calculateAndSaveLineupsResults should return empty map when no teams exist`() = runTest {
        val raceResult = createRaceWeekendResult()
        coEvery { teamRepository.getAllTeams(null) } returns emptyFlow()

        val result = service.calculateAndSaveLineupsResults(raceResult)

        assertTrue(result.isEmpty())
    }
}
