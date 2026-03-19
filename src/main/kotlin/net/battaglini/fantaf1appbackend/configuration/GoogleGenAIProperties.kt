package net.battaglini.fantaf1appbackend.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "google.genai")
data class GoogleGenAIProperties(
    val apiKey: String,
    val chatModel: String,
    val chatModelThinking: String?,
    val defaultOutputTokens: Int,
    val imageModel: String
)
