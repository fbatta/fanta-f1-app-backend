package net.battaglini.fantaf1appbackend.enums.openf1

import net.battaglini.fantaf1appbackend.enums.RaceWeekendSessionType
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.deser.std.StdDeserializer

enum class OpenF1SessionName(
    val sessionName: String
) {
    PRACTICE_1("Practice 1"),
    PRACTICE_2("Practice 2"),
    PRACTICE_3("Practice 3"),
    SPRINT_QUALIFYING("Sprint qualifying"),
    SPRINT("Sprint"),
    QUALIFYING("Qualifying"),
    RACE("Race"),
    UNKNOWN("Unknown");

    override fun toString(): String {
        return sessionName
    }

    companion object {
        fun fromString(sessionName: String): OpenF1SessionName {
            return entries.find { it.sessionName.equals(sessionName, ignoreCase = true) } ?: UNKNOWN
        }

        fun OpenF1SessionName.toRaceWeekendSessionType(): RaceWeekendSessionType = when (this) {
            PRACTICE_1 -> RaceWeekendSessionType.PRACTICE_1
            PRACTICE_2 -> RaceWeekendSessionType.PRACTICE_2
            PRACTICE_3 -> RaceWeekendSessionType.PRACTICE_3
            SPRINT_QUALIFYING -> RaceWeekendSessionType.SPRINT_QUALIFYING
            SPRINT -> RaceWeekendSessionType.SPRINT_RACE
            QUALIFYING -> RaceWeekendSessionType.QUALIFYING
            RACE -> RaceWeekendSessionType.RACE
            UNKNOWN -> RaceWeekendSessionType.UNKNOWN
        }

        class Deserializer : StdDeserializer<OpenF1SessionName>(String::class.java) {
            override fun deserialize(
                p: JsonParser?,
                ctxt: DeserializationContext?
            ): OpenF1SessionName {
                return fromString(p?.string ?: return UNKNOWN)
            }
        }
    }
}