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
        service.calculateAndUpdatePrices("r3")

        // Assert
        // P(d1) = 0.8 * ((10+15+20)/3) + 0.2 * ((5+5+10)/3) = 0.8 * 15 + 0.2 * 6.666 = 12 + 1.333 = 13.333
        // Cost(d1) = 20 + (13.333 / 20) * (85 - 20) = 20 + 0.6666 * 65 = 20 + 43.333 = 63.333 -> 63
        
        // P(d2) = 0.8 * 6.666 + 0.2 * 15 = 5.333 + 3 = 8.333
        // Cost(d2) = 20 + (8.333 / 20) * 65 = 20 + 0.4166 * 65 = 20 + 27.083 = 47.083 -> 47

        coVerify {
            driverCostRepository.createOrUpdateDriversCosts(withArg { costs ->
                assertEquals(2, costs.size)
                assertEquals(63.0, costs.find { it.driverId == "d1" }?.driverCost)
                assertEquals(47.0, costs.find { it.driverId == "d2" }?.driverCost)
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

        // Previous results were better, but current average is still lower than previous cost
        // Let's say previous cost was 70.
        // Current Avg is 10, 15, 12 -> 12.333
        // Previous Avg (R1, R2) -> 12.5 (Wait, smoothing uses composite score improvement)
        
        val res1 = RaceWeekendResult("r1", "GP1", 101, kotlin.time.Clock.System.now(), kotlin.time.Clock.System.now(), 1, listOf(
            RaceWeekendResult.Companion.Result("d1", 1, "VER", 20.0)
        ), null)
        val res2 = RaceWeekendResult("r2", "GP2", 102, kotlin.time.Clock.System.now(), kotlin.time.Clock.System.now(), 1, listOf(
            RaceWeekendResult.Companion.Result("d1", 1, "VER", 10.0)
        ), null)
        val res3 = RaceWeekendResult("r3", "GP3", 103, kotlin.time.Clock.System.now(), kotlin.time.Clock.System.now(), 1, listOf(
            RaceWeekendResult.Companion.Result("d1", 1, "VER", 18.0)
        ), null)

        // driverAvg = (20 + 10 + 18) / 3 = 16.0
        // previousAvg (results.dropLast(1)) = (20 + 10) / 2 = 15.0
        // Since 16.0 > 15.0, price should not drop below previous cost.
        
        val previousCost = 75.0
        coEvery { driverRepository.getDrivers() } returns activeDrivers.asFlow()
        coEvery { raceRepository.getRacesByYear(year) } returns allRaces.asFlow()
        coEvery { raceWeekendResultRepository.getRaceWeekendResults(any()) } returns listOf(res1, res2, res3).asFlow()
        coEvery { driverCostRepository.getDriversCosts() } returns listOf(DriverCost("d1", previousCost)).asFlow()
        coEvery { driverCostRepository.createOrUpdateDriversCosts(any()) } returns Unit

        // Act
        service.calculateAndUpdatePrices("r3")

        // Assert
        // P(d1) = 16.0
        // NewCost = 20 + (16/20)*65 = 20 + 52 = 72.0
        // But smoothing says if improved, don't drop. 16.0 > 15.0, so keep 75.0.

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
        val d1 = TestFactories.createDriver(driverId = "d1", teamName = "T1")
        val d2 = TestFactories.createDriver(driverId = "d2", teamName = "T2")
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
        localService.calculateAndUpdatePrices("r1")

        // Assert
        // P(d1) = 20.0 -> Cost = 20 + (20/20)*65 = 85.0
        // P(d2) = 20.0 -> Cost = 20 + (20/20)*65 = 85.0
        // GridAvg = 85.0. 85.0 > 52.0.
        // Factor = 50 / 85 = 0.5882
        // FinalCost = round(85 * 0.5882) = round(50.0) = 50.0

        coVerify {
            driverCostRepository.createOrUpdateDriversCosts(withArg { costs ->
                assertEquals(50.0, costs.find { it.driverId == "d1" }?.driverCost)
                assertEquals(50.0, costs.find { it.driverId == "d2" }?.driverCost)
            })
        }
    }
}
