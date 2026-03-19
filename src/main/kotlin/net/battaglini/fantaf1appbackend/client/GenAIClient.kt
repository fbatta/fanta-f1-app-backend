package net.battaglini.fantaf1appbackend.client

import kotlinx.coroutines.flow.Flow

interface GenAIClient {
    suspend fun generateContentNoThinking(prompt: String, instructions: List<String>): Flow<String>
}