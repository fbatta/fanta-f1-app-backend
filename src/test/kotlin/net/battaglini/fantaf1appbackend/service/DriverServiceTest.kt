package net.battaglini.fantaf1appbackend.service

import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import net.battaglini.fantaf1appbackend.client.OpenF1Client
import net.battaglini.fantaf1appbackend.configuration.SeedingProperties
import net.battaglini.fantaf1appbackend.exception.DriverNotFoundException
import net.battaglini.fantaf1appbackend.model.Driver
import net.battaglini.fantaf1appbackend.model.RaceWeekend
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1DriverResponse
import net.battaglini.fantaf1appbackend.model.request.UpdateDriversCostsRequest
import net.battaglini.fantaf1appbackend.repository.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@ExtendWith(MockKExtension::class)
class DriverServiceTest {

    @MockK
    lateinit var openF1Client: OpenF1Client

    @MockK
    lateinit var genAIService: GenAIService

    @MockK
    lateinit var driverRepository: DriverRepository

    @MockK
    lateinit var driverCostRepository: DriverCostRepository

    @MockK
    lateinit var driverSummaryRepository: DriverSummaryRepository

    @MockK
    lateinit var raceWeekendResultRepository: RaceWeekendResultRepository

    @MockK
    lateinit var raceRepository: RaceRepository

    @MockK
    lateinit var seedingProperties: SeedingProperties

    @MockK
    lateinit var clock: Clock

    val timeZone = TimeZone.UTC

    @InjectMockKs
    lateinit var driverService: DriverService

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    private fun createOpenF1Driver(
        acronym: String = "VER",
        driverNumber: Int = 1,
        firstName: String = "Max",
        lastName: String = "Verstappen",
        fullName: String = "Max Verstappen"
    ) = OpenF1DriverResponse(
        broadcastName = "$firstName $lastName",
        driverNumber = driverNumber,
        firstName = firstName,
        fullName = fullName,
        headshotUrl = "url",
        lastName = lastName,
        meetingKey = "1234",
        nameAcronym = acronym,
        sessionKey = "latest",
        teamColour = "000000",
        teamName = "Red Bull Racing"
    )

    private fun createDriver(
        id: String = "uuid",
        acronym: String = "VER",
        driverNumber: Int = 1,
        name: String = "Max Verstappen"
    ) = Driver(
        driverId = id,
        driverNumber = driverNumber,
        acronym = acronym,
        driverAvatar = "url",
        initialCost = 0,
        isActive = true,
        name = name,
        teamName = "Red Bull Racing",
        teamColour = "000000"
    )

    private fun createRaceWeekend(raceId: String = "race1", year: Int = 2023) = RaceWeekend(
        raceId = raceId,
        openF1MeetingKey = 1,
        raceName = "Test Race",
        dateStart = Instant.parse("$year-01-01T12:00:00Z"),
        dateEnd = Instant.parse("$year-01-03T12:00:00Z"),
        sessions = emptyList(),
        circuitImage = "",
        countryName = "",
        countryFlag = "",
        circuitType = "",
        dateLineupOpen = Instant.parse("$year-01-01T12:00:00Z"),
        dateLineupClose = Instant.parse("$year-01-01T12:00:00Z")
    )

    private fun createRaceWeekendResult(
        raceId: String = "race1",
        driverId: String = "1",
        points: Double = 25.0
    ) = RaceWeekendResult(
        raceId = raceId,
        raceName = "Test Race",
        openF1MeetingKey = 1,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
        version = 1,
        results = listOf(RaceWeekendResult.Companion.Result(driverId, 1, "VER", points)),
        summaryParagraphs = null
    )

    @Test
    fun `onStart should call seedDrivers when drivers seeding is enabled`() = runTest {
        every { seedingProperties.drivers } returns true
        val openF1Driver = createOpenF1Driver()
        every { openF1Client.getDrivers(sessionKeys = listOf("latest")) } returns flowOf(openF1Driver)
        coEvery { driverRepository.createOrUpdateDrivers(any()) } just Runs

        driverService.onStart()

        verify { openF1Client.getDrivers(sessionKeys = listOf("latest")) }
        coVerify { driverRepository.createOrUpdateDrivers(any()) }
    }

