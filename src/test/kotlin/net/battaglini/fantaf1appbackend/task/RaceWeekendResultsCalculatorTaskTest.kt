package net.battaglini.fantaf1appbackend.task

import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toLocalDateTime
import net.battaglini.fantaf1appbackend.client.OpenF1Client
import net.battaglini.fantaf1appbackend.configuration.ChannelConfiguration
import net.battaglini.fantaf1appbackend.configuration.ResultsCalculatorProperties
import net.battaglini.fantaf1appbackend.enums.RaceWeekendSessionType
import net.battaglini.fantaf1appbackend.enums.TaskType
import net.battaglini.fantaf1appbackend.enums.openf1.OpenF1SessionName
import net.battaglini.fantaf1appbackend.enums.openf1.OpenF1SessionType
import net.battaglini.fantaf1appbackend.model.*
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1MeetingResponse
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1SessionResponse
import net.battaglini.fantaf1appbackend.repository.DriverRepository
import net.battaglini.fantaf1appbackend.repository.RaceWeekendResultRepository
import net.battaglini.fantaf1appbackend.service.PracticeResultsService
import net.battaglini.fantaf1appbackend.service.QualifyingResultsService
import net.battaglini.fantaf1appbackend.service.RaceResultsService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class RaceWeekendResultsCalculatorTaskTest {

    @MockK
    lateinit var resultsCalculatorProperties: ResultsCalculatorProperties

    @MockK
    lateinit var practiceResultsService: PracticeResultsService

    @MockK
    lateinit var qualifyingResultsService: QualifyingResultsService

    @MockK
    lateinit var raceResultsService: RaceResultsService

    @MockK
    lateinit var raceWeekendResultRepository: RaceWeekendResultRepository

    @MockK
    lateinit var openF1Client: OpenF1Client

    @MockK
    lateinit var driverRepository: DriverRepository

    @MockK
    lateinit var taskChannel: Channel<ChannelConfiguration.Companion.TaskChannelMessage>

    @MockK
    lateinit var clock: Clock

    @InjectMockKs
    lateinit var task: RaceWeekendResultsCalculatorTask

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        every { resultsCalculatorProperties.enable } returns true
        every { resultsCalculatorProperties.dryRun } returns false
        
        mockkStatic("kotlinx.coroutines.DelayKt")
        coEvery { delay(any<Long>()) } just Runs
        coEvery { delay(any<kotlin.time.Duration>()) } just Runs
    }

    private fun createDriver(acronym: String) = Driver(
        driverId = "id-$acronym",
        driverNumber = 1,
        acronym = acronym,
        driverAvatar = "url",
        initialCost = 10,
        isActive = true,
        name = "Name $acronym",
        teamName = "Team",
        teamColour = "000000"
    )

    private fun createMeeting(key: Int, year: Int, dateEnd: LocalDateTime) = OpenF1MeetingResponse(
        circuitKey = 1,
        circuitImage = "url",
        meetingName = "Meeting $key",
        meetingKey = key,
        meetingOfficialName = "Official Meeting $key",
        countryName = "Italy",
        countryFlag = "IT",
        circuitType = "street",
        year = year,
        dateStart = LocalDateTime(year, 1, 1, 0, 0, 0),
        dateEnd = dateEnd,
        gmtOffset = UtcOffset.ZERO
    )

    private fun createPracticeResult(acronym: String, fastestLap: Duration, raceId: String = "race1") = DriverPracticeResult(
        fastestLap = fastestLap,
        raceId = raceId,
        driverId = "id-$acronym",
        sessionId = "session1",
        sessionType = RaceWeekendSessionType.PRACTICE_COMBINED,
        driverNumber = 1,
        driverAcronym = acronym
    )

    private fun createQualifyingResult(acronym: String, position: Int, raceId: String = "race1") = DriverQualifyingResult(
        fastestLapQ1 = 1.0.toDuration(DurationUnit.SECONDS),
        fastestLapQ2 = 1.0.toDuration(DurationUnit.SECONDS),
        fastestLapQ3 = 1.0.toDuration(DurationUnit.SECONDS),
        finalPosition = position,
        dns = false,
        dnf = false,
        dsq = false,
        raceId = raceId,
        driverId = "id-$acronym",
        sessionId = "session1",
        sessionType = RaceWeekendSessionType.QUALIFYING,
        driverNumber = 1,
        driverAcronym = acronym
    )

    private fun createRaceResult(acronym: String, position: Int, raceId: String = "race1") = DriverRaceResult(
        fastestLap = 1.0.toDuration(DurationUnit.SECONDS),
        startPosition = 1,
        finalPosition = position,
        dns = false,
        dnf = false,
        dsq = false,
        numberOfOvertakes = 0,
        maximumSpeedAtTrap = 300.0,
        raceId = raceId,
        driverId = "id-$acronym",
        sessionId = "session1",
        sessionType = RaceWeekendSessionType.RACE,
        driverNumber = 1,
        driverAcronym = acronym
    )

    @Test
    fun `calculateRaceWeekendResults should calculate mean points correctly`() = runTest {
        val drivers = listOf(createDriver("VER"), createDriver("HAM"))
        coEvery { driverRepository.getDrivers() } returns flowOf(*drivers.toTypedArray())

        every { clock.now() } returns Instant.fromEpochMilliseconds(0)

        val raceWeekend = RaceWeekend(
            raceId = "race1",
            raceName = "Race 1",
            openF1MeetingKey = 1,
            dateStart = clock.now(),
            dateEnd = clock.now(),
            sessions = emptyList(),
            circuitImage = "url",
            countryName = "Italy",
            countryFlag = "IT",
            circuitType = "street",
            dateLineupOpen = clock.now(),
            dateLineupClose = clock.now()
        )

        val practiceResults = listOf(
            createPracticeResult("VER", 1.0.toDuration(DurationUnit.SECONDS)),
            createPracticeResult("HAM", 1.1.toDuration(DurationUnit.SECONDS))
        )
        val qualifyingResults = listOf(
            createQualifyingResult("VER", 1),
            createQualifyingResult("HAM", 2)
        )
        val raceResults = listOf(
            createRaceResult("VER", 1),
            createRaceResult("HAM", 2)
        )

        val result = task.calculateRaceWeekendResults(
            practiceResults,
            qualifyingResults,
            emptyList(),
            raceResults,
            emptyList(),
            raceWeekend
        )

        assertEquals("race1", result.raceId)
        assertEquals(2, result.results.size)
        
        val verResult = result.results.find { it.driverAcronym == "VER" }!!
        assertEquals(20.0, verResult.points)

        val hamResult = result.results.find { it.driverAcronym == "HAM" }!!
        assertEquals(17.0, hamResult.points)
    }

    @Test
    fun `runTask should perform calculation and save result`() = runTest {
        val now = Instant.parse("2024-03-22T12:00:00Z")
        every { clock.now() } returns now
        val year = 2024
        
        val meeting = createMeeting(1, year, LocalDateTime(year, 3, 20, 10, 0, 0))
        
        every { openF1Client.getRaces(meetingKey = any(), year = any(), circuitKey = any()) } returns flowOf(meeting)
        coEvery { raceWeekendResultRepository.findRaceWeekendResult(openF1MeetingKey = 1) } returns null
        
        val sessionResponse = OpenF1SessionResponse(
            meetingKey = 1,
            sessionKey = 101,
            sessionName = OpenF1SessionName.RACE,
            sessionType = OpenF1SessionType.RACE,
            dateStart = LocalDateTime(year, 3, 20, 8, 0, 0),
            dateEnd = LocalDateTime(year, 3, 20, 10, 0, 0),
            gmtOffset = UtcOffset.ZERO,
            year = year
        )
        every { openF1Client.getSessions(1) } returns flowOf(sessionResponse)
        
        val ver = createDriver("VER")
        coEvery { driverRepository.getDrivers() } returns flowOf(ver)
        
        coEvery { practiceResultsService.getDriversResultsForCombinedPractice(any()) } returns flowOf(createPracticeResult("VER", 1.0.toDuration(DurationUnit.SECONDS), "dummy"))
        coEvery { qualifyingResultsService.getDriversResultsForQualifying(any(), false) } returns flowOf(createQualifyingResult("VER", 1, "dummy"))
        coEvery { qualifyingResultsService.getDriversResultsForQualifying(any(), true) } returns flowOf()
        coEvery { raceResultsService.getResultsForRace(any(), false) } returns flowOf(createRaceResult("VER", 1, "dummy"))
        coEvery { raceResultsService.getResultsForRace(any(), true) } returns flowOf()
        
        coEvery { raceWeekendResultRepository.saveRaceWeekendResult(any()) } just Runs
        coEvery { taskChannel.send(any()) } just Runs

        task.runTask()
        
        coVerify(timeout = 5000) { raceWeekendResultRepository.saveRaceWeekendResult(any()) }
        coVerify(timeout = 5000) { taskChannel.send(match { it.taskType == TaskType.RACE_WEEKEND_RESULTS_CALCULATION_COMPLETED }) }
    }

    @Test
    fun `runTask should skip if disabled`() = runTest {
        every { resultsCalculatorProperties.enable } returns false
        
        task.runTask()
        
        coVerify(exactly = 0) { openF1Client.getRaces(any(), any(), any()) }
    }

    @Test
    fun `runTask should stop if no recent race weekend found`() = runTest {
        val now = Instant.parse("2024-03-22T12:00:00Z")
        every { clock.now() } returns now
        val year = 2024
        
        val meeting = createMeeting(1, year, LocalDateTime(year, 3, 10, 12, 0, 0))
        
        every { openF1Client.getRaces(meetingKey = any(), year = any(), circuitKey = any()) } returns flowOf(meeting)
        
        task.runTask()
        
        coVerify(exactly = 0) { raceWeekendResultRepository.findRaceWeekendResult(any(), any()) }
    }

    @Test
    fun `runTask should stop if results already exist`() = runTest {
        val now = Instant.parse("2024-03-22T12:00:00Z")
        every { clock.now() } returns now
        val year = 2024
        val meeting = createMeeting(1, year, LocalDateTime(year, 3, 20, 10, 0, 0))
        
        every { openF1Client.getRaces(meetingKey = any(), year = any(), circuitKey = any()) } returns flowOf(meeting)
        coEvery { raceWeekendResultRepository.findRaceWeekendResult(openF1MeetingKey = 1) } returns mockk()
        
        task.runTask()
        
        coVerify(exactly = 0) { openF1Client.getSessions(any()) }
    }

    @Test
    fun `runTask should not save results if dryRun is enabled`() = runTest {
        every { resultsCalculatorProperties.dryRun } returns true
        val now = Instant.parse("2024-03-22T12:00:00Z")
        every { clock.now() } returns now
        val year = 2024
        val meeting = createMeeting(1, year, LocalDateTime(year, 3, 20, 10, 0, 0))
        
        every { openF1Client.getRaces(any(), any(), any()) } returns flowOf(meeting)
        coEvery { raceWeekendResultRepository.findRaceWeekendResult(openF1MeetingKey = 1) } returns null
        every { openF1Client.getSessions(1) } returns flowOf()
        
        val ver = createDriver("VER")
        coEvery { driverRepository.getDrivers() } returns flowOf(ver)
        
        coEvery { practiceResultsService.getDriversResultsForCombinedPractice(any()) } returns flowOf(createPracticeResult("VER", 1.0.toDuration(DurationUnit.SECONDS), "dummy"))
        coEvery { qualifyingResultsService.getDriversResultsForQualifying(any(), false) } returns flowOf(createQualifyingResult("VER", 1, "dummy"))
        coEvery { raceResultsService.getResultsForRace(any(), false) } returns flowOf(createRaceResult("VER", 1, "dummy"))
        
        task.runTask()
        
        coVerify(exactly = 0) { raceWeekendResultRepository.saveRaceWeekendResult(any()) }
        coVerify(exactly = 0) { taskChannel.send(any()) }
    }

    @Test
    fun `runTask should stop if minimum set of results is not available`() = runTest {
        val now = Instant.parse("2024-03-22T12:00:00Z")
        every { clock.now() } returns now
        val year = 2024
        val meeting = createMeeting(1, year, LocalDateTime(year, 3, 20, 10, 0, 0))
        
        every { openF1Client.getRaces(any(), any(), any()) } returns flowOf(meeting)
        coEvery { raceWeekendResultRepository.findRaceWeekendResult(openF1MeetingKey = 1) } returns null
        every { openF1Client.getSessions(1) } returns flowOf()
        
        // Missing race results
        coEvery { practiceResultsService.getDriversResultsForCombinedPractice(any()) } returns flowOf(createPracticeResult("VER", 1.0.toDuration(DurationUnit.SECONDS)))
        coEvery { qualifyingResultsService.getDriversResultsForQualifying(any(), false) } returns flowOf(createQualifyingResult("VER", 1))
        coEvery { raceResultsService.getResultsForRace(any(), any()) } returns emptyFlow()
        
        task.runTask()
        
        coVerify(exactly = 0) { raceWeekendResultRepository.saveRaceWeekendResult(any()) }
    }
}
