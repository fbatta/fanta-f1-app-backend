package net.battaglini.fantaf1appbackend.service

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.battaglini.fantaf1appbackend.enums.RaceWeekendSessionType
import net.battaglini.fantaf1appbackend.model.*
import net.battaglini.fantaf1appbackend.repository.DriverRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@ExtendWith(MockKExtension::class)
class RaceWeekendResultsCalculatorTest {

    @MockK
    lateinit var driverRepository: DriverRepository

    @MockK
    lateinit var clock: Clock

    @InjectMockKs
    lateinit var calculator: RaceWeekendResultsCalculator

    private val now = Instant.parse("2026-06-15T12:00:00Z")

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        every { clock.now() } returns now
    }

    private fun createPracticeResult(
        acronym: String,
        fastestLapSeconds: Double,
        driverId: String = "id-$acronym",
        driverNumber: Int = 1
    ) = DriverPracticeResult(
        fastestLap = fastestLapSeconds.seconds,
        raceId = "race1",
        driverId = driverId,
        sessionId = "sess-practice",
        sessionType = RaceWeekendSessionType.PRACTICE_COMBINED,
        driverNumber = driverNumber,
        driverAcronym = acronym
    )

    private fun createQualifyingResult(
        acronym: String,
        finalPosition: Int?,
        sessionType: RaceWeekendSessionType = RaceWeekendSessionType.QUALIFYING,
        driverId: String = "id-$acronym",
        driverNumber: Int = 1
    ) = DriverQualifyingResult(
        fastestLapQ1 = 90.seconds,
        fastestLapQ2 = 89.seconds,
        fastestLapQ3 = 88.seconds,
        finalPosition = finalPosition,
        dns = false,
        dnf = false,
        dsq = false,
        raceId = "race1",
        driverId = driverId,
        sessionId = "sess-qualifying",
        sessionType = sessionType,
        driverNumber = driverNumber,
        driverAcronym = acronym
    )

    private fun createRaceResult(
        acronym: String,
        finalPosition: Int?,
        sessionType: RaceWeekendSessionType = RaceWeekendSessionType.RACE,
        driverId: String = "id-$acronym",
        driverNumber: Int = 1
    ) = DriverRaceResult(
        fastestLap = 90.seconds,
        startPosition = 1,
        finalPosition = finalPosition,
        dns = false,
        dnf = false,
        dsq = false,
        numberOfOvertakes = 2,
        maximumSpeedAtTrap = 320.0,
        raceId = "race1",
        driverId = driverId,
        sessionId = "sess-race",
        sessionType = sessionType,
        driverNumber = driverNumber,
        driverAcronym = acronym
    )

    @Test
    fun `calculateRaceWeekendResults should calculate points correctly for all 5 sessions`() = runTest {
        val drivers = listOf(
            TestFactories.createDriver(driverId = "id-VER", driverNumber = 1, acronym = "VER"),
            TestFactories.createDriver(driverId = "id-HAM", driverNumber = 44, acronym = "HAM"),
            TestFactories.createDriver(driverId = "id-LEC", driverNumber = 16, acronym = "LEC")
        )
        coEvery { driverRepository.getDrivers() } returns flowOf(*drivers.toTypedArray())

        val raceWeekend = TestFactories.createRaceWeekend(raceId = "race1", raceName = "Monaco Grand Prix", openF1MeetingKey = 100)

        // Practice sorting: VER (85.0s -> 20.0), HAM (85.5s -> 17.0), LEC (86.0s -> 15.0)
        val practiceResults = listOf(
            createPracticeResult("LEC", 86.0),
            createPracticeResult("VER", 85.0),
            createPracticeResult("HAM", 85.5)
        )

        // Qualifying sorting: LEC (1st -> 20.0), VER (2nd -> 17.0), HAM (3rd -> 15.0)
        val qualifyingResults = listOf(
            createQualifyingResult("VER", 2),
            createQualifyingResult("LEC", 1),
            createQualifyingResult("HAM", 3)
        )

        // Sprint Qualifying sorting: HAM (1st -> 20.0), LEC (2nd -> 17.0), VER (3rd -> 15.0)
        val sprintQualifyingResults = listOf(
            createQualifyingResult("LEC", 2, RaceWeekendSessionType.SPRINT_QUALIFYING),
            createQualifyingResult("HAM", 1, RaceWeekendSessionType.SPRINT_QUALIFYING),
            createQualifyingResult("VER", 3, RaceWeekendSessionType.SPRINT_QUALIFYING)
        )

        // Sprint Race sorting: VER (1st -> 20.0), HAM (2nd -> 17.0), LEC (3rd -> 15.0)
        val sprintRaceResults = listOf(
            createRaceResult("VER", 1, RaceWeekendSessionType.SPRINT_RACE),
            createRaceResult("HAM", 2, RaceWeekendSessionType.SPRINT_RACE),
            createRaceResult("LEC", 3, RaceWeekendSessionType.SPRINT_RACE)
        )

        // Race sorting: HAM (1st -> 20.0), VER (2nd -> 17.0), LEC (3rd -> 15.0)
        val raceResults = listOf(
            createRaceResult("HAM", 1),
            createRaceResult("LEC", 3),
            createRaceResult("VER", 2)
        )

        val result = calculator.calculateRaceWeekendResults(
            driverPracticeResults = practiceResults,
            driverQualifyingResults = qualifyingResults,
            driverSprintQualifyingResults = sprintQualifyingResults,
            driverRaceResults = raceResults,
            driverSprintRaceResults = sprintRaceResults,
            raceWeekend = raceWeekend
        )

        assertEquals("race1", result.raceId)
        assertEquals("Monaco Grand Prix", result.raceName)
        assertEquals(100, result.openF1MeetingKey)
        assertEquals(now, result.createdAt)
        assertEquals(now, result.updatedAt)
        assertEquals(1, result.version)

        val resultsMap = result.results.associateBy { it.driverAcronym }
        assertEquals(3, resultsMap.size)

        // VER: (20 + 17 + 15 + 20 + 17) / 5 = 17.8
        assertEquals(17.8, resultsMap["VER"]?.points)
        assertEquals(1, resultsMap["VER"]?.driverNumber)
        assertEquals("id-VER", resultsMap["VER"]?.driverId)

        // HAM: (17 + 15 + 20 + 17 + 20) / 5 = 17.8
        assertEquals(17.8, resultsMap["HAM"]?.points)
        assertEquals(44, resultsMap["HAM"]?.driverNumber)
        assertEquals("id-HAM", resultsMap["HAM"]?.driverId)

        // LEC: (15 + 20 + 17 + 15 + 15) / 5 = 16.4
        assertEquals(16.4, resultsMap["LEC"]?.points)
        assertEquals(16, resultsMap["LEC"]?.driverNumber)
        assertEquals("id-LEC", resultsMap["LEC"]?.driverId)
    }

    @Test
    fun `calculateRaceWeekendResults should calculate points with partial sessions`() = runTest {
        val drivers = listOf(
            TestFactories.createDriver(driverId = "id-VER", driverNumber = 1, acronym = "VER"),
            TestFactories.createDriver(driverId = "id-HAM", driverNumber = 44, acronym = "HAM"),
            TestFactories.createDriver(driverId = "id-LEC", driverNumber = 16, acronym = "LEC")
        )
        coEvery { driverRepository.getDrivers() } returns flowOf(*drivers.toTypedArray())

        val raceWeekend = TestFactories.createRaceWeekend(raceId = "race1")

        // Practice: VER (20.0), HAM (17.0), LEC (15.0)
        val practiceResults = listOf(
            createPracticeResult("LEC", 86.0),
            createPracticeResult("VER", 85.0),
            createPracticeResult("HAM", 85.5)
        )

        // Qualifying: LEC (20.0), VER (17.0), HAM (15.0)
        val qualifyingResults = listOf(
            createQualifyingResult("VER", 2),
            createQualifyingResult("LEC", 1),
            createQualifyingResult("HAM", 3)
        )

        // Race: HAM (20.0), VER (17.0), LEC (15.0)
        val raceResults = listOf(
            createRaceResult("HAM", 1),
            createRaceResult("LEC", 3),
            createRaceResult("VER", 2)
        )

        val result = calculator.calculateRaceWeekendResults(
            driverPracticeResults = practiceResults,
            driverQualifyingResults = qualifyingResults,
            driverSprintQualifyingResults = emptyList(),
            driverRaceResults = raceResults,
            driverSprintRaceResults = emptyList(),
            raceWeekend = raceWeekend
        )

        val resultsMap = result.results.associateBy { it.driverAcronym }

        // VER: (20 + 17 + 17) / 3 = 18.0
        assertEquals(18.0, resultsMap["VER"]?.points)

        // HAM: (17 + 15 + 20) / 3 = 17.333 -> 17.3
        assertEquals(17.3, resultsMap["HAM"]?.points)

        // LEC: (15 + 20 + 15) / 3 = 16.666 -> 16.7
        assertEquals(16.7, resultsMap["LEC"]?.points)
    }

    @Test
    fun `calculateRaceWeekendResults should handle drivers with missing results`() = runTest {
        val drivers = listOf(
            TestFactories.createDriver(driverId = "id-VER", driverNumber = 1, acronym = "VER"),
            TestFactories.createDriver(driverId = "id-HAM", driverNumber = 44, acronym = "HAM"),
            TestFactories.createDriver(driverId = "id-LEC", driverNumber = 16, acronym = "LEC")
        )
        coEvery { driverRepository.getDrivers() } returns flowOf(*drivers.toTypedArray())

        val raceWeekend = TestFactories.createRaceWeekend(raceId = "race1")

        // VER and HAM have practice results, LEC is missing
        val practiceResults = listOf(
            createPracticeResult("VER", 85.0),
            createPracticeResult("HAM", 85.5)
        )

        // VER and LEC have qualifying results, HAM is missing
        val qualifyingResults = listOf(
            createQualifyingResult("VER", 1),
            createQualifyingResult("LEC", 2)
        )

        val result = calculator.calculateRaceWeekendResults(
            driverPracticeResults = practiceResults,
            driverQualifyingResults = qualifyingResults,
            driverSprintQualifyingResults = emptyList(),
            driverRaceResults = emptyList(),
            driverSprintRaceResults = emptyList(),
            raceWeekend = raceWeekend
        )

        val resultsMap = result.results.associateBy { it.driverAcronym }

        // VER: Practice (20.0), Qualifying (20.0) -> (20 + 20) / 2 = 20.0
        assertEquals(20.0, resultsMap["VER"]?.points)

        // HAM: Practice (17.0), Qualifying (missing) -> (17.0) / 1 = 17.0
        assertEquals(17.0, resultsMap["HAM"]?.points)

        // LEC: Practice (missing), Qualifying (17.0) -> (17.0) / 1 = 17.0
        assertEquals(17.0, resultsMap["LEC"]?.points)
    }

    @Test
    fun `calculateRaceWeekendResults should return 0 points for a driver with no results at all`() = runTest {
        val drivers = listOf(
            TestFactories.createDriver(driverId = "id-VER", driverNumber = 1, acronym = "VER"),
            TestFactories.createDriver(driverId = "id-HAM", driverNumber = 44, acronym = "HAM")
        )
        coEvery { driverRepository.getDrivers() } returns flowOf(*drivers.toTypedArray())

        val raceWeekend = TestFactories.createRaceWeekend(raceId = "race1")

        // Only VER has practice results
        val practiceResults = listOf(
            createPracticeResult("VER", 85.0)
        )

        val result = calculator.calculateRaceWeekendResults(
            driverPracticeResults = practiceResults,
            driverQualifyingResults = emptyList(),
            driverSprintQualifyingResults = emptyList(),
            driverRaceResults = emptyList(),
            driverSprintRaceResults = emptyList(),
            raceWeekend = raceWeekend
        )

        val resultsMap = result.results.associateBy { it.driverAcronym }

        // VER has results -> 20.0
        assertEquals(20.0, resultsMap["VER"]?.points)

        // HAM has no results -> 0.0
        assertEquals(0.0, resultsMap["HAM"]?.points)
    }

    @Test
    fun `calculateRaceWeekendResults should place null finalPosition drivers at the end`() = runTest {
        val drivers = listOf(
            TestFactories.createDriver(driverId = "id-VER", acronym = "VER"),
            TestFactories.createDriver(driverId = "id-HAM", acronym = "HAM"),
            TestFactories.createDriver(driverId = "id-LEC", acronym = "LEC")
        )
        coEvery { driverRepository.getDrivers() } returns flowOf(*drivers.toTypedArray())

        val raceWeekend = TestFactories.createRaceWeekend(raceId = "race1")

        // HAM has null final position, so they should be sorted last
        // VER is 1st, LEC is 2nd, HAM is null
        val qualifyingResults = listOf(
            createQualifyingResult("HAM", null),
            createQualifyingResult("VER", 1),
            createQualifyingResult("LEC", 2)
        )

        val result = calculator.calculateRaceWeekendResults(
            driverPracticeResults = emptyList(),
            driverQualifyingResults = qualifyingResults,
            driverSprintQualifyingResults = emptyList(),
            driverRaceResults = emptyList(),
            driverSprintRaceResults = emptyList(),
            raceWeekend = raceWeekend
        )

        val resultsMap = result.results.associateBy { it.driverAcronym }

        // VER: Index 0 -> 20.0
        assertEquals(20.0, resultsMap["VER"]?.points)

        // LEC: Index 1 -> 17.0
        assertEquals(17.0, resultsMap["LEC"]?.points)

        // HAM: Index 2 -> 15.0
        assertEquals(15.0, resultsMap["HAM"]?.points)
    }

    @Test
    fun `calculateRaceWeekendResults should assign 0 points to drivers beyond 15th position`() = runTest {
        // Create 16 drivers
        val driverAcronyms = (1..16).map { "D$it" }
        val drivers = driverAcronyms.map { acronym ->
            TestFactories.createDriver(driverId = "id-$acronym", acronym = acronym)
        }
        coEvery { driverRepository.getDrivers() } returns flowOf(*drivers.toTypedArray())

        val raceWeekend = TestFactories.createRaceWeekend(raceId = "race1")

        // Create 16 race results, sorted D1 -> D16
        val raceResults = driverAcronyms.mapIndexed { index, acronym ->
            createRaceResult(acronym, finalPosition = index + 1)
        }

        val result = calculator.calculateRaceWeekendResults(
            driverPracticeResults = emptyList(),
            driverQualifyingResults = emptyList(),
            driverSprintQualifyingResults = emptyList(),
            driverRaceResults = raceResults,
            driverSprintRaceResults = emptyList(),
            raceWeekend = raceWeekend
        )

        val resultsMap = result.results.associateBy { it.driverAcronym }

        // D1 (Index 0): 20.0
        assertEquals(20.0, resultsMap["D1"]?.points)
        // D2 (Index 1): 17.0
        assertEquals(17.0, resultsMap["D2"]?.points)
        // D15 (Index 14): 1.0
        assertEquals(1.0, resultsMap["D15"]?.points)
        // D16 (Index 15): 0.0
        assertEquals(0.0, resultsMap["D16"]?.points)
    }
}
