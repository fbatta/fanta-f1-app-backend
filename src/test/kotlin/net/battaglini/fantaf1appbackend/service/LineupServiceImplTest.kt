package net.battaglini.fantaf1appbackend.service

import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import net.battaglini.fantaf1appbackend.exception.InvalidRequestException
import net.battaglini.fantaf1appbackend.model.*
import net.battaglini.fantaf1appbackend.model.request.CreateLineupRequest
import net.battaglini.fantaf1appbackend.model.response.CreateLineupResponse
import net.battaglini.fantaf1appbackend.repository.DriverCostRepository
import net.battaglini.fantaf1appbackend.repository.DriverRepository
import net.battaglini.fantaf1appbackend.repository.LineupRepository
import net.battaglini.fantaf1appbackend.repository.TeamRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Clock
import kotlin.time.Instant

@ExtendWith(MockKExtension::class)
class LineupServiceImplTest {

    @MockK
    lateinit var clock: Clock

    @MockK
    lateinit var raceWeekendService: RaceWeekendService

    @MockK
    lateinit var teamRepository: TeamRepository

    @MockK
    lateinit var driverRepository: DriverRepository

    @MockK
    lateinit var driverCostRepository: DriverCostRepository

    @MockK
    lateinit var lineupRepository: LineupRepository

    @InjectMockKs
    lateinit var service: LineupServiceImpl

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    private fun createRaceWeekend(
        raceId: String = "race1",
        raceName: String = "Test Race"
    ) = RaceWeekend(
        raceId = raceId,
        openF1MeetingKey = 1,
        raceName = raceName,
        dateStart = Instant.fromEpochMilliseconds(0),
        dateEnd = Instant.fromEpochMilliseconds(0),
        sessions = emptyList(),
        circuitImage = "circuit.png",
        countryName = "Test Country",
        countryFlag = "🇹🇪",
        circuitType = "Permanent",
        dateLineupOpen = Instant.fromEpochMilliseconds(0),
        dateLineupClose = Instant.fromEpochMilliseconds(0)
    )

    private fun createTeam(
        teamId: String = "team1",
        ownerId: String = "owner1"
    ) = Team(
        teamId = teamId,
        teamName = "Test Team",
        teamAvatarUrl = null,
        ownerId = ownerId,
        lobbyId = "lobby1",
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
        points = mutableMapOf()
    )

    private fun createDriver(
        driverId: String = "id-VER",
        driverNumber: Int = 1,
        acronym: String = "VER",
        name: String = "Max Verstappen",
        teamName: String = "Red Bull Racing",
        teamColour: String = "#3671C6"
    ) = Driver(
        driverId = driverId,
        driverNumber = driverNumber,
        acronym = acronym,
        driverAvatar = "avatar.png",
        initialCost = 0,
        isActive = true,
        name = name,
        teamName = teamName,
        teamColour = teamColour
    )

    private fun createDriverCost(
        driverId: String = "id-VER",
        driverCost: Double = 10.0
    ) = DriverCost(
        driverId = driverId,
        driverCost = driverCost
    )

    @Test
    fun `createLineup should create lineup successfully`() = runTest {
        val now = Instant.fromEpochMilliseconds(1000)
        every { clock.now() } returns now

        val raceWeekend = createRaceWeekend()
        val team = createTeam()
        val driver = createDriver()
        val driverCost = createDriverCost()

        coEvery { raceWeekendService.getRaceWeekend("race1") } returns raceWeekend
        coEvery { teamRepository.getTeamByTeamId("team1") } returns team
        coEvery { driverRepository.findDriverById("id-VER") } returns driver
        coEvery { driverCostRepository.getDriverCostByDriverId("id-VER") } returns driverCost
        coEvery { lineupRepository.createOrUpdateLineup(any()) } just Runs

        val request = CreateLineupRequest(
            teamId = "team1",
            raceId = "race1",
            driverIds = listOf("id-VER")
        )

        val response = service.createLineup(request)

        assertEquals("lineup-team1-race1", response.lineupId)
        assertEquals("team1", response.teamId)
        assertEquals("race1", response.raceId)
        assertEquals(1, response.drivers.size)
        assertEquals("id-VER", response.drivers[0].driverId)
        assertEquals(1, response.drivers[0].driverNumber)
        assertEquals("VER", response.drivers[0].driverAcronym)
        assertEquals(10.0, response.drivers[0].driverCost)

        coVerify { lineupRepository.createOrUpdateLineup(any()) }
    }

