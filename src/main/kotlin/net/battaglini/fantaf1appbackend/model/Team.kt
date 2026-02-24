package net.battaglini.fantaf1appbackend.model

import net.battaglini.fantaf1appbackend.deserializer.KotlinInstantDeserializer
import net.battaglini.fantaf1appbackend.serializer.KotlinInstantSerializer
import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.databind.annotation.JsonSerialize
import kotlin.time.Instant

data class Team(
    val teamId: String,
    val teamName: String,
    val teamAvatarUrl: String?,
    val ownerId: String,
    val lobbyId: String,
    @JsonSerialize(using = KotlinInstantSerializer::class)
    @JsonDeserialize(using = KotlinInstantDeserializer::class)
    val createdAt: Instant,
    @JsonSerialize(using = KotlinInstantSerializer::class)
    @JsonDeserialize(using = KotlinInstantDeserializer::class)
    val updatedAt: Instant,
    val points: MutableMap<Int, Double>
)
