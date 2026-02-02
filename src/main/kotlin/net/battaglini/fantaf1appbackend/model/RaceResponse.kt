package net.battaglini.fantaf1appbackend.model

import net.battaglini.fantaf1appbackend.serializer.KotlinInstantSerializer
import tools.jackson.databind.annotation.JsonSerialize
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class RaceResponse @OptIn(ExperimentalTime::class) constructor(
    val raceName: String,
    @JsonSerialize(using = KotlinInstantSerializer::class)
    val dateStart: Instant,
    @JsonSerialize(using = KotlinInstantSerializer::class)
    val dateEnd: Instant
)
