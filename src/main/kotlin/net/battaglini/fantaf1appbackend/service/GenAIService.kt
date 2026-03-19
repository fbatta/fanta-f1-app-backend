package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.battaglini.fantaf1appbackend.client.GenAIClient
import org.springframework.stereotype.Service
import kotlin.time.Clock

@Service
class GenAIService(
    private val genAIClient: GenAIClient
) {
    suspend fun generateDriverSummary(driverName: String): Flow<String> {
        val year = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
        val instructions = listOf(
            "You are a sports journalist.",
            "You're writing a quick summary of a Formula 1 driver's $year season performance.",
            "Keep it brief and on point.",
            "Add a paragraph talking about how the formula 1 team for which they race is performing in the $year season.",
        )

        return genAIClient.generateContentNoThinking(
            "How is $driverName doing in the $year Formula 1 season?",
            instructions
        )
    }

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