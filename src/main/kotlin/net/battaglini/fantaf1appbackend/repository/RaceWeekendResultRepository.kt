package net.battaglini.fantaf1appbackend.repository

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import org.springframework.stereotype.Repository

@Repository
class RaceWeekendResultRepository(
    val firestoreInstance: Firestore
) {
    suspend fun saveRaceWeekendResult(raceWeekendResult: RaceWeekendResult) {
        withContext(Dispatchers.IO) {
            firestoreInstance.collection(COLLECTION_PATH)
                .document(raceWeekendResult.raceId)
                .set(raceWeekendResult)
                .get()
        }
    }

    suspend fun updateRaceWeekendResult(raceWeekendResult: RaceWeekendResult) {
        withContext(Dispatchers.IO) {
            val snapshot = firestoreInstance.collection(COLLECTION_PATH)
                .document(raceWeekendResult.raceId)
                .get()
                .get()
            if (!snapshot.exists()) {
                throw RaceWeekendResultRepositoryException("Race weekend result not found for raceId=${raceWeekendResult.raceId}")
            }
            val existing = snapshot.toObject(RaceWeekendResult::class.java)
                ?: throw RaceWeekendResultRepositoryException("Race weekend result not found for raceId=${raceWeekendResult.raceId}")

            if (existing.version >= raceWeekendResult.version) {
                throw RaceWeekendResultRepositoryException("The new version number is not greater than the existing version number for raceId=${raceWeekendResult.raceId}")
            }

            snapshot.reference.set(raceWeekendResult).get()
        }
    }

    suspend fun getRaceWeekendResult(raceId: String? = null, openF1MeetingKey: Int? = null): RaceWeekendResult? {
        if (raceId == null && openF1MeetingKey == null) {
            throw RaceWeekendResultRepositoryException("One of raceId or openF1MeetingKey are required")
        }
        var result: RaceWeekendResult? = null
        raceId?.also {
            val snapshot = withContext(Dispatchers.IO) {
                firestoreInstance.collection(COLLECTION_PATH)
                    .document(raceId)
                    .get()
                    .get()
            }
            if (!snapshot.exists()) {
                throw RaceWeekendResultRepositoryException("Race weekend result not found for raceId=$raceId")
            }
            result = snapshot.toObject(RaceWeekendResult::class.java)
                ?: throw RaceWeekendResultRepositoryException("Race weekend result not found for raceId=$raceId")
        }
        openF1MeetingKey?.also {
            val querySnapshot = withContext(Dispatchers.IO) {
                firestoreInstance.collection(COLLECTION_PATH)
                    .whereEqualTo(RaceWeekendResult::openF1MeetingKey.name, openF1MeetingKey)
                    .get()
                    .get()
            }
            result = querySnapshot.first().toObject(RaceWeekendResult::class.java)
        }
        return result
    }

    companion object {
        const val COLLECTION_PATH = "/race_weekend_results"

        class RaceWeekendResultRepositoryException(
            override val message: String? = null,
            override val cause: Throwable? = null
        ) : RuntimeException(message, cause)
    }
}