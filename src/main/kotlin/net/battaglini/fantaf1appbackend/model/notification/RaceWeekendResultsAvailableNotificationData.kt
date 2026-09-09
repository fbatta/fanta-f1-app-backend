package net.battaglini.fantaf1appbackend.model.notification

data class RaceWeekendResultsAvailableNotificationData(
    val raceId: String,
    val teamId: String,
    val lobbyId: String,
    val raceName: String,
    override val notificationId: String
) : BaseNotificationData(notificationId) {
    fun toMap(): Map<String, String> {
        return mapOf(
            "raceId" to raceId,
            "teamId" to teamId,
            "lobbyId" to lobbyId,
            "raceName" to raceName,
            "notificationId" to notificationId
        )
    }
}
