package net.battaglini.fantaf1appbackend.service

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.test.runTest
import net.battaglini.fantaf1appbackend.configuration.PricingProperties
import net.battaglini.fantaf1appbackend.model.DriverCost
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import net.battaglini.fantaf1appbackend.repository.DriverCostRepository
import net.battaglini.fantaf1appbackend.repository.DriverRepository
import net.battaglini.fantaf1appbackend.repository.RaceRepository
import net.battaglini.fantaf1appbackend.repository.RaceWeekendResultRepository
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

class DriverPricingServiceTest {
    private val driverRepository = mockk<DriverRepository>()
    private val driverCostRepository = mockk<DriverCostRepository>()
    private val raceWeekendResultRepository = mockk<RaceWeekendResultRepository>()
    private val raceRepository = mockk<RaceRepository>()
    private val pricingProperties = PricingProperties(
        enable = true,
        dryRun = false,
        rollingWindowSize = 3,
        driverWeight = 0.8,
        teamWeight = 0.2,
        priceFloor = 20.0,
        priceCeiling = 85.0,
        targetAvgPrice = 50.0,
        maxAvgPriceThreshold = 100.0 // Increased to avoid deflator in most tests
    )

    private val service = DriverPricingServiceImpl(
        driverRepository,
        driverCostRepository,
        raceWeekendResultRepository,
        raceRepository,
        pricingProperties
    )

    @Test
    fun `should calculate and update prices correctly`() = runTest {
        // Arrange
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val d1 = TestFactories.createDriver(driverId = "d1", teamName = "T1")
        val d2 = TestFactories.createDriver(driverId = "d2", teamName = "T1")
        val activeDrivers = listOf(d1, d2)

        val r1 = TestFactories.createRaceWeekend(raceId = "r1", dateStart = kotlin.time.Instant.parse("2025-01-01T10:00:00Z"))
        val r2 = TestFactories.createRaceWeekend(raceId = "r2", dateStart = kotlin.time.Instant.parse("2025-01-08T10:00:00Z"))
        val r3 = TestFactories.createRaceWeekend(raceId = "r3", dateStart = kotlin.time.Instant.parse("2025-01-15T10:00:00Z"))
        val allRaces = listOf(r1, r2, r3)

        val res1 = RaceWeekendResult("r1", "GP1", 101, kotlin.time.Clock.System.now(), kotlin.time.Clock.System.now(), 1, listOf(
            RaceWeekendResult.Companion.Result("d1", 1, "VER", 10.0),
            RaceWeekendResult.Companion.Result("d2", 2, "PER", 5.0)
        ), null)
        val res2 = RaceWeekendResult("r2", "GP2", 102, kotlin.time.Clock.System.now(), kotlin.time.Clock.System.now(), 1, listOf(
            RaceWeekendResult.Companion.Result("d1", 1, "VER", 15.0),
            RaceWeekendResult.Companion.Result("d2", 2, "PER", 5.0)
        ), null)
        val res3 = RaceWeekendResult("r3", "GP3", 103, kotlin.time.Clock.System.now(), kotlin.time.Clock.System.now(), 1, listOf(
            RaceWeekendResult.Companion.Result("d1", 1, "VER", 20.0),
            RaceWeekendResult.Companion.Result("d2", 2, "PER", 10.0)
        ), null)

        coEvery { driverRepository.getDrivers() } returns activeDrivers.asFlow()
        coEvery { raceRepository.getRacesByYear(year) } returns allRaces.asFlow()
        coEvery { raceWeekendResultRepository.getRaceWeekendResults(any()) } returns listOf(res1, res2, res3).asFlow()
        coEvery { driverCostRepository.getDriversCosts() } returns emptyList<DriverCost>().asFlow()
        coEvery { driverCostRepository.createOrUpdateDriversCosts(any()) } returns Unit

        // Act
        val updates = service.calculateAndUpdatePrices("r3", updateAll = true)

        // Assert
        assertEquals(2, updates.size)
        val d1Update = updates.find { it.driverId == "d1" }!!
        assertEquals(20.0, d1Update.previousPrice) // Floor since currentCostsMap is empty
        assertEquals(63.0, d1Update.newPrice)
        assertEquals(215.0, d1Update.percentageChange)

        val d2Update = updates.find { it.driverId == "d2" }!!
        assertEquals(20.0, d2Update.previousPrice)
        assertEquals(47.0, d2Update.newPrice)
        assertEquals(135.0, d2Update.percentageChange)

        coVerify {
            driverCostRepository.createOrUpdateDriversCosts(withArg { costs ->
                assertEquals(2, costs.size)
                assertEquals(63.0, costs.find { it.driverId == "d1" }?.driverCost)
                assertEquals(47.0, costs.find { it.driverId == "d2" }?.driverCost)
            })
        }
    }

