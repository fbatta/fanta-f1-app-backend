package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.battaglini.fantaf1appbackend.client.GenAIClient
import org.springframework.stereotype.Service
import kotlin.time.Clock

/**
 * Service responsible for generating AI-driven content related to Formula 1.
 *
 * This service interacts with the [GenAIClient] to provide summaries and recaps
 * based on specific driver performance and race events.
 */
@Service
class GenAIService(
    private val genAIClient: GenAIClient,
    private val clock: Clock
) {
    /**
     * Generates a brief summary of a driver's performance for the current season.
     *
     * @param driverName The name of the Formula 1 driver.
     * @param averageScore The driver's average score within the IDGAF-1 application.
     * @return A [Flow] of strings containing the generated summary.
     */
    suspend fun generateDriverSummary(driverName: String, averageScore: Double): Flow<String> {
        val year = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
        val instructions = listOf(
            "You are a sports journalist.",
            "You're writing a quick summary of a Formula 1 driver's $year season performance.",
            "Keep it brief and on point, maximum 40 words, excluding articles and conjunctions.",
            "Add a paragraph talking about how the formula 1 team for which they race is performing in the $year season.",
            "Add a sentence mentioning that the driver's average score in the IDGAF-1 app is $averageScore."
        )

        return genAIClient.generateContentNoThinking(
            "How is $driverName doing in the $year Formula 1 season?",
            instructions
        )
    }

    /**
     * Generates a comprehensive recap of a specific Formula 1 Grand Prix weekend.
     *
     * @param raceName The name of the Grand Prix (e.g., "Monaco Grand Prix").
     * @return A [Flow] of strings containing the generated race recap.
     */
    suspend fun generateRaceRecap(raceName: String): Flow<String> {
        val instructions = listOf(
            "You are a sports journalist.",
            "You're writing a quick summary of a Formula 1 Grand Prix.",
            "Consider the entire weekend (free practice, qualifying, etc.), not just the actual race.",
            "Mention the weather conditions.",
            "Mention which teams and drivers did best and which did worst."
        )

        return genAIClient.generateContentNoThinking("Give me a recap of the $raceName", instructions)
    }
}