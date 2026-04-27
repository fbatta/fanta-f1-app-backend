package net.battaglini.fantaf1appbackend.service

/**
 * Service responsible for calculating and updating dynamic prices for drivers
 * based on recent performance and team form.
 */
interface DriverPricingService {
    /**
     * Calculates new costs for all active drivers based on the last N race results.
     * Updates the costs in the repository.
     * 
     * @param lastRaceId The ID of the race that just finished.
     */
    suspend fun calculateAndUpdatePrices(lastRaceId: String)
}