    @Test
    fun `createLineup should update existing lineup`() = runTest {
        val now = Instant.fromEpochMilliseconds(1000)
        every { clock.now() } returns now

        val raceWeekend = createRaceWeekend()
        val team = createTeam()
        val driver = createDriver()
        val driverCost = createDriverCost()

        coEvery { raceWeekendService.getRaceWeekend("race1") } returns raceWeekend
        coEvery { teamRepository.getTeamByTeamId("team1") } returns team
        coEvery { driverRepository.findDriverById("id-VER") } returns driver
        coEvery { driverCostRepository.getDriverCostByDriverId("id-VER") } returns driverCost
        coEvery { lineupRepository.createOrUpdateLineup(any()) } just Runs

        val request = CreateLineupRequest(
            teamId = "team1",
            raceId = "race1",
            driverIds = listOf("id-VER")
        )

        val response = service.createLineup(request)

        coVerify { lineupRepository.createOrUpdateLineup(any()) }
    }

    @Test
    fun `createLineup should create lineup with multiple drivers`() = runTest {
        val now = Instant.fromEpochMilliseconds(1000)
        every { clock.now() } returns now

        val raceWeekend = createRaceWeekend()
        val team = createTeam()
        val driver1 = createDriver("id-VER", 1, "VER")
        val driver2 = createDriver("id-HAM", 44, "HAM")
        val cost1 = createDriverCost("id-VER", 10.0)
        val cost2 = createDriverCost("id-HAM", 8.0)

        coEvery { raceWeekendService.getRaceWeekend("race1") } returns raceWeekend
        coEvery { teamRepository.getTeamByTeamId("team1") } returns team
        coEvery { driverRepository.findDriverById("id-VER") } returns driver1
        coEvery { driverRepository.findDriverById("id-HAM") } returns driver2
        coEvery { driverCostRepository.getDriverCostByDriverId("id-VER") } returns cost1
        coEvery { driverCostRepository.getDriverCostByDriverId("id-HAM") } returns cost2
        coEvery { lineupRepository.createOrUpdateLineup(any()) } just Runs

        val request = CreateLineupRequest(
            teamId = "team1",
            raceId = "race1",
            driverIds = listOf("id-VER", "id-HAM")
        )

        val response = service.createLineup(request)

        assertEquals(2, response.drivers.size)
        assertEquals("id-VER", response.drivers[0].driverId)
        assertEquals("id-HAM", response.drivers[1].driverId)
        assertEquals(10.0, response.drivers[0].driverCost)
        assertEquals(8.0, response.drivers[1].driverCost)
    }

    @Test
    fun `createLineup should throw when race weekend not found`() = runTest {
        val raceWeekend = createRaceWeekend()
        coEvery { raceWeekendService.getRaceWeekend("nonexistent") } returns null
        coEvery { teamRepository.getTeamByTeamId("team1") } returns createTeam()
        coEvery { driverRepository.findDriverById("id-VER") } returns createDriver()
        coEvery { driverCostRepository.getDriverCostByDriverId("id-VER") } returns createDriverCost()

        val request = CreateLineupRequest(
            teamId = "team1",
            raceId = "nonexistent",
            driverIds = listOf("id-VER")
        )

        val e = assertThrows<InvalidRequestException> {
            service.createLineup(request)
        }
        assertEquals("RaceWeekend with raceId=nonexistent not found", e.message)
    }

    @Test
    fun `createLineup should throw when team not found`() = runTest {
        val raceWeekend = createRaceWeekend()
        coEvery { raceWeekendService.getRaceWeekend("race1") } returns raceWeekend
        coEvery { teamRepository.getTeamByTeamId("team1") } returns null

        val request = CreateLineupRequest(
            teamId = "team1",
            raceId = "race1",
            driverIds = listOf("id-VER")
        )

        val e = assertThrows<InvalidRequestException> {
            service.createLineup(request)
        }
        assertEquals("Team with teamId=team1 not found", e.message)
    }