    @Test
    fun `should detect latest race when lastRaceId is null`() = runTest {
        // Arrange
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val d1 = TestFactories.createDriver(driverId = "d1", teamName = "T1")
        val activeDrivers = listOf(d1)

        val r1 = TestFactories.createRaceWeekend(raceId = "r1", dateStart = kotlin.time.Instant.parse("2025-01-01T10:00:00Z"))
        val r2 = TestFactories.createRaceWeekend(raceId = "r2", dateStart = kotlin.time.Instant.parse("2025-01-08T10:00:00Z"))
        val allRaces = listOf(r1, r2)

        val res1 = RaceWeekendResult("r1", "GP1", 101, kotlin.time.Clock.System.now(), kotlin.time.Clock.System.now(), 1, listOf(
            RaceWeekendResult.Companion.Result("d1", 1, "VER", 10.0)
        ), null)

        coEvery { driverRepository.getDrivers() } returns activeDrivers.asFlow()
        coEvery { raceRepository.getRacesByYear(year) } returns allRaces.asFlow()
        coEvery { raceWeekendResultRepository.findRaceWeekendResult(raceId = "r2") } returns null
        coEvery { raceWeekendResultRepository.findRaceWeekendResult(raceId = "r1") } returns res1
        coEvery { raceWeekendResultRepository.getRaceWeekendResults(listOf("r1")) } returns listOf(res1).asFlow()
        coEvery { driverCostRepository.getDriversCosts() } returns emptyList<DriverCost>().asFlow()
        coEvery { driverCostRepository.createOrUpdateDriversCosts(any()) } returns Unit

        // Act
        service.calculateAndUpdatePrices(lastRaceId = null, updateAll = true)

        // Assert
        coVerify {
            // Should have used r1 as anchor
            raceWeekendResultRepository.getRaceWeekendResults(listOf("r1"))
            driverCostRepository.createOrUpdateDriversCosts(any())
        }
    }

    @Test
    fun `should update only specified drivers`() = runTest {
        // Arrange
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val d1 = TestFactories.createDriver(driverId = "d1", acronym = "VER", teamName = "T1")
        val d2 = TestFactories.createDriver(driverId = "d2", acronym = "PER", teamName = "T1")
        val activeDrivers = listOf(d1, d2)

        val r1 = TestFactories.createRaceWeekend(raceId = "r1", dateStart = kotlin.time.Instant.parse("2025-01-01T10:00:00Z"))
        val allRaces = listOf(r1)

        val res1 = RaceWeekendResult("r1", "GP1", 101, kotlin.time.Clock.System.now(), kotlin.time.Clock.System.now(), 1, listOf(
            RaceWeekendResult.Companion.Result("d1", 1, "VER", 20.0),
            RaceWeekendResult.Companion.Result("d2", 2, "PER", 20.0)
        ), null)

        coEvery { driverRepository.getDrivers() } returns activeDrivers.asFlow()
        coEvery { raceRepository.getRacesByYear(year) } returns allRaces.asFlow()
        coEvery { raceWeekendResultRepository.getRaceWeekendResults(any()) } returns listOf(res1).asFlow()
        coEvery { driverCostRepository.getDriversCosts() } returns emptyList<DriverCost>().asFlow()
        coEvery { driverCostRepository.createOrUpdateDriversCosts(any()) } returns Unit

        // Act: Update only VER
        service.calculateAndUpdatePrices(lastRaceId = "r1", acronyms = listOf("VER"), updateAll = false)

        // Assert
        coVerify {
            driverCostRepository.createOrUpdateDriversCosts(withArg { costs ->
                assertEquals(1, costs.size)
                assertEquals("d1", costs[0].driverId)
            })
        }
    }

