package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.battaglini.fantaf1appbackend.client.GenAIClient
import org.springframework.stereotype.Service
import kotlin.time.Clock

@Service
class GenAIServiceImpl(
    private val genAIClient: GenAIClient,
    private val clock: Clock,
    private val timeZone: TimeZone
) : GenAIService {
    override suspend fun generateDriverSummary(driverName: String, averageScore: Double): Flow<String> {
        val year = clock.now().toLocalDateTime(timeZone).year
        val instructions = listOf(
            "You are a sports journalist.",
            "You're writing a quick summary of a Formula 1 driver's $year season performance.",
            "Use the google search tool to find the latest information of the driver, as well as the last 2 results.",
            "Keep it brief and on point, maximum 40 words, excluding articles and conjunctions.",
            "Add a paragraph talking about how the formula 1 team for which they race is performing in the $year season.",
            "Add a sentence mentioning that the driver's average score in the IDGAF-1 app is ${
                "%.1f".format(
                    averageScore
                )
            }.",
            "Use markdown formatting."
        )

        return genAIClient.generateContentNoThinking(
            "How is $driverName doing in the $year Formula 1 season?",
            instructions
        )
    }

    override suspend fun generateRaceRecap(raceName: String): Flow<String> {
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