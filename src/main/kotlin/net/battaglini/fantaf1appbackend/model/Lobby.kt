package net.battaglini.fantaf1appbackend.model

import net.battaglini.fantaf1appbackend.deserializer.KotlinInstantDeserializer
import net.battaglini.fantaf1appbackend.serializer.KotlinInstantSerializer
import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.databind.annotation.JsonSerialize
import kotlin.time.Instant

data class Lobby(
    val lobbyId: String,
    val lobbyName: String,
    val lobbyPassword: String,
    val ownerId: String,
    val year: Int,
    @JsonSerialize(using = KotlinInstantSerializer::class)
    @JsonDeserialize(using = KotlinInstantDeserializer::class)
    val createdAt: Instant,
    @JsonSerialize(using = KotlinInstantSerializer::class)
    @JsonDeserialize(using = KotlinInstantDeserializer::class)
    val updatedAt: Instant
)
