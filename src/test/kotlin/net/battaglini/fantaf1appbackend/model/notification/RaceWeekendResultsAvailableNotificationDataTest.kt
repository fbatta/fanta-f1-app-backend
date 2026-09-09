package net.battaglini.fantaf1appbackend.model.notification

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RaceWeekendResultsAvailableNotificationDataTest {

    @Test
    fun `toMap should return map with all notification fields`() {
        val data = RaceWeekendResultsAvailableNotificationData(
            raceId = "race-1",
            teamId = "team-1",
            lobbyId = "lobby-1",
            raceName = "Australian Grand Prix",
            notificationId = "notif-uuid"
        )

        val map = data.toMap()

        assertEquals(5, map.size)
        assertEquals("race-1", map["raceId"])
        assertEquals("team-1", map["teamId"])
        assertEquals("lobby-1", map["lobbyId"])
        assertEquals("Australian Grand Prix", map["raceName"])
        assertEquals("notif-uuid", map["notificationId"])
    }

    @Test
    fun `should inherit from BaseNotificationData`() {
        val data = RaceWeekendResultsAvailableNotificationData(
            raceId = "race-1",
            teamId = "team-1",
            lobbyId = "lobby-1",
            raceName = "Australian Grand Prix",
            notificationId = "notif-uuid"
        )

        val baseNotification: BaseNotificationData = data
        assertEquals("notif-uuid", baseNotification.notificationId)
    }
}
