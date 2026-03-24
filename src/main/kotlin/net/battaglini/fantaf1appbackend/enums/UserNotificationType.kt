package net.battaglini.fantaf1appbackend.enums

import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.deser.std.StdDeserializer
import tools.jackson.databind.ser.std.StdSerializer

enum class UserNotificationType(val value: String) {
    RACE_WEEKEND_RESULTS_AVAILABLE("raceWeekendResultsAvailable"),
    UNKNOWN("unknown");

    companion object {
        fun fromValue(value: String): UserNotificationType {
            return entries.find { it.value == value } ?: UNKNOWN
        }

        class Deserializer : StdDeserializer<UserNotificationType>(String::class.java) {
            override fun deserialize(
                p: JsonParser?,
                ctxt: DeserializationContext?
            ): UserNotificationType? {
                return p?.let { UserNotificationType.fromValue(it.string) } ?: UNKNOWN
            }
        }

        class Serializer : StdSerializer<UserNotificationType>(String::class.java) {
            override fun serialize(
                value: UserNotificationType?,
                gen: JsonGenerator?,
                provider: SerializationContext?
            ) {
                value?.let { gen?.writeString(it.value) } ?: gen?.writeNull()
            }
        }
    }
}