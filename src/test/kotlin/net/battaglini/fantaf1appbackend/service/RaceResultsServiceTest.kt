package net.battaglini.fantaf1appbackend.service

import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import net.battaglini.fantaf1appbackend.client.OpenF1Client
import net.battaglini.fantaf1appbackend.enums.RaceWeekendSessionType
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1LapResponse
import net.battaglini.fantaf1appbackend.model.openf1.OpenF1SessionResultResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Duration.Companion.seconds

@ExtendWith(MockKExtension::class)
class RaceResultsServiceTest {

    @MockK
    lateinit var openF1Client: OpenF1Client

    @MockK
    lateinit var driverService: DriverService

    @InjectMockKs
    lateinit var raceResultsService: RaceResultsServiceImpl

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    private fun createRaceResult(
        driverNumber: Int = 1,
        position: Int? = 1,
        dns: Boolean = false,
        dnf: Boolean = false,
        dsq: Boolean = false
    ) = OpenF1SessionResultResponse(
        position = position,
        driverNumber = driverNumber,
        numberOfLaps = 57,
        dnf = dnf,
        dns = dns,
        dsq = dsq,
        gapToLeader = null,
        duration = 87.5,
        meetingKey = 100,
        sessionKey = 1001
    )

    private fun createLap(
        driverNumber: Int = 1,
        lapDuration: Double? = 85.5,
        speedTrapSpeed: Double? = 320.0
    ) = OpenF1LapResponse(
        driverNumber = driverNumber,
        durationSector1 = 28.0,
        durationSector2 = 29.0,
        durationSector3 = 28.5,
        i1Speed = null,
        i2Speed = null,
        isPitOutLap = false,
        lapDuration = lapDuration,
        lapNumber = 1,
        meetingKey = 100,
        sessionKey = 1001,
        speedTrapSpeed = speedTrapSpeed
    )

    @Test
    fun `getResultsForRace should return race results for all drivers with correct fields`() = runTest {
        val raceWeekend = TestFactories.createRaceWeekend(
            sessions = listOf(
                TestFactories.createSession(sessionId = "sess1", openF1SessionKey = 1001, sessionType = RaceWeekendSessionType.RACE)
            )
        )
        val driver = TestFactories.createDriver(driverNumber = 1)

        coEvery { openF1Client.getResults(sessionKeys = listOf("1001")) } returns flowOf(
            createRaceResult(driverNumber = 1, position = 1)
        )
        coEvery { openF1Client.getLaps(sessionKey = "1001", driverNumber = 1) } returns flowOf(
            createLap(driverNumber = 1, lapDuration = 86.0, speedTrapSpeed = 318.0),
            createLap(driverNumber = 1, lapDuration = 85.5, speedTrapSpeed = 320.0)
        )
        coEvery { driverService.getDriversInSessions(listOf(1001)) } returns flowOf(driver)

        val result = raceResultsService.getResultsForRace(raceWeekend, isSprintRace = false).toList()

        assertEquals(1, result.size)
        assertEquals("race1", result[0].raceId)
        assertEquals("driver1", result[0].driverId)
        assertEquals(RaceWeekendSessionType.RACE, result[0].sessionType)
        assertEquals(1, result[0].driverNumber)
        assertEquals("VER", result[0].driverAcronym)
        assertEquals(85.5.seconds, result[0].fastestLap)
        assertEquals(1, result[0].finalPosition)
        assertEquals(0, result[0].numberOfOvertakes)
        assertEquals(320.0, result[0].maximumSpeedAtTrap)
    }

    @Test
    fun `getResultsForRace should use SPRINT_RACE session when isSprintRace is true`() = runTest {
        val raceWeekend = TestFactories.createRaceWeekend(
            sessions = listOf(
                TestFactories.createSession(
                    sessionId = "sess_sprint",
                    openF1SessionKey = 2001,
                    sessionType = RaceWeekendSessionType.SPRINT_RACE
                )
            )
        )
        val driver = TestFactories.createDriver(driverNumber = 4)

        coEvery { openF1Client.getResults(sessionKeys = listOf("2001")) } returns flowOf(
            createRaceResult(driverNumber = 4, position = 3)
        )
        coEvery { openF1Client.getLaps(sessionKey = "2001", driverNumber = 4) } returns emptyFlow()
        coEvery { driverService.getDriversInSessions(listOf(2001)) } returns flowOf(driver)

        val result = raceResultsService.getResultsForRace(raceWeekend, isSprintRace = true).toList()

        assertEquals(1, result.size)
        assertEquals(RaceWeekendSessionType.SPRINT_RACE, result[0].sessionType)
        assertEquals(3, result[0].finalPosition)
    }

