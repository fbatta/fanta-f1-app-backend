package net.battaglini.fantaf1appbackend.enums

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UserNotificationTypeTest {

    @Test
    fun `should have LINEUP_OPEN with value lineupOpen`() {
        val type = UserNotificationType.valueOf("LINEUP_OPEN")
        assertEquals("lineupOpen", type.value)
    }

    @Test
    fun `should return LINEUP_OPEN fromValue lineupOpen`() {
        val type = UserNotificationType.fromValue("lineupOpen")
        assertEquals(UserNotificationType.LINEUP_OPEN, type)
    }

    @Test
    fun `should return RACE_WEEKEND_RESULTS_AVAILABLE fromValue raceWeekendResultsAvailable`() {
        val type = UserNotificationType.fromValue("raceWeekendResultsAvailable")
        assertEquals(UserNotificationType.RACE_WEEKEND_RESULTS_AVAILABLE, type)
    }

    @Test
    fun `should return UNKNOWN fromValue unknown`() {
        val type = UserNotificationType.fromValue("unknown")
        assertEquals(UserNotificationType.UNKNOWN, type)
    }

    @Test
    fun `should return UNKNOWN fromValue for invalid value`() {
        val type = UserNotificationType.fromValue("invalid")
        assertEquals(UserNotificationType.UNKNOWN, type)
    }
}
