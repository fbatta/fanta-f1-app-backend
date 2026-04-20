package net.battaglini.fantaf1appbackend.repository

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class LineupNotificationRepository(
    private val firestore: Firestore
) {
    suspend fun isLineupOpenNotificationSent(raceId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val docRef = firestore.collection(COLLECTION_PATH).document("lineup_open_$raceId")
            docRef.get().get().exists()
        }
    }

    suspend fun isLineupCloseReminderSent(raceId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val docRef = firestore.collection(COLLECTION_PATH).document("lineup_close_reminder_$raceId")
            docRef.get().get().exists()
        }
    }

    suspend fun isLineupClosedNotificationSent(raceId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val docRef = firestore.collection(COLLECTION_PATH).document("lineup_closed_$raceId")
            docRef.get().get().exists()
        }
    }

    suspend fun markLineupOpenNotificationAsSent(raceId: String) {
        withContext(Dispatchers.IO) {
            val docRef = firestore.collection(COLLECTION_PATH).document("lineup_open_$raceId")
            val data = mapOf(
                "raceId" to raceId,
                "sentAt" to Instant.now().toEpochMilli(),
                "notificationType" to "LINEUP_OPEN"
            )
            docRef.set(data).get()
        }
    }

    suspend fun markLineupCloseReminderAsSent(raceId: String) {
        withContext(Dispatchers.IO) {
            val docRef = firestore.collection(COLLECTION_PATH).document("lineup_close_reminder_$raceId")
            val data = mapOf(
                "raceId" to raceId,
                "sentAt" to Instant.now().toEpochMilli(),
                "notificationType" to "LINEUP_CLOSE_REMINDER"
            )
            docRef.set(data).get()
        }
    }

    suspend fun markLineupClosedNotificationAsSent(raceId: String) {
        withContext(Dispatchers.IO) {
            val docRef = firestore.collection(COLLECTION_PATH).document("lineup_closed_$raceId")
            val data = mapOf(
                "raceId" to raceId,
                "sentAt" to Instant.now().toEpochMilli(),
                "notificationType" to "LINEUP_CLOSED"
            )
            docRef.set(data).get()
        }
    }

    companion object {
        private const val COLLECTION_PATH = "lineup_notifications"
    }
}
