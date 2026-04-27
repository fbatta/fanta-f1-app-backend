package net.battaglini.fantaf1appbackend.service

import net.battaglini.fantaf1appbackend.model.response.DriverPriceUpdateDetails

/**
 * Service responsible for calculating and updating dynamic prices for drivers
 * based on recent performance and team form.
 */
interface DriverPricingService {
    /**
     * Calculates new costs for drivers based on the last N race results.
     * 
     * @param lastRaceId The ID of the race to use as the anchor. If null, uses the latest race with results.
     * @param acronyms Optional list of driver acronyms to update. If null/empty and updateAll is false, does nothing.
     * @param updateAll If true, updates all active drivers regardless of the acronyms list.
     * @return A list of updated driver details including old and new prices.
     */
    suspend fun calculateAndUpdatePrices(
        lastRaceId: String? = null,
        acronyms: List<String>? = null,
        updateAll: Boolean = false
    ): List<DriverPriceUpdateDetails>
}
