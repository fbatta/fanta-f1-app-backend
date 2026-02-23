package net.battaglini.fantaf1appbackend.serializer

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ser.std.StdSerializer
import kotlin.time.Instant

class KotlinInstantSerializer : StdSerializer<Instant>(Long::class.java) {
    override fun serialize(
        value: Instant?,
        gen: JsonGenerator?,
        provider: SerializationContext?
    ) {
        gen?.writeNumber(value?.toEpochMilliseconds() ?: 0)
    }
}