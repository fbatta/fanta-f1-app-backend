package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.Flow

/**
 * Service responsible for generating AI-driven content related to Formula 1.
 *
 * This service interacts with the GenAIClient to provide summaries and recaps
 * based on specific driver performance and race events.
 */
interface GenAIService {
    /**
     * Generates a brief summary of a driver's performance for the current season.
     *
     * @param driverName The name of the Formula 1 driver.
     * @param averageScore The driver's average score within the application.
     * @return A [Flow] of strings containing the generated summary.
     */
    suspend fun generateDriverSummary(driverName: String, averageScore: Double): Flow<String>

    /**
     * Generates a comprehensive recap of a specific Formula 1 Grand Prix weekend.
     *
     * @param raceName The name of the Grand Prix (e.g., "Monaco Grand Prix").
     * @return A [Flow] of strings containing the generated race recap.
     */
    suspend fun generateRaceRecap(raceName: String): Flow<String>
}