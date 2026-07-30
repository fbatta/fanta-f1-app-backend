package net.battaglini.fantaf1appbackend.task

import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import com.google.auth.oauth2.GoogleCredentials
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
import net.battaglini.fantaf1appbackend.service.RaceWeekendResultsCalculator
import net.battaglini.fantaf1appbackend.service.RaceWeekendService
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import com.ninjasquad.springmockk.MockkBean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.test.context.TestPropertySource
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.time.toDuration
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class)
@SpringBootTest
@TestPropertySource(properties = ["results-calculator.enable=false"])
class RaceWeekendResultsCalculatorIntegrationTest {

    @MockkBean
    lateinit var openF1Client: OpenF1Client

    @MockkBean
    lateinit var driverRepository: DriverRepository

    @MockkBean
    lateinit var raceWeekendResultRepository: RaceWeekendResultRepository

    @MockkBean
    lateinit var raceWeekendService: RaceWeekendService

    @MockkBean
    lateinit var raceWeekendResultsCalculator: RaceWeekendResultsCalculator

    @MockkBean(relaxed = true)
    lateinit var googleCredentials: GoogleCredentials

    @Autowired
    lateinit var task: RaceWeekendResultsCalculatorTask

    @MockkBean(relaxed = true)
    lateinit var resultsCalculatorProperties: ResultsCalculatorProperties

    @MockkBean(relaxed = true)
    lateinit var pricingTask: PricingTask

    @MockkBean(relaxed = true)
    lateinit var teamsResultsCalculatorTask: TeamsResultsCalculatorTask

    @Autowired
    lateinit var taskChannel: Channel<ChannelConfiguration.Companion.TaskChannelMessage>

    @MockkBean
    lateinit var clock: Clock

    @MockkBean(relaxed = true)
    lateinit var timeZone: TimeZone

    private val now = Instant.parse("2024-03-22T12:00:00Z")

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        every { clock.now() } returns now
        every { resultsCalculatorProperties.enable } returns true
        every { resultsCalculatorProperties.dryRun } returns false

        while (taskChannel.tryReceive().isSuccess) {
            // drain the channel to ensure tests start with an empty channel
        }

        val meeting = createMeeting(1, 2024, LocalDateTime(2024, 3, 20, 10, 0, 0))
        coEvery { openF1Client.getRaces(any(), any(), any()) } returns flowOf(meeting)
        coEvery { raceWeekendResultRepository.findRaceWeekendResult(openF1MeetingKey = 1) } returns null
    }

    @Test
    fun `runTask should calculate and save race weekend results`() = runTest {
        val year = 2024
        val meeting = createMeeting(1, year, LocalDateTime(year, 3, 20, 10, 0, 0))

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

        coEvery { openF1Client.getSessions(1) } returns flowOf(sessionResponse)


        val combinedResults = CombinedDriversRaceWeekendResults(
            raceId = "race1",
            combinedPracticeResults = listOf(
                createPracticeResult("VER", 1.0.seconds, "dummy"),
                createPracticeResult("HAM", 1.1.seconds, "dummy")
            ),
            qualifyingResults = listOf(
                createQualifyingResult("VER", 1, "dummy"),
                createQualifyingResult("HAM", 2, "dummy")
            ),
            sprintQualifyingResults = emptyList(),
            raceResults = listOf(
                createRaceResult("VER", 1, "dummy"),
                createRaceResult("HAM", 2, "dummy")
            ),
            sprintRaceResults = emptyList()
        )

        coEvery { raceWeekendService.fetchDriversResults(any()) } returns combinedResults

        val expectedResult = RaceWeekendResult(
            raceId = "race1",
            raceName = "Meeting 1",
            openF1MeetingKey = 1,
            createdAt = now,
            updatedAt = now,
            version = 1,
            results = listOf(
                RaceWeekendResult.Companion.Result("id-VER", 1, "VER", 20.0),
                RaceWeekendResult.Companion.Result("id-HAM", 44, "HAM", 17.0)
            )
        )

        coEvery { raceWeekendResultsCalculator.calculateRaceWeekendResults(any(), any(), any(), any(), any(), any()) } returns expectedResult
        coEvery { raceWeekendResultRepository.saveRaceWeekendResult(any()) } just Runs

        task.runTask()

        coVerify(timeout = 5000) { openF1Client.getRaces(any(), any(), any()) }
        coVerify(timeout = 5000) { openF1Client.getSessions(1) }
        coVerify(timeout = 5000) { raceWeekendService.fetchDriversResults(any()) }
        coVerify(timeout = 5000) { raceWeekendResultsCalculator.calculateRaceWeekendResults(any(), any(), any(), any(), any(), any()) }

        coVerify(timeout = 5000) { raceWeekendResultRepository.saveRaceWeekendResult(match {
            it.raceId == "race1" &&
            it.raceName == "Meeting 1" &&
            it.openF1MeetingKey == 1 &&
            it.createdAt == now &&
            it.updatedAt == now &&
            it.version == 1 &&
            it.results.size == 2
        }) }

        val receivedMessage = taskChannel.receiveCatching()
        assertTrue(receivedMessage.isSuccess, "Expected a channel message but got none")
        assertEquals(TaskType.UPDATE_DRIVERS_PRICING, receivedMessage.getOrThrow().taskType)
    }

    @Test
    fun `runTask should skip when disabled`() = runTest {
        every { resultsCalculatorProperties.enable } returns false

        task.runTask()

        coVerify(exactly = 0, timeout = 5000) { openF1Client.getRaces(any(), any(), any()) }
    }

    @Test
    fun `runTask should not save when dryRun is enabled`() = runTest {
        every { resultsCalculatorProperties.dryRun } returns true

        val year = 2024
        val meeting = createMeeting(1, year, LocalDateTime(year, 3, 20, 10, 0, 0))
        coEvery { openF1Client.getSessions(1) } returns flowOf(OpenF1SessionResponse(
            meetingKey = 1,
            sessionKey = 101,
            sessionName = OpenF1SessionName.RACE,
            sessionType = OpenF1SessionType.RACE,
            dateStart = LocalDateTime(year, 3, 20, 8, 0, 0),
            dateEnd = LocalDateTime(year, 3, 20, 10, 0, 0),
            gmtOffset = UtcOffset.ZERO,
            year = year
        ))

        val combinedResults = CombinedDriversRaceWeekendResults(
            raceId = "race1",
            combinedPracticeResults = emptyList(),
            qualifyingResults = emptyList(),
            sprintQualifyingResults = emptyList(),
            raceResults = emptyList(),
            sprintRaceResults = emptyList()
        )
        coEvery { raceWeekendService.fetchDriversResults(any()) } returns combinedResults

        val expectedResult = RaceWeekendResult(
            raceId = "race1",
            raceName = "Meeting 1",
            openF1MeetingKey = 1,
            createdAt = now,
            updatedAt = now,
            version = 1,
            results = emptyList()
        )
        coEvery { raceWeekendResultsCalculator.calculateRaceWeekendResults(any(), any(), any(), any(), any(), any()) } returns expectedResult

        task.runTask()

        coVerify(exactly = 0, timeout = 5000) { raceWeekendResultRepository.saveRaceWeekendResult(any()) }
        assertTrue(taskChannel.isEmpty, "Expected no channel messages but got some")
    }

    @Test
    fun `runTask should skip if results already exist`() = runTest {
        val year = 2024
        val meeting = createMeeting(1, year, LocalDateTime(year, 3, 20, 10, 0, 0))
        coEvery { openF1Client.getSessions(1) } returns flowOf(OpenF1SessionResponse(
            meetingKey = 1,
            sessionKey = 101,
            sessionName = OpenF1SessionName.RACE,
            sessionType = OpenF1SessionType.RACE,
            dateStart = LocalDateTime(year, 3, 20, 8, 0, 0),
            dateEnd = LocalDateTime(year, 3, 20, 10, 0, 0),
            gmtOffset = UtcOffset.ZERO,
            year = year
        ))
        coEvery { raceWeekendResultRepository.findRaceWeekendResult(openF1MeetingKey = 1) } returns mockk()

        task.runTask()

        coVerify(exactly = 0, timeout = 5000) { openF1Client.getSessions(any()) }
        coVerify(exactly = 0, timeout = 5000) { raceWeekendService.fetchDriversResults(any()) }
        coVerify(exactly = 0, timeout = 5000) { raceWeekendResultsCalculator.calculateRaceWeekendResults(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `runTask should stop if no recent race weekend found`() = runTest {
        val year = 2024
        val meeting = createMeeting(1, year, LocalDateTime(year, 3, 10, 12, 0, 0))
        coEvery { openF1Client.getRaces(any(), any(), any()) } returns flowOf(meeting)

        task.runTask()

        coVerify(exactly = 0, timeout = 5000) { raceWeekendResultRepository.findRaceWeekendResult(any(), any()) }
    }

    @Test
    fun `runTask should stop if minimum set of results is not available`() = runTest {
        val year = 2024
        val meeting = createMeeting(1, year, LocalDateTime(year, 3, 20, 10, 0, 0))
        coEvery { openF1Client.getSessions(1) } returns flowOf(OpenF1SessionResponse(
            meetingKey = 1,
            sessionKey = 101,
            sessionName = OpenF1SessionName.RACE,
            sessionType = OpenF1SessionType.RACE,
            dateStart = LocalDateTime(year, 3, 20, 8, 0, 0),
            dateEnd = LocalDateTime(year, 3, 20, 10, 0, 0),
            gmtOffset = UtcOffset.ZERO,
            year = year
        ))
        coEvery { raceWeekendService.fetchDriversResults(any()) } returns null

        task.runTask()

        coVerify(exactly = 0, timeout = 5000) { raceWeekendResultRepository.saveRaceWeekendResult(any()) }
    }

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

    private fun createPracticeResult(acronym: String, fastestLap: kotlin.time.Duration, raceId: String = "race1") =
        DriverPracticeResult(
            fastestLap = fastestLap,
            raceId = raceId,
            driverId = "id-$acronym",
            sessionId = "session1",
            sessionType = RaceWeekendSessionType.PRACTICE_COMBINED,
            driverNumber = 1,
            driverAcronym = acronym
        )

    private fun createQualifyingResult(acronym: String, position: Int, raceId: String = "race1") =
        DriverQualifyingResult(
            fastestLapQ1 = 1.0.seconds,
            fastestLapQ2 = 1.0.seconds,
            fastestLapQ3 = 1.0.seconds,
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
        fastestLap = 1.0.seconds,
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
}
