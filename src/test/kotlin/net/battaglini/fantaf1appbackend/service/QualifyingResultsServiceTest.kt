package net.battaglini.fantaf1appbackend.service

import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import net.battaglini.fantaf1appbackend.client.OpenF1Client
import net.battaglini.fantaf1appbackend.enums.RaceWeekendSessionType
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1QualifyingSessionResultResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Duration.Companion.seconds

@ExtendWith(MockKExtension::class)
class QualifyingResultsServiceTest {

    @MockK
    lateinit var openF1Client: OpenF1Client

    @MockK
    lateinit var driverService: DriverService

    @InjectMockKs
    lateinit var qualifyingResultsService: QualifyingResultsServiceImpl

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    private fun createQualifyingResult(
        driverNumber: Int = 1,
        q1Time: Double? = 88.0,
        q2Time: Double? = 87.5,
        q3Time: Double? = 86.5,
        position: Int? = 1,
        dns: Boolean = false,
        dnf: Boolean = false,
        dsq: Boolean = false
    ) = OpenF1QualifyingSessionResultResponse(
        position = position ?: 1,
        driverNumber = driverNumber,
        numberOfLaps = 12,
        dnf = dnf,
        dns = dns,
        dsq = dsq,
        gapToLeader = listOf(null, null, null),
        duration = listOf(q1Time, q2Time, q3Time),
        meetingKey = 100,
        sessionKey = 1001
    )

    @Test
    fun `getDriversResultsForQualifying should map Q1 Q2 Q3 times from qualifying results`() = runTest {
        val raceWeekend = TestFactories.createRaceWeekend(
            sessions = listOf(
                TestFactories.createSession(sessionId = "sess1", openF1SessionKey = 1001, sessionType = RaceWeekendSessionType.QUALIFYING)
            )
        )
        val driver = TestFactories.createDriver(driverNumber = 1)

        coEvery { openF1Client.getQualifyingResults(sessionKeys = listOf("1001")) } returns flowOf(
            createQualifyingResult(driverNumber = 1, q1Time = 88.0, q2Time = 87.5, q3Time = 86.5, position = 1)
        )
        coEvery { driverService.getDriversInSessions(listOf(1001)) } returns flowOf(driver)

        val result = qualifyingResultsService.getDriversResultsForQualifying(raceWeekend, isSprintQualifying = false).toList()

        assertEquals(1, result.size)
        assertEquals(88.0.seconds, result[0].fastestLapQ1)
        assertEquals(87.5.seconds, result[0].fastestLapQ2)
        assertEquals(86.5.seconds, result[0].fastestLapQ3)
        assertEquals(1, result[0].finalPosition)
        assertEquals(RaceWeekendSessionType.QUALIFYING, result[0].sessionType)
    }

    @Test
    fun `getDriversResultsForQualifying should use SPRINT_QUALIFYING when isSprintQualifying is true`() = runTest {
        val raceWeekend = TestFactories.createRaceWeekend(
            sessions = listOf(
                TestFactories.createSession(
                    sessionId = "sess_sprint_q",
                    openF1SessionKey = 2001,
                    sessionType = RaceWeekendSessionType.SPRINT_QUALIFYING
                )
            )
        )
        val driver = TestFactories.createDriver(driverNumber = 4)

        coEvery { openF1Client.getQualifyingResults(sessionKeys = listOf("2001")) } returns flowOf(
            createQualifyingResult(driverNumber = 4, q1Time = 89.0, q2Time = 88.0, q3Time = 87.0, position = 3)
        )
        coEvery { driverService.getDriversInSessions(listOf(2001)) } returns flowOf(driver)

        val result = qualifyingResultsService.getDriversResultsForQualifying(raceWeekend, isSprintQualifying = true).toList()

        assertEquals(1, result.size)
        assertEquals(RaceWeekendSessionType.SPRINT_QUALIFYING, result[0].sessionType)
        assertEquals(3, result[0].finalPosition)
    }