    @Test
    fun `onStart should do nothing when drivers seeding is disabled`() = runTest {
        every { seedingProperties.drivers } returns false

        driverService.onStart()

        verify(exactly = 0) { openF1Client.getDrivers(any(), any(), any(), any()) }
        coVerify(exactly = 0) { driverRepository.createOrUpdateDrivers(any()) }
    }

    @Test
    fun `seedDrivers should fetch drivers from client and save to repository`() = runTest {
        val openF1Driver = createOpenF1Driver()
        every { openF1Client.getDrivers(sessionKeys = listOf("latest")) } returns flowOf(openF1Driver)
        coEvery { driverRepository.createOrUpdateDrivers(any()) } just Runs

        driverService.seedDrivers()

        coVerify {
            driverRepository.createOrUpdateDrivers(withArg { drivers ->
                assertEquals(1, drivers.size)
                assertEquals(openF1Driver.nameAcronym, drivers[0].acronym)
                assertEquals(openF1Driver.driverNumber, drivers[0].driverNumber)
                assertEquals(openF1Driver.fullName, drivers[0].name)
            })
        }
    }

    @Test
    fun `seedDrivers should handle exceptions gracefully without throwing`() = runTest {
        every { openF1Client.getDrivers(sessionKeys = listOf("latest")) } throws RuntimeException("API Error")

        driverService.seedDrivers()

        coVerify(exactly = 0) { driverRepository.createOrUpdateDrivers(any()) }
    }

    @Test
    fun `updateDriversCosts should create and update costs successfully`() = runTest {
        val driver1 = createDriver(id = "1", acronym = "VER")
        val driver2 = createDriver(id = "2", acronym = "HAM")
        coEvery { driverRepository.getDrivers() } returns flowOf(driver1, driver2)
        coEvery { driverCostRepository.createOrUpdateDriversCosts(any()) } just Runs

        val request = UpdateDriversCostsRequest(
            driversCosts = listOf(
                UpdateDriversCostsRequest.Companion.DriverCostRequest("VER", 30.0),
                UpdateDriversCostsRequest.Companion.DriverCostRequest(
                    "ham",
                    28.5
                ) // Lowercase to test case-insensitivity
            )
        )

        driverService.updateDriversCosts(request)

        coVerify {
            driverCostRepository.createOrUpdateDriversCosts(withArg { costs ->
                assertEquals(2, costs.size)
                assertTrue(costs.any { it.driverId == "1" && it.driverCost == 30.0 })
                assertTrue(costs.any { it.driverId == "2" && it.driverCost == 28.5 })
            })
        }
    }

    @Test
    fun `updateDriversCosts should throw DriverNotFoundException when a driver is not found`() = runTest {
        val driver1 = createDriver(id = "1", acronym = "VER")
        coEvery { driverRepository.getDrivers() } returns flowOf(driver1)

        val request = UpdateDriversCostsRequest(
            driversCosts = listOf(
                UpdateDriversCostsRequest.Companion.DriverCostRequest("LEC", 25.0)
            )
        )

        val exception = assertThrows<DriverNotFoundException> {
            driverService.updateDriversCosts(request)
        }

        assertEquals("Driver with acronym LEC not found", exception.message)
        coVerify(exactly = 0) { driverCostRepository.createOrUpdateDriversCosts(any()) }
    }

    @Test
    fun `updateDriverSummary should generate and save summary`() = runTest {
        every { clock.now() } returns Instant.parse("2023-07-15T10:00:00Z")

        val driver = createDriver(id = "1", acronym = "VER", name = "Max Verstappen")
        val race1 = createRaceWeekend("race1", 2023)
        val result1 = createRaceWeekendResult("race1", "1", 25.0)

        coEvery { driverRepository.findDriverByAcronym("VER") } returns driver
        coEvery { driverRepository.findDriverById("1") } returns driver
        coEvery { raceRepository.getRacesByYear(2023) } returns flowOf(race1)
        coEvery { raceWeekendResultRepository.getRaceWeekendResults(listOf("race1")) } returns flowOf(result1)
        coEvery { genAIService.generateDriverSummary("Max Verstappen", 25.0) } returns flowOf("Great season")
        coEvery { driverSummaryRepository.createOrUpdateDriverSummary(any()) } just Runs

        driverService.updateDriverSummary("VER")

        coVerify { genAIService.generateDriverSummary("Max Verstappen", 25.0) }
        coVerify {
            driverSummaryRepository.createOrUpdateDriverSummary(withArg { summary ->
                assertEquals("1", summary.driverId)
                assertEquals(listOf("Great season"), summary.summaryParagraphs)
            })
        }
    }

