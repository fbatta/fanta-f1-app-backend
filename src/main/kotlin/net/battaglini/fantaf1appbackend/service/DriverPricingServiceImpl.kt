package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.toList
import net.battaglini.fantaf1appbackend.configuration.PricingProperties
import net.battaglini.fantaf1appbackend.model.Driver
import net.battaglini.fantaf1appbackend.model.DriverCost
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import net.battaglini.fantaf1appbackend.repository.DriverCostRepository
import net.battaglini.fantaf1appbackend.repository.DriverRepository
import net.battaglini.fantaf1appbackend.repository.RaceRepository
import net.battaglini.fantaf1appbackend.repository.RaceWeekendResultRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import kotlin.math.roundToLong

@Service
class DriverPricingServiceImpl(
    private val driverRepository: DriverRepository,
    private val driverCostRepository: DriverCostRepository,
    private val raceWeekendResultRepository: RaceWeekendResultRepository,
    private val raceRepository: RaceRepository,
    private val pricingProperties: PricingProperties
) : DriverPricingService {

    override suspend fun calculateAndUpdatePrices(
        lastRaceId: String?,
        acronyms: List<String>?,
        updateAll: Boolean
    ) {
        if (!pricingProperties.enable) {
            LOGGER.info("Driver pricing is disabled.")
            return
        }

        LOGGER.info("Starting driver pricing recalculation for raceId={}, acronyms={}, updateAll={}", lastRaceId, acronyms, updateAll)

        val activeDrivers = driverRepository.getDrivers().toList().filter { it.isActive }
        if (activeDrivers.isEmpty()) {
            LOGGER.warn("No active drivers found for pricing recalculation.")
            return
        }

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val allRaces = raceRepository.getRacesByYear(currentYear).toList()
            .sortedBy { it.dateStart }

        val effectiveLastRaceId = lastRaceId ?: allRaces.lastOrNull()?.raceId
        if (effectiveLastRaceId == null) {
            LOGGER.warn("No race found to use as anchor for pricing recalculation.")
            return
        }

        val lastRaceDateStart = allRaces.find { r -> r.raceId == effectiveLastRaceId }?.dateStart
        if (lastRaceDateStart == null) {
            LOGGER.warn("Last race with id {} not found in current year races.", effectiveLastRaceId)
            return
        }

        val lastRaces = allRaces.filter { it.dateStart <= lastRaceDateStart }
            .takeLast(pricingProperties.rollingWindowSize)
        
        val results = raceWeekendResultRepository.getRaceWeekendResults(lastRaces.map { it.raceId }).toList()
        // Ensure results are in the same order as lastRaces for smoothing logic
        val sortedResults = lastRaces.mapNotNull { race -> results.find { it.raceId == race.raceId } }

        val currentCostsMap = driverCostRepository.getDriversCosts().toList().associateBy { it.driverId }

        val newCosts = activeDrivers.map { driver ->
            val driverAvg = calculateRollingAvg(driver.driverId, sortedResults)
            val teammate = findTeammate(driver, activeDrivers)
            val teammateAvg = teammate?.let { calculateRollingAvg(it.driverId, sortedResults) } ?: driverAvg
            
            val powerScore = (pricingProperties.driverWeight * driverAvg) + (pricingProperties.teamWeight * teammateAvg)
            var newCostValue = mapScoreToPrice(powerScore)

            // Smoothing: If composite score improved compared to the state AFTER previous race, price cannot drop.
            // Using driverAvg as a proxy for improvement if previous results are not fully available.
            val previousResults = sortedResults.dropLast(1)
            if (previousResults.isNotEmpty()) {
                val previousDriverAvg = calculateRollingAvg(driver.driverId, previousResults)
                val previousTeammateAvg = teammate?.let { calculateRollingAvg(it.driverId, previousResults) } ?: previousDriverAvg
                val previousPowerScore = (pricingProperties.driverWeight * previousDriverAvg) + (pricingProperties.teamWeight * previousTeammateAvg)

                if (powerScore > previousPowerScore) {
                    val previousCost = currentCostsMap[driver.driverId]?.driverCost
                    if (previousCost != null) {
                        newCostValue = maxOf(newCostValue, previousCost)
                    }
                }
            }

            DriverCost(driver.driverId, newCostValue)
        }

        // Global Deflator logic
        val gridAvg = newCosts.map { it.driverCost }.average()
        val finalCosts = if (gridAvg > pricingProperties.maxAvgPriceThreshold) {
            val factor = pricingProperties.targetAvgPrice / gridAvg
            newCosts.map { it.copy(driverCost = (it.driverCost * factor).roundToLong().toDouble()) }
        } else {
            newCosts
        }

        if (pricingProperties.dryRun) {
            LOGGER.info("DRY RUN: New driver costs: {}", finalCosts)
        } else {
            driverCostRepository.createOrUpdateDriversCosts(finalCosts)
            LOGGER.info("Successfully updated driver costs for {} drivers. Grid average: {}", finalCosts.size, gridAvg)
        }
    }

    private fun calculateRollingAvg(driverId: String, results: List<RaceWeekendResult>): Double {
        val scores = results.mapNotNull { res -> res.results.find { it.driverId == driverId }?.points }
        return if (scores.isEmpty()) 0.0 else scores.average()
    }

    private fun findTeammate(driver: Driver, drivers: List<Driver>) =
        drivers.find { it.teamName == driver.teamName && it.driverId != driver.driverId }

    private fun mapScoreToPrice(score: Double): Double {
        // Map 0-20 points to 20-85 credits
        // Formula: Cost = 20 + (P / 20) * (85 - 20)
        val scoreClamped = score.coerceIn(0.0, 20.0)
        val mapped = pricingProperties.priceFloor + (scoreClamped / 20.0) * (pricingProperties.priceCeiling - pricingProperties.priceFloor)
        return mapped.roundToLong().toDouble()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DriverPricingServiceImpl::class.java)
    }
}