    @Test
    fun `getDriversResultsForQualifying should return emptyFlow when no qualifying session found`() = runTest {
        val raceWeekend = TestFactories.createRaceWeekend(
            sessions = listOf(
                TestFactories.createSession(sessionId = "sess1", openF1SessionKey = 1001, sessionType = RaceWeekendSessionType.PRACTICE_1),
                TestFactories.createSession(sessionId = "sess2", openF1SessionKey = 1002, sessionType = RaceWeekendSessionType.RACE)
            )
        )

        val result = qualifyingResultsService.getDriversResultsForQualifying(raceWeekend, isSprintQualifying = false).toList()

        assertTrue(result.isEmpty())
        verify(exactly = 0) { openF1Client.getQualifyingResults(any()) }
        coVerify(exactly = 0) { driverService.getDriversInSessions(any()) }
    }

    @Test
    fun `getDriversResultsForQualifying should apply defaults when driver has no result`() = runTest {
        val raceWeekend = TestFactories.createRaceWeekend(
            sessions = listOf(
                TestFactories.createSession(sessionId = "sess1", openF1SessionKey = 1001, sessionType = RaceWeekendSessionType.QUALIFYING)
            )
        )
        val driver = TestFactories.createDriver(driverNumber = 99) // Driver not in results

        coEvery { openF1Client.getQualifyingResults(sessionKeys = listOf("1001")) } returns flowOf(
            createQualifyingResult(driverNumber = 1, q1Time = 88.0, q2Time = 87.0, q3Time = 86.0, position = 1)
        )
        coEvery { driverService.getDriversInSessions(listOf(1001)) } returns flowOf(driver)

        val result = qualifyingResultsService.getDriversResultsForQualifying(raceWeekend, isSprintQualifying = false).toList()

        assertEquals(1, result.size)
        // Defaults: Q1/Q2/Q3 = 9999s, position = 22, dns/dnf/dsq = false
        assertEquals(9_999.seconds, result[0].fastestLapQ1)
        assertEquals(9_999.seconds, result[0].fastestLapQ2)
        assertEquals(9_999.seconds, result[0].fastestLapQ3)
        assertEquals(22, result[0].finalPosition)
        assertFalse(result[0].dns)
        assertFalse(result[0].dnf)
        assertFalse(result[0].dsq)
    }

    @Test
    fun `getDriversResultsForQualifying should default Q3 when only Q1 and Q2 times available`() = runTest {
        val raceWeekend = TestFactories.createRaceWeekend(
            sessions = listOf(
                TestFactories.createSession(sessionId = "sess1", openF1SessionKey = 1001, sessionType = RaceWeekendSessionType.QUALIFYING)
            )
        )
        val driver = TestFactories.createDriver(driverNumber = 1)

        // Driver eliminated in Q2 — has Q1 and Q2 times but no Q3
        coEvery { openF1Client.getQualifyingResults(sessionKeys = listOf("1001")) } returns flowOf(
            createQualifyingResult(driverNumber = 1, q1Time = 88.0, q2Time = 87.5, q3Time = null, position = 15)
        )
        coEvery { driverService.getDriversInSessions(listOf(1001)) } returns flowOf(driver)

        val result = qualifyingResultsService.getDriversResultsForQualifying(raceWeekend, isSprintQualifying = false).toList()

        assertEquals(1, result.size)
        assertEquals(88.0.seconds, result[0].fastestLapQ1)
        assertEquals(87.5.seconds, result[0].fastestLapQ2)
        assertEquals(9_999.seconds, result[0].fastestLapQ3)
        assertEquals(15, result[0].finalPosition)
    }