    @Test
    fun `updateDriverSummary should skip saving when summary is empty`() = runTest {
        val race1 = createRaceWeekend("race1", 2023)
        val result1 = createRaceWeekendResult("race1", "1", 25.0)

        every { clock.now() } returns Instant.parse("2023-07-15T10:00:00Z")

        val driver = createDriver(id = "1", acronym = "VER", name = "Max Verstappen")
        coEvery { driverRepository.findDriverByAcronym("VER") } returns driver
        coEvery { driverRepository.findDriverById("1") } returns driver
        coEvery { raceRepository.getRacesByYear(2023) } returns flowOf(race1)
        coEvery { raceWeekendResultRepository.getRaceWeekendResults(listOf("race1")) } returns flowOf(result1)
        coEvery { genAIService.generateDriverSummary(any(), any()) } returns emptyFlow()

        driverService.updateDriverSummary("VER")

        coVerify { genAIService.generateDriverSummary(any(), any()) }
        coVerify(exactly = 0) { driverSummaryRepository.createOrUpdateDriverSummary(any()) }
    }

    @Test
    fun `updateDriverSummary should do nothing when driver is not found`() = runTest {
        coEvery { driverRepository.findDriverByAcronym("VER") } returns null

        driverService.updateDriverSummary("VER")

        coVerify(exactly = 0) { genAIService.generateDriverSummary(any(), any()) }
        coVerify(exactly = 0) { driverSummaryRepository.createOrUpdateDriverSummary(any()) }
    }

    @Test
    fun `getDriversInSessions should return only drivers that match both repository and session drivers`() = runTest {
        val sessionKeys = listOf(1, 2)
        val openF1Driver1 = createOpenF1Driver(acronym = "VER")
        val openF1Driver2 = createOpenF1Driver(acronym = "LEC")

        every { openF1Client.getDrivers(sessionKeys = listOf("1", "2")) } returns flowOf(openF1Driver1, openF1Driver2)

        val driver1 = createDriver(id = "1", acronym = "VER")
        val driver2 = createDriver(id = "2", acronym = "HAM") // In repo, but not returned by session API
        val driver3 = createDriver(id = "3", acronym = "LEC")

        coEvery { driverRepository.getDrivers() } returns flowOf(driver1, driver2, driver3)

        val result = driverService.getDriversInSessions(sessionKeys).toList()

        assertEquals(2, result.size)
        assertTrue(result.any { it.acronym == "VER" })
        assertTrue(result.any { it.acronym == "LEC" })
        assertFalse(result.any { it.acronym == "HAM" })
    }

    @Test
    fun `calculateDriverAverageScore should calculate average score correctly using driverAcronym`() = runTest {
        val driver = createDriver(id = "1", acronym = "VER")
        val race1 = createRaceWeekend(raceId = "race1", year = 2023)
        val race2 = createRaceWeekend(raceId = "race2", year = 2023)
        val result1 = createRaceWeekendResult(raceId = "race1", driverId = "1", points = 25.0)
        val result2 = createRaceWeekendResult(raceId = "race2", driverId = "1", points = 18.0)

        coEvery { driverRepository.findDriverByAcronym("VER") } returns driver
        coEvery { raceRepository.getRacesByYear(2023) } returns flowOf(race1, race2)
        coEvery {
            raceWeekendResultRepository.getRaceWeekendResults(
                listOf(
                    "race1",
                    "race2"
                )
            )
        } returns flowOf(result1, result2)

        val averageResult = driverService.calculateDriverAverageScore(year = 2023, driverAcronym = "VER")

        assertEquals("1", averageResult.driverId)
        assertEquals("VER", averageResult.driverAcronym)
        assertEquals(21.5, averageResult.points) // (25 + 18) / 2
    }

