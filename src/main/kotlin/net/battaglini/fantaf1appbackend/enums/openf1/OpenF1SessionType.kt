package net.battaglini.fantaf1appbackend.enums.openf1

import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.deser.std.StdDeserializer

enum class OpenF1SessionType {
    PRACTICE,
    QUALIFYING,
    RACE,
    UNKNOWN;

    override fun toString(): String {
        return super.toString().lowercase().replaceFirstChar { it.titlecase() }
    }

    companion object {
        fun fromString(sessionType: String): OpenF1SessionType {
            return entries.find { it.toString().equals(sessionType, ignoreCase = true) } ?: UNKNOWN
        }

        class Deserializer : StdDeserializer<OpenF1SessionType>(String::class.java) {
            override fun deserialize(
                p: JsonParser?,
                ctxt: DeserializationContext?
            ): OpenF1SessionType {
                return fromString(p?.string ?: return UNKNOWN)
            }
        }
    }
}