    @Test
    fun `getResultsForRace should return emptyFlow when no race session found`() = runTest {
        val raceWeekend = TestFactories.createRaceWeekend(
            sessions = listOf(
                TestFactories.createSession(sessionId = "sess1", openF1SessionKey = 1001, sessionType = RaceWeekendSessionType.PRACTICE_1),
                TestFactories.createSession(sessionId = "sess2", openF1SessionKey = 1002, sessionType = RaceWeekendSessionType.QUALIFYING)
            )
        )

        val result = raceResultsService.getResultsForRace(raceWeekend, isSprintRace = false).toList()

        assertTrue(result.isEmpty())
        verify(exactly = 0) { openF1Client.getResults(any()) }
    }

    @Test
    fun `getResultsForRace should return emptyFlow when race results are empty`() = runTest {
        val raceWeekend = TestFactories.createRaceWeekend(
            sessions = listOf(
                TestFactories.createSession(sessionId = "sess1", openF1SessionKey = 1001, sessionType = RaceWeekendSessionType.RACE)
            )
        )

        coEvery { openF1Client.getResults(sessionKeys = listOf("1001")) } returns emptyFlow()

        val result = raceResultsService.getResultsForRace(raceWeekend, isSprintRace = false).toList()

        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { driverService.getDriversInSessions(any()) }
    }

    @Test
    fun `getResultsForRace should return emptyFlow on exception instead of throwing`() = runTest {
        val raceWeekend = TestFactories.createRaceWeekend(
            sessions = listOf(
                TestFactories.createSession(sessionId = "sess1", openF1SessionKey = 1001, sessionType = RaceWeekendSessionType.RACE)
            )
        )

        coEvery { openF1Client.getResults(sessionKeys = listOf("1001")) } throws RuntimeException("API timeout")

        val result = raceResultsService.getResultsForRace(raceWeekend, isSprintRace = false).toList()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getResultsForRace should default to position 22 when final position is null`() = runTest {
        val raceWeekend = TestFactories.createRaceWeekend(
            sessions = listOf(
                TestFactories.createSession(sessionId = "sess1", openF1SessionKey = 1001, sessionType = RaceWeekendSessionType.RACE)
            )
        )
        val driver = TestFactories.createDriver(driverNumber = 1)

        coEvery { openF1Client.getResults(sessionKeys = listOf("1001")) } returns flowOf(
            createRaceResult(driverNumber = 1, position = null)
        )
        coEvery { openF1Client.getLaps(sessionKey = "1001", driverNumber = 1) } returns emptyFlow()
        coEvery { driverService.getDriversInSessions(listOf(1001)) } returns flowOf(driver)

        val result = raceResultsService.getResultsForRace(raceWeekend, isSprintRace = false).toList()

        assertEquals(1, result.size)
        assertEquals(22, result[0].finalPosition)
    }

    @Test
    fun `getResultsForRace should propagate DNS DNF DSQ flags from race results`() = runTest {
        val raceWeekend = TestFactories.createRaceWeekend(
            sessions = listOf(
                TestFactories.createSession(sessionId = "sess1", openF1SessionKey = 1001, sessionType = RaceWeekendSessionType.RACE)
            )
        )
        val dnsDriver = TestFactories.createDriver(driverNumber = 77)
        val dnfDriver = TestFactories.createDriver(driverNumber = 20)
        val dsqDriver = TestFactories.createDriver(driverNumber = 18)

        coEvery { openF1Client.getResults(sessionKeys = listOf("1001")) } returns flowOf(
            createRaceResult(driverNumber = 77, dns = true, dnf = false, dsq = false),
            createRaceResult(driverNumber = 20, dns = false, dnf = true, dsq = false),
            createRaceResult(driverNumber = 18, dns = false, dnf = false, dsq = true)
        )
        coEvery { openF1Client.getLaps(sessionKey = "1001", driverNumber = 77) } returns emptyFlow()
        coEvery { openF1Client.getLaps(sessionKey = "1001", driverNumber = 20) } returns emptyFlow()
        coEvery { openF1Client.getLaps(sessionKey = "1001", driverNumber = 18) } returns emptyFlow()
        coEvery { driverService.getDriversInSessions(listOf(1001)) } returns flowOf(dnsDriver, dnfDriver, dsqDriver)

        val result = raceResultsService.getResultsForRace(raceWeekend, isSprintRace = false).toList()

        assertEquals(3, result.size)
        val dnsResult = result.find { it.driverNumber == 77 }!!
        val dnfResult = result.find { it.driverNumber == 20 }!!
        val dsqResult = result.find { it.driverNumber == 18 }!!
        assertTrue(dnsResult.dns)
        assertFalse(dnsResult.dnf)
        assertFalse(dnsResult.dsq)
        assertFalse(dnfResult.dns)
        assertTrue(dnfResult.dnf)
        assertFalse(dnfResult.dsq)
        assertFalse(dsqResult.dns)
        assertFalse(dsqResult.dnf)
        assertTrue(dsqResult.dsq)
    }
}
