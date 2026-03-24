package net.battaglini.fantaf1appbackend.repository

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.battaglini.fantaf1appbackend.model.RaceWeekendResult
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper

@Repository
class RaceWeekendResultRepository(
    private val firestoreInstance: Firestore,
    private val objectMapper: ObjectMapper
) {
    suspend fun saveRaceWeekendResult(raceWeekendResult: RaceWeekendResult) {
        withContext(Dispatchers.IO) {
            firestoreInstance.collection(COLLECTION_PATH)
                .document(raceWeekendResult.raceId)
                .create(objectMapper.convertValue(raceWeekendResult, Map::class.java))
                .get()
        }
    }

    suspend fun updateRaceWeekendResult(raceWeekendResult: RaceWeekendResult) {
        withContext(Dispatchers.IO) {
            val map: Map<String, Any> =
                objectMapper.convertValue(raceWeekendResult, Map::class.java) as Map<String, Any>
            firestoreInstance.collection(COLLECTION_PATH)
                .document(raceWeekendResult.raceId)
                .update(map)
                .get()
        }
    }

    suspend fun findRaceWeekendResult(raceId: String? = null, openF1MeetingKey: Int? = null): RaceWeekendResult? {
        if (raceId == null && openF1MeetingKey == null) {
            throw RaceWeekendResultRepositoryException("One of raceId or openF1MeetingKey are required")
        }

        try {
            var result: RaceWeekendResult? = null
            raceId?.also {
                val snapshot = withContext(Dispatchers.IO) {
                    firestoreInstance.collection(COLLECTION_PATH)
                        .document(raceId)
                        .get()
                        .get()
                }
                result = objectMapper.convertValue(snapshot.data, RaceWeekendResult::class.java)
            }
            openF1MeetingKey?.also {
                val querySnapshot = withContext(Dispatchers.IO) {
                    firestoreInstance.collection(COLLECTION_PATH)
                        .whereEqualTo(RaceWeekendResult::openF1MeetingKey.name, openF1MeetingKey)
                        .get()
                        .get()
                }
                result = querySnapshot.firstOrNull()?.data?.let {
                    objectMapper.convertValue(
                        it,
                        RaceWeekendResult::class.java
                    )
                }
            }
            return result
        } catch (e: NoSuchElementException) {
            return null
        }
    }

    companion object {
        const val COLLECTION_PATH = "race_weekend_results"

        class RaceWeekendResultRepositoryException(
            override val message: String? = null,
            override val cause: Throwable? = null
        ) : RuntimeException(message, cause)
    }
}