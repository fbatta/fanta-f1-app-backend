package net.battaglini.fantaf1appbackend.deserializer

import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.deser.std.StdDeserializer

class GapToLeaderDeserializer : StdDeserializer<Double>(String::class.java) {
    override fun deserialize(
        p: JsonParser?,
        ctxt: DeserializationContext?
    ): Double {
        return try {
            p?.string?.toDouble() ?: 9_999.0
        } catch (_: NumberFormatException) {
            9_999.0
        }
    }
}