    @Test
    fun `getDriversResultsForQualifying should set dns flag when driver is DNS`() = runTest {
        val raceWeekend = TestFactories.createRaceWeekend(
            sessions = listOf(
                TestFactories.createSession(sessionId = "sess1", openF1SessionKey = 1001, sessionType = RaceWeekendSessionType.QUALIFYING)
            )
        )
        val driver = TestFactories.createDriver(driverNumber = 77)

        coEvery { openF1Client.getQualifyingResults(sessionKeys = listOf("1001")) } returns flowOf(
            createQualifyingResult(driverNumber = 77, dns = true, dnf = false, dsq = false, position = null)
        )
        coEvery { driverService.getDriversInSessions(listOf(1001)) } returns flowOf(driver)

        val result = qualifyingResultsService.getDriversResultsForQualifying(raceWeekend, isSprintQualifying = false).toList()

        assertEquals(1, result.size)
        assertTrue(result[0].dns)
        assertFalse(result[0].dnf)
        assertFalse(result[0].dsq)
    }

    @Test
    fun `getDriversResultsForQualifying should set dnf flag when driver has DNF`() = runTest {
        val raceWeekend = TestFactories.createRaceWeekend(
            sessions = listOf(
                TestFactories.createSession(sessionId = "sess1", openF1SessionKey = 1001, sessionType = RaceWeekendSessionType.QUALIFYING)
            )
        )
        val driver = TestFactories.createDriver(driverNumber = 20)

        coEvery { openF1Client.getQualifyingResults(sessionKeys = listOf("1001")) } returns flowOf(
            createQualifyingResult(driverNumber = 20, q1Time = 88.0, dnf = true, position = null)
        )
        coEvery { driverService.getDriversInSessions(listOf(1001)) } returns flowOf(driver)

        val result = qualifyingResultsService.getDriversResultsForQualifying(raceWeekend, isSprintQualifying = false).toList()

        assertEquals(1, result.size)
        assertFalse(result[0].dns)
        assertTrue(result[0].dnf)
        assertFalse(result[0].dsq)
    }

    @Test
    fun `getDriversResultsForQualifying should set dsq flag when driver is disqualified`() = runTest {
        val raceWeekend = TestFactories.createRaceWeekend(
            sessions = listOf(
                TestFactories.createSession(sessionId = "sess1", openF1SessionKey = 1001, sessionType = RaceWeekendSessionType.QUALIFYING)
            )
        )
        val driver = TestFactories.createDriver(driverNumber = 18)

        coEvery { openF1Client.getQualifyingResults(sessionKeys = listOf("1001")) } returns flowOf(
            createQualifyingResult(driverNumber = 18, q1Time = 88.0, q2Time = 87.0, dsq = true, position = null)
        )
        coEvery { driverService.getDriversInSessions(listOf(1001)) } returns flowOf(driver)

        val result = qualifyingResultsService.getDriversResultsForQualifying(raceWeekend, isSprintQualifying = false).toList()

        assertEquals(1, result.size)
        assertFalse(result[0].dns)
        assertFalse(result[0].dnf)
        assertTrue(result[0].dsq)
    }

    @Test
    fun `getDriversResultsForQualifying should apply defaults when qualifying result durations are null`() = runTest {
        val raceWeekend = TestFactories.createRaceWeekend(
            sessions = listOf(
                TestFactories.createSession(sessionId = "sess1", openF1SessionKey = 1001, sessionType = RaceWeekendSessionType.QUALIFYING)
            )
        )
        val driver = TestFactories.createDriver(driverNumber = 1)

        // All Q times are null — position is non-nullable in OpenF1QualifyingSessionResultResponse
        // so it defaults to 1; only Q times get the 9999s default
        coEvery { openF1Client.getQualifyingResults(sessionKeys = listOf("1001")) } returns flowOf(
            createQualifyingResult(driverNumber = 1, q1Time = null, q2Time = null, q3Time = null, position = 1)
        )
        coEvery { driverService.getDriversInSessions(listOf(1001)) } returns flowOf(driver)

        val result = qualifyingResultsService.getDriversResultsForQualifying(raceWeekend, isSprintQualifying = false).toList()

        assertEquals(1, result.size)
        assertEquals(9_999.seconds, result[0].fastestLapQ1)
        assertEquals(9_999.seconds, result[0].fastestLapQ2)
        assertEquals(9_999.seconds, result[0].fastestLapQ3)
        // position is non-nullable in the model, so it uses the actual value (1)
        assertEquals(1, result[0].finalPosition)
    }
}
