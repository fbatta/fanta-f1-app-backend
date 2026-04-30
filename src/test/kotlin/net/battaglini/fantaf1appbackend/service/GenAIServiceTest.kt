package net.battaglini.fantaf1appbackend.service

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import net.battaglini.fantaf1appbackend.client.GenAIClient
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Clock
import kotlin.time.Instant

@ExtendWith(MockKExtension::class)
class GenAIServiceTest {

    @MockK
    lateinit var genAIClient: GenAIClient

    @MockK
    lateinit var clock: Clock

    val timeZone = TimeZone.UTC

    @InjectMockKs
    lateinit var genAIService: GenAIServiceImpl

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `generateDriverSummary should construct prompt with current year from Clock`() = runTest {
        every { clock.now() } returns Instant.parse("2025-07-15T10:00:00Z")
        val expectedPrompt = "How is Max Verstappen doing in the 2025 Formula 1 season?"
        val expectedInstructions = listOf(
            "You are a sports journalist.",
            "You're writing a quick summary of a Formula 1 driver's 2025 season performance.",
            "Use the google search tool to find the latest information of the driver, as well as the last 2 results.",
            "Keep it brief and on point, maximum 40 words, excluding articles and conjunctions.",
            "Add a paragraph talking about how the formula 1 team for which they race is performing in the 2025 season.",
            "Add a sentence mentioning that the driver's average score in the IDGAF-1 app is 25.0.",
            "Use markdown formatting. The sentence about the idgaf-1 score should be bold"
        )

        coEvery { genAIClient.generateContentNoThinking(any(), any()) } returns flowOf("Generated summary")

        val result = genAIService.generateDriverSummary("Max Verstappen", 25.0).toList()

        assertEquals(1, result.size)
        assertEquals("Generated summary", result[0])
        coVerify { genAIClient.generateContentNoThinking(expectedPrompt, expectedInstructions) }
    }

    @Test
    fun `generateDriverSummary should include driver name in prompt`() = runTest {
        every { clock.now() } returns Instant.parse("2025-01-01T00:00:00Z")

        coEvery { genAIClient.generateContentNoThinking(any(), any()) } returns flowOf("Summary")

        genAIService.generateDriverSummary("Charles Leclerc", 18.5).toList()

        coVerify {
            genAIClient.generateContentNoThinking(withArg { prompt ->
                assertTrue(prompt.contains("Charles Leclerc"), "Prompt should contain driver name")
                assertTrue(prompt.contains("Formula 1 season"))
            }, any())
        }
    }

    @Test
    fun `generateDriverSummary should include formatted average score in instructions`() = runTest {
        every { clock.now() } returns Instant.parse("2025-06-01T00:00:00Z")

        coEvery { genAIClient.generateContentNoThinking(any(), any()) } returns flowOf("Summary")

        genAIService.generateDriverSummary("Lewis Hamilton", 21.333).toList()

        coVerify {
            genAIClient.generateContentNoThinking(any(), withArg { instructions ->
                val scoreInstruction = instructions.find { it.contains("IDGAF-1 app") }
                assertNotNull(scoreInstruction, "Should contain a sentence about average score")
                assertTrue(scoreInstruction!!.contains("21.3"), "Should format average score to 1 decimal place")
            })
        }
    }

    @Test
    fun `generateDriverSummary should delegate to genAIClient with correct prompt and instructions`() = runTest {
        every { clock.now() } returns Instant.parse("2026-03-01T12:00:00Z")

        coEvery { genAIClient.generateContentNoThinking(any<String>(), any<List<String>>()) } returns flowOf(
            "Line 1",
            "Line 2"
        )

        val result = genAIService.generateDriverSummary("Fernando Alonso", 15.0).toList()

        assertEquals(2, result.size)
        assertEquals("Line 1", result[0])
        assertEquals("Line 2", result[1])
        coVerify(exactly = 1) { genAIClient.generateContentNoThinking(any(), any()) }
    }

    @Test
    fun `generateRaceRecap should construct prompt with race name`() = runTest {
        every { clock.now() } returns Instant.parse("2025-06-01T12:00:00Z")
        coEvery { genAIClient.generateContentNoThinking(any(), any()) } returns flowOf("Race recap content")

        genAIService.generateRaceRecap("Monaco Grand Prix").toList()

        coVerify {
            genAIClient.generateContentNoThinking(withArg { prompt ->
                assertTrue(prompt.contains("Monaco Grand Prix"), "Prompt should contain race name")
                assertEquals("Give me a recap of the 2025 Monaco Grand Prix", prompt)
            }, any())
        }
    }

    @Test
    fun `generateRaceRecap should include correct instructions list`() = runTest {
        every { clock.now() } returns Instant.parse("2025-06-01T12:00:00Z")
        val expectedInstructions = listOf(
            "You are a sports journalist.",
            "You're writing a quick summary of a Formula 1 Grand Prix.",
            "Consider the entire weekend (free practice, qualifying, etc.), not just the actual race.",
            "If the race weekend included Sprint qualifying and Sprint race, include those int the recap as well",
            "Use the google search tool to find the latest information about the race.",
            "Mention the weather conditions.",
            "Mention which teams and drivers did best and which did worst.",
            "Use markdown formatting."
        )

        coEvery { genAIClient.generateContentNoThinking(any(), any()) } returns flowOf("Recap")

        genAIService.generateRaceRecap("British Grand Prix").toList()

        coVerify { genAIClient.generateContentNoThinking(any(), expectedInstructions) }
    }

    @Test
    fun `generateDriverSummary should use correct TimeZone and Clock for year extraction`() = runTest {
        // Different year should be reflected in the prompt
        every { clock.now() } returns Instant.parse("2024-12-31T23:59:59Z")

        coEvery { genAIClient.generateContentNoThinking(any(), any()) } returns flowOf("Summary")

        genAIService.generateDriverSummary("George Russell", 12.0).toList()

        coVerify {
            genAIClient.generateContentNoThinking(withArg { prompt ->
                assertTrue(prompt.contains("2024"), "Prompt should contain year 2024 from Clock")
            }, withArg { instructions ->
                val yearInstruction = instructions.find { it.contains("season performance") }
                assertTrue(yearInstruction!!.contains("2024"), "Instructions should mention year 2024")
            })
        }
    }

    @Test
    fun `generateRaceRecap should delegate to client even with empty race name`() = runTest {
        every { clock.now() } returns Instant.parse("2025-06-01T12:00:00Z")
        // No validation on race name — empty string should still delegate
        coEvery { genAIClient.generateContentNoThinking(any(), any()) } returns flowOf("Empty recap")

        val result = genAIService.generateRaceRecap("").toList()

        assertEquals(1, result.size)
        assertEquals("Empty recap", result[0])
        coVerify { genAIClient.generateContentNoThinking("Give me a recap of the 2025 ", any<List<String>>()) }
    }
}