    @Test
    fun `createLineup should throw when driver not found`() = runTest {
        val now = Instant.fromEpochMilliseconds(1000)
        every { clock.now() } returns now
        val raceWeekend = createRaceWeekend()
        val team = createTeam()
        coEvery { raceWeekendService.getRaceWeekend("race1") } returns raceWeekend
        coEvery { teamRepository.getTeamByTeamId("team1") } returns team
        coEvery { driverRepository.findDriverById("id-VER") } returns null

        val request = CreateLineupRequest(
            teamId = "team1",
            raceId = "race1",
            driverIds = listOf("id-VER")
        )

        val e = assertThrows<InvalidRequestException> {
            service.createLineup(request)
        }
        assertEquals("Driver with driverId=id-VER not found", e.message)
    }

    @Test
    fun `createLineup should throw when driver cost not found`() = runTest {
        val now = Instant.fromEpochMilliseconds(1000)
        every { clock.now() } returns now
        val raceWeekend = createRaceWeekend()
        val team = createTeam()
        val driver = createDriver()
        coEvery { raceWeekendService.getRaceWeekend("race1") } returns raceWeekend
        coEvery { teamRepository.getTeamByTeamId("team1") } returns team
        coEvery { driverRepository.findDriverById("id-VER") } returns driver
        coEvery { driverCostRepository.getDriverCostByDriverId("id-VER") } returns null

        val request = CreateLineupRequest(
            teamId = "team1",
            raceId = "race1",
            driverIds = listOf("id-VER")
        )

        val e = assertThrows<InvalidRequestException> {
            service.createLineup(request)
        }
        assertEquals("Driver cost not found for driverId=id-VER", e.message)
    }

    @Test
    fun `createLineup should throw when driver IDs list is empty`() = runTest {
        val raceWeekend = createRaceWeekend()
        val team = createTeam()
        coEvery { raceWeekendService.getRaceWeekend("race1") } returns raceWeekend
        coEvery { teamRepository.getTeamByTeamId("team1") } returns team

        val request = CreateLineupRequest(
            teamId = "team1",
            raceId = "race1",
            driverIds = emptyList()
        )

        val e = assertThrows<InvalidRequestException> {
            service.createLineup(request)
        }
        assertEquals("Driver IDs list cannot be empty", e.message)
    }

    @Test
    fun `createLineup should use team ownerId as lineup ownerId`() = runTest {
        val now = Instant.fromEpochMilliseconds(1000)
        every { clock.now() } returns now

        val raceWeekend = createRaceWeekend()
        val team = createTeam("team1", "owner1")
        val driver = createDriver()
        val driverCost = createDriverCost()

        coEvery { raceWeekendService.getRaceWeekend("race1") } returns raceWeekend
        coEvery { teamRepository.getTeamByTeamId("team1") } returns team
        coEvery { driverRepository.findDriverById("id-VER") } returns driver
        coEvery { driverCostRepository.getDriverCostByDriverId("id-VER") } returns driverCost
        coEvery { lineupRepository.createOrUpdateLineup(any()) } just Runs

        val request = CreateLineupRequest(
            teamId = "team1",
            raceId = "race1",
            driverIds = listOf("id-VER")
        )

        service.createLineup(request)

        coVerify {
            lineupRepository.createOrUpdateLineup(
                match {
                    it.ownerId == "owner1" && it.teamId == "team1" && it.raceId == "race1"
                }
            )
        }
    }

    @Test
    fun `createLineup should set createdAt and updatedAt to current time`() = runTest {
        val now = Instant.fromEpochMilliseconds(1000)
        every { clock.now() } returns now

        val raceWeekend = createRaceWeekend()
        val team = createTeam()
        val driver = createDriver()
        val driverCost = createDriverCost()

        coEvery { raceWeekendService.getRaceWeekend("race1") } returns raceWeekend
        coEvery { teamRepository.getTeamByTeamId("team1") } returns team
        coEvery { driverRepository.findDriverById("id-VER") } returns driver
        coEvery { driverCostRepository.getDriverCostByDriverId("id-VER") } returns driverCost
        coEvery { lineupRepository.createOrUpdateLineup(any()) } just Runs

        val request = CreateLineupRequest(
            teamId = "team1",
            raceId = "race1",
            driverIds = listOf("id-VER")
        )

        service.createLineup(request)

        coVerify {
            lineupRepository.createOrUpdateLineup(
                match {
                    it.createdAt == now && it.updatedAt == now && it.version == 1 && it.score == null
                }
            )
        }
    }
}