    @Test
    fun `calculateDriverAverageScore should calculate average score correctly using driverId`() = runTest {
        val driver = createDriver(id = "1", acronym = "VER")
        val race1 = createRaceWeekend(raceId = "race1", year = 2023)
        val race2 = createRaceWeekend(raceId = "race2", year = 2023)
        val result1 = createRaceWeekendResult(raceId = "race1", driverId = "1", points = 25.0)
        val result2 = createRaceWeekendResult(raceId = "race2", driverId = "1", points = 18.0)

        coEvery { driverRepository.findDriverById("1") } returns driver
        coEvery { raceRepository.getRacesByYear(2023) } returns flowOf(race1, race2)
        coEvery {
            raceWeekendResultRepository.getRaceWeekendResults(
                listOf(
                    "race1",
                    "race2"
                )
            )
        } returns flowOf(result1, result2)

        val averageResult = driverService.calculateDriverAverageScore(year = 2023, driverId = "1")

        assertEquals("1", averageResult.driverId)
        assertEquals("VER", averageResult.driverAcronym)
        assertEquals(21.5, averageResult.points)
    }

    @Test
    fun `calculateDriverAverageScore should throw DriverNotFoundException if driver does not exist`() = runTest {
        coEvery { driverRepository.findDriverByAcronym("VER") } returns null

        val exception = assertThrows<DriverNotFoundException> {
            driverService.calculateDriverAverageScore(year = 2023, driverAcronym = "VER")
        }
        assertEquals("Driver with id=null or acronym=VER", exception.message)
    }

    @Test
    fun `calculateDriverAverageScore should throw IllegalStateException if no races found for the year`() = runTest {
        val driver = createDriver(id = "1", acronym = "VER")
        coEvery { driverRepository.findDriverByAcronym("VER") } returns driver
        coEvery { raceRepository.getRacesByYear(2023) } returns emptyFlow()

        val exception = assertThrows<IllegalStateException> {
            driverService.calculateDriverAverageScore(year = 2023, driverAcronym = "VER")
        }
        assertEquals("No races found for year=2023", exception.message)
    }

    @Test
    fun `calculateDriverAverageScore should return 0 points if no results found for driver in existing race results`() =
        runTest {
            val driver = createDriver(id = "1", acronym = "VER")
            val race1 = createRaceWeekend(raceId = "race1", year = 2023)
            // Result for another driver
            val result1 = createRaceWeekendResult(raceId = "race1", driverId = "2", points = 25.0)

            coEvery { driverRepository.findDriverByAcronym("VER") } returns driver
            coEvery { raceRepository.getRacesByYear(2023) } returns flowOf(race1)
            coEvery { raceWeekendResultRepository.getRaceWeekendResults(listOf("race1")) } returns flowOf(result1)

            val averageResult = driverService.calculateDriverAverageScore(year = 2023, driverAcronym = "VER")

            assertEquals(0.0, averageResult.points)
        }

    @Test
    fun `calculateDriverAverageScore should return NaN points if no race results exist for found races`() = runTest {
        val driver = createDriver(id = "1", acronym = "VER")
        val race1 = createRaceWeekend(raceId = "race1", year = 2023)

        coEvery { driverRepository.findDriverByAcronym("VER") } returns driver
        coEvery { raceRepository.getRacesByYear(2023) } returns flowOf(race1)
        coEvery { raceWeekendResultRepository.getRaceWeekendResults(listOf("race1")) } returns emptyFlow()

        val averageResult = driverService.calculateDriverAverageScore(year = 2023, driverAcronym = "VER")

        assertTrue(averageResult.points.isNaN())
    }

    @Test
    fun `calculateDriverAverageScore should throw IllegalArgumentException if no driverId or acronym provided`() =
        runTest {
            val exception = assertThrows<IllegalArgumentException> {
                driverService.calculateDriverAverageScore(year = 2023)
            }
            assertEquals("Either driverId or driverAcronym must be provided", exception.message)
        }
}