    @Test
    fun `should apply smoothing correctly`() = runTest {
        // Arrange
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val d1 = TestFactories.createDriver(driverId = "d1", teamName = "T1")
        val activeDrivers = listOf(d1)

        val r1 = TestFactories.createRaceWeekend(raceId = "r1", dateStart = kotlin.time.Instant.parse("2025-01-01T10:00:00Z"))
        val r2 = TestFactories.createRaceWeekend(raceId = "r2", dateStart = kotlin.time.Instant.parse("2025-01-08T10:00:00Z"))
        val r3 = TestFactories.createRaceWeekend(raceId = "r3", dateStart = kotlin.time.Instant.parse("2025-01-15T10:00:00Z"))
        val allRaces = listOf(r1, r2, r3)

        val res1 = RaceWeekendResult("r1", "GP1", 101, kotlin.time.Clock.System.now(), kotlin.time.Clock.System.now(), 1, listOf(
            RaceWeekendResult.Companion.Result("d1", 1, "VER", 20.0)
        ), null)
        val res2 = RaceWeekendResult("r2", "GP2", 102, kotlin.time.Clock.System.now(), kotlin.time.Clock.System.now(), 1, listOf(
            RaceWeekendResult.Companion.Result("d1", 1, "VER", 10.0)
        ), null)
        val res3 = RaceWeekendResult("r3", "GP3", 103, kotlin.time.Clock.System.now(), kotlin.time.Clock.System.now(), 1, listOf(
            RaceWeekendResult.Companion.Result("d1", 1, "VER", 18.0)
        ), null)

        val previousCost = 75.0
        coEvery { driverRepository.getDrivers() } returns activeDrivers.asFlow()
        coEvery { raceRepository.getRacesByYear(year) } returns allRaces.asFlow()
        coEvery { raceWeekendResultRepository.getRaceWeekendResults(any()) } returns listOf(res1, res2, res3).asFlow()
        coEvery { driverCostRepository.getDriversCosts() } returns listOf(DriverCost("d1", previousCost)).asFlow()
        coEvery { driverCostRepository.createOrUpdateDriversCosts(any()) } returns Unit

        // Act
        service.calculateAndUpdatePrices("r3", updateAll = true)

        // Assert
        coVerify {
            driverCostRepository.createOrUpdateDriversCosts(withArg { costs ->
                assertEquals(75.0, costs.find { it.driverId == "d1" }?.driverCost)
            })
        }
    }

    @Test
    fun `should apply global deflator correctly`() = runTest {
        // Arrange
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val d1 = TestFactories.createDriver(driverId = "d1", acronym = "VER", teamName = "T1")
        val d2 = TestFactories.createDriver(driverId = "d2", acronym = "HAM", teamName = "T2")
        val activeDrivers = listOf(d1, d2)

        val r1 = TestFactories.createRaceWeekend(raceId = "r1", dateStart = kotlin.time.Instant.parse("2025-01-01T10:00:00Z"))
        val allRaces = listOf(r1)

        val res1 = RaceWeekendResult("r1", "GP1", 101, kotlin.time.Clock.System.now(), kotlin.time.Clock.System.now(), 1, listOf(
            RaceWeekendResult.Companion.Result("d1", 1, "VER", 20.0),
            RaceWeekendResult.Companion.Result("d2", 2, "HAM", 20.0)
        ), null)

        val localProperties = pricingProperties.copy(maxAvgPriceThreshold = 52.0)
        val localService = DriverPricingServiceImpl(
            driverRepository,
            driverCostRepository,
            raceWeekendResultRepository,
            raceRepository,
            localProperties
        )

        coEvery { driverRepository.getDrivers() } returns activeDrivers.asFlow()
        coEvery { raceRepository.getRacesByYear(year) } returns allRaces.asFlow()
        coEvery { raceWeekendResultRepository.getRaceWeekendResults(any()) } returns listOf(res1).asFlow()
        coEvery { driverCostRepository.getDriversCosts() } returns emptyList<DriverCost>().asFlow()
        coEvery { driverCostRepository.createOrUpdateDriversCosts(any()) } returns Unit

        // Act
        localService.calculateAndUpdatePrices("r1", updateAll = true)

        // Assert
        coVerify {
            driverCostRepository.createOrUpdateDriversCosts(withArg { costs ->
                assertEquals(50.0, costs.find { it.driverId == "d1" }?.driverCost)
                assertEquals(50.0, costs.find { it.driverId == "d2" }?.driverCost)
            })
        }
    }

    @Test
    fun `should apply deflator to only updated drivers while considering entire grid`() = runTest {
        // Arrange
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val d1 = TestFactories.createDriver(driverId = "d1", acronym = "VER", teamName = "T1")
        val d2 = TestFactories.createDriver(driverId = "d2", acronym = "HAM", teamName = "T2")
        val activeDrivers = listOf(d1, d2)

        val r1 = TestFactories.createRaceWeekend(raceId = "r1", dateStart = kotlin.time.Instant.parse("2025-01-01T10:00:00Z"))
        val allRaces = listOf(r1)

        val res1 = RaceWeekendResult("r1", "GP1", 101, kotlin.time.Clock.System.now(), kotlin.time.Clock.System.now(), 1, listOf(
            RaceWeekendResult.Companion.Result("d1", 1, "VER", 20.0),
            RaceWeekendResult.Companion.Result("d2", 2, "HAM", 0.0)
        ), null)

        // HAM already has high price, which pushes grid avg up
        val currentCosts = listOf(
            DriverCost("d1", 20.0),
            DriverCost("d2", 80.0)
        )

        val localProperties = pricingProperties.copy(maxAvgPriceThreshold = 52.0, targetAvgPrice = 50.0)
        val localService = DriverPricingServiceImpl(
            driverRepository,
            driverCostRepository,
            raceWeekendResultRepository,
            raceRepository,
            localProperties
        )

        coEvery { driverRepository.getDrivers() } returns activeDrivers.asFlow()
        coEvery { raceRepository.getRacesByYear(year) } returns allRaces.asFlow()
        coEvery { raceWeekendResultRepository.getRaceWeekendResults(any()) } returns listOf(res1).asFlow()
        coEvery { driverCostRepository.getDriversCosts() } returns currentCosts.asFlow()
        coEvery { driverCostRepository.createOrUpdateDriversCosts(any()) } returns Unit

        // Act: Update only VER. 
        // P(VER) = 20 -> New Candidate Cost = 85.
        // Projected Grid: VER(85) + HAM(80) = 165. Avg = 82.5.
        // 82.5 > 52. Factor = 50 / 82.5 = 0.606
        // Final VER Cost = round(85 * 0.606) = 52.
        localService.calculateAndUpdatePrices(lastRaceId = "r1", acronyms = listOf("VER"), updateAll = false)

        // Assert
        coVerify {
            driverCostRepository.createOrUpdateDriversCosts(withArg { costs ->
                assertEquals(1, costs.size)
                assertEquals("d1", costs[0].driverId)
                assertEquals(52.0, costs[0].driverCost)
            })
        }
    }
}
