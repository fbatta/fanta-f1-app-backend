package net.battaglini.fantaf1appbackend.client

import com.google.genai.Client
import com.google.genai.types.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext
import net.battaglini.fantaf1appbackend.configuration.GoogleGenAIProperties
import org.springframework.stereotype.Component

@Component
class GenAIClientImpl(
    private val properties: GoogleGenAIProperties,
    private val client: Client
) : GenAIClient {
    override suspend fun generateContentNoThinking(prompt: String, instructions: List<String>): Flow<String> {
        val systemInstruction = Content.fromParts(*instructions.map { Part.fromText(it) }.toTypedArray())

        val config = GenerateContentConfig.builder()
            .thinkingConfig(ThinkingConfig.builder().thinkingBudget(0))
            .candidateCount(1)
            .maxOutputTokens(properties.defaultOutputTokens)
            .safetySettings(safetySetting)
            .systemInstruction(systemInstruction)
            .tools(googleSearchTool)
            .build()

        val response =
            withContext(Dispatchers.IO) {
                client.async.models.generateContent(properties.chatModel, prompt, config).get()
            }
        return response.parts()?.map { part ->
            if (part.text().isPresent) {
                return@map part.text().get()
            }
            return@map ""
        }?.asFlow() ?: emptyFlow()
    }

    companion object {
        private val safetySetting = SafetySetting.builder()
            .category(HarmCategory.Known.HARM_CATEGORY_HATE_SPEECH)
            .threshold(HarmBlockThreshold.Known.BLOCK_LOW_AND_ABOVE)
            .build()

        private val googleSearchTool = Tool.builder().googleSearch(GoogleSearch.builder()).build()
    }
}