package net.battaglini.fantaf1appbackend.service

import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import net.battaglini.fantaf1appbackend.client.OpenF1Client
import net.battaglini.fantaf1appbackend.enums.RaceWeekendSessionType
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1SessionResultResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Duration.Companion.seconds

@ExtendWith(MockKExtension::class)
class PracticeResultsServiceTest {

    @MockK
    lateinit var openF1Client: OpenF1Client

    @MockK
    lateinit var driverService: DriverService

    @InjectMockKs
    lateinit var practiceResultsService: PracticeResultsServiceImpl

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    private fun createOpenF1Result(
        driverNumber: Int = 1,
        duration: Double? = 85.5
    ) = OpenF1SessionResultResponse(
        position = 1,
        driverNumber = driverNumber,
        numberOfLaps = 57,
        dnf = false,
        dns = false,
        dsq = false,
        gapToLeader = null,
        duration = duration,
        meetingKey = 100,
        sessionKey = 1001
    )

    @Test
    fun `getDriversResultsForCombinedPractice should compute fastest lap across P1 P2 P3 sessions`() = runTest {
        val raceWeekend = TestFactories.createRaceWeekend(
            sessions = listOf(
                TestFactories.createSession(sessionId = "sess1", openF1SessionKey = 1001, sessionType = RaceWeekendSessionType.PRACTICE_1),
                TestFactories.createSession(sessionId = "sess2", openF1SessionKey = 1002, sessionType = RaceWeekendSessionType.PRACTICE_2),
                TestFactories.createSession(sessionId = "sess3", openF1SessionKey = 1003, sessionType = RaceWeekendSessionType.PRACTICE_3)
            )
        )
        val driver = TestFactories.createDriver(driverNumber = 1)

        // Results from all 3 practice sessions
        val results = listOf(
            createOpenF1Result(driverNumber = 1, duration = 88.0),
            createOpenF1Result(driverNumber = 1, duration = 85.5),
            createOpenF1Result(driverNumber = 1, duration = 87.2)
        )

        coEvery { openF1Client.getResults(meetingKey = 100, sessionKeys = listOf("1001", "1002", "1003")) } returns flowOf(
            results[0], results[1], results[2]
        )
        coEvery { driverService.getDriversInSessions(listOf(1001, 1002, 1003)) } returns flowOf(driver)

        val result = practiceResultsService.getDriversResultsForCombinedPractice(raceWeekend).toList()

        assertEquals(1, result.size)
        assertEquals("race1", result[0].raceId)
        assertEquals("driver1", result[0].driverId)
        assertEquals(RaceWeekendSessionType.PRACTICE_COMBINED, result[0].sessionType)
        assertEquals(1, result[0].driverNumber)
        assertEquals("VER", result[0].driverAcronym)
        // Fastest lap should be 85.5 seconds (minimum of 88.0, 85.5, 87.2)
        assertEquals(85.5.seconds, result[0].fastestLap)
        coVerify { openF1Client.getResults(meetingKey = 100, sessionKeys = listOf("1001", "1002", "1003")) }
        coVerify { driverService.getDriversInSessions(listOf(1001, 1002, 1003)) }
    }

    @Test
    fun `getDriversResultsForCombinedPractice should return emptyFlow when no practice sessions`() = runTest {
        val raceWeekend = TestFactories.createRaceWeekend(sessions = emptyList())

        val result = practiceResultsService.getDriversResultsForCombinedPractice(raceWeekend).toList()

        assertTrue(result.isEmpty())
        verify(exactly = 0) { openF1Client.getResults(any(), any()) }
        coVerify(exactly = 0) { driverService.getDriversInSessions(any()) }
    }

    @Test
    fun `getDriversResultsForCombinedPractice should use default fastest lap when driver has no results`() = runTest {
        val raceWeekend = TestFactories.createRaceWeekend(
            sessions = listOf(
                TestFactories.createSession(sessionId = "sess1", openF1SessionKey = 1001, sessionType = RaceWeekendSessionType.PRACTICE_1)
            )
        )
        val driver = TestFactories.createDriver(driverNumber = 16) // Driver number not in results

        coEvery { openF1Client.getResults(meetingKey = 100, sessionKeys = listOf("1001")) } returns flowOf(
            createOpenF1Result(driverNumber = 1, duration = 85.0)
        )
        coEvery { driverService.getDriversInSessions(listOf(1001)) } returns flowOf(driver)

        val result = practiceResultsService.getDriversResultsForCombinedPractice(raceWeekend).toList()

        assertEquals(1, result.size)
        // Default fastest lap is 999_999.9 seconds
        assertEquals(999_999.9.seconds, result[0].fastestLap)
    }

    @Test
    fun `getDriversResultsForCombinedPractice should find global fastest lap across all practice sessions`() = runTest {
        val raceWeekend = TestFactories.createRaceWeekend(
            sessions = listOf(
                TestFactories.createSession(sessionId = "sess1", openF1SessionKey = 1001, sessionType = RaceWeekendSessionType.PRACTICE_1),
                TestFactories.createSession(sessionId = "sess2", openF1SessionKey = 1002, sessionType = RaceWeekendSessionType.PRACTICE_2)
            )
        )
        val driver = TestFactories.createDriver(driverNumber = 4)

        // Driver has laps in P2 (1002) with faster time than P1 (1001)
        val results = listOf(
            createOpenF1Result(driverNumber = 4, duration = 90.0),  // P1
            createOpenF1Result(driverNumber = 4, duration = 84.3)   // P2 - fastest
        )

        coEvery { openF1Client.getResults(meetingKey = 100, sessionKeys = listOf("1001", "1002")) } returns flowOf(
            results[0], results[1]
        )
        coEvery { driverService.getDriversInSessions(listOf(1001, 1002)) } returns flowOf(driver)

        val result = practiceResultsService.getDriversResultsForCombinedPractice(raceWeekend).toList()

        assertEquals(1, result.size)
        assertEquals(84.3.seconds, result[0].fastestLap)
    }

    @Test
    fun `getDriversResultsForCombinedPractice should ignore null duration laps`() = runTest {
        val raceWeekend = TestFactories.createRaceWeekend(
            sessions = listOf(
                TestFactories.createSession(sessionId = "sess1", openF1SessionKey = 1001, sessionType = RaceWeekendSessionType.PRACTICE_1)
            )
        )
        val driver = TestFactories.createDriver(driverNumber = 1)

        // All laps have null duration — should use default 999_999.9
        val results = listOf(
            createOpenF1Result(driverNumber = 1, duration = null),
            createOpenF1Result(driverNumber = 1, duration = null)
        )

        coEvery { openF1Client.getResults(meetingKey = 100, sessionKeys = listOf("1001")) } returns flowOf(
            results[0], results[1]
        )
        coEvery { driverService.getDriversInSessions(listOf(1001)) } returns flowOf(driver)

        val result = practiceResultsService.getDriversResultsForCombinedPractice(raceWeekend).toList()

        assertEquals(1, result.size)
        assertEquals(999_999.9.seconds, result[0].fastestLap)
    }

    @Test
    fun `getDriversResultsForCombinedPractice should compute separate fastest laps for multiple drivers`() = runTest {
        val raceWeekend = TestFactories.createRaceWeekend(
            sessions = listOf(
                TestFactories.createSession(sessionId = "sess1", openF1SessionKey = 1001, sessionType = RaceWeekendSessionType.PRACTICE_1)
            )
        )
        val driver1 = TestFactories.createDriver(driverNumber = 1, driverId = "d1", acronym = "VER")
        val driver2 = TestFactories.createDriver(driverNumber = 4, driverId = "d2", acronym = "NOR")

        val results = listOf(
            createOpenF1Result(driverNumber = 1, duration = 85.0),
            createOpenF1Result(driverNumber = 4, duration = 86.5),
            createOpenF1Result(driverNumber = 1, duration = 84.2),  // VER's fastest
            createOpenF1Result(driverNumber = 4, duration = 85.8)   // NOR's fastest
        )

        coEvery { openF1Client.getResults(meetingKey = 100, sessionKeys = listOf("1001")) } returns flowOf(
            results[0], results[1], results[2], results[3]
        )
        coEvery { driverService.getDriversInSessions(listOf(1001)) } returns flowOf(driver1, driver2)

        val result = practiceResultsService.getDriversResultsForCombinedPractice(raceWeekend).toList()

        assertEquals(2, result.size)
        val verResult = result.find { it.driverNumber == 1 }!!
        val norResult = result.find { it.driverNumber == 4 }!!
        assertEquals(84.2.seconds, verResult.fastestLap)
        assertEquals(85.8.seconds, norResult.fastestLap)
    }

    @Test
    fun `getDriversResultsForCombinedPractice should filter out non-practice sessions`() = runTest {
        val raceWeekend = TestFactories.createRaceWeekend(
            sessions = listOf(
                TestFactories.createSession(sessionId = "sess1", openF1SessionKey = 1001, sessionType = RaceWeekendSessionType.PRACTICE_1),
                TestFactories.createSession(sessionId = "sess2", openF1SessionKey = 1002, sessionType = RaceWeekendSessionType.PRACTICE_2),
                TestFactories.createSession(sessionId = "sess3", openF1SessionKey = 1003, sessionType = RaceWeekendSessionType.QUALIFYING),
                TestFactories.createSession(sessionId = "sess4", openF1SessionKey = 1004, sessionType = RaceWeekendSessionType.RACE),
                TestFactories.createSession(sessionId = "sess5", openF1SessionKey = 1005, sessionType = RaceWeekendSessionType.SPRINT_RACE)
            )
        )
        val driver = TestFactories.createDriver(driverNumber = 1)

        coEvery { openF1Client.getResults(meetingKey = 100, sessionKeys = listOf("1001", "1002")) } returns flowOf(
            createOpenF1Result(driverNumber = 1, duration = 85.0)
        )
        coEvery { driverService.getDriversInSessions(listOf(1001, 1002)) } returns flowOf(driver)

        val result = practiceResultsService.getDriversResultsForCombinedPractice(raceWeekend).toList()

        assertEquals(1, result.size)
        // Should only query P1 and P2 session keys, not qualifying/race/sprint
        coVerify { openF1Client.getResults(meetingKey = 100, sessionKeys = listOf("1001", "1002")) }
    }
}
