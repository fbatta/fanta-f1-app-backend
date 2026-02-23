package net.battaglini.fantaf1appbackend.deserializer

import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.deser.std.StdDeserializer
import kotlin.time.Instant

class KotlinInstantDeserializer : StdDeserializer<Instant>(Long::class.java) {
    override fun deserialize(
        p: JsonParser?,
        ctxt: DeserializationContext?
    ): Instant? {
        return p?.longValue?.let { return Instant.fromEpochMilliseconds(it) }
    }
}