package net.battaglini.fantaf1appbackend.model

import net.battaglini.fantaf1appbackend.serializer.KotlinInstantSerializer
import tools.jackson.databind.annotation.JsonSerialize
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class Race(
    val raceId: String,
    val openF1MeetingKey: Int,
    val raceName: String,
    @JsonSerialize(using = KotlinInstantSerializer::class)
    val dateStart: Instant,
    @JsonSerialize(using = KotlinInstantSerializer::class)
    val dateEnd: Instant
)
