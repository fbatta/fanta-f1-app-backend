package net.battaglini.fantaf1appbackend.client

import kotlinx.coroutines.flow.Flow

interface GenAIClient {
    suspend fun generateContentNoThinking(prompt: String, instructions: List<String>): Flow<String>
    suspend fun generateContentThinking(prompt: String, instructions: List<String>, thinkingBudget: Int?): Flow<String>
}