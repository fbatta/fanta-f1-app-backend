package net.battaglini.fantaf1appbackend.enums

enum class RaceWeekendSessionType(
    val sessionName: String,
) {
    PRACTICE_1("Practice 1"),
    PRACTICE_2("Practice 2"),
    PRACTICE_3("Practice 3"),
    SPRINT_QUALIFYING("Sprint qualifying"),
    SPRINT_RACE("Sprint race"),
    QUALIFYING("Qualifying"),
    RACE("Race"),
    UNKNOWN("Unknown");

    companion object {
        fun fromSessionName(sessionName: String): RaceWeekendSessionType {
            return entries.find { it.sessionName.equals(sessionName, ignoreCase = true) } ?: UNKNOWN
        }

        fun fromString(sessionType: String): RaceWeekendSessionType {
            return entries.find { it.name.equals(sessionType, ignoreCase = true) } ?: UNKNOWN
        }
    }
}