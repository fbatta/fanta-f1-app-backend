package net.battaglini.fantaf1appbackend.repository

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.withContext
import net.battaglini.fantaf1appbackend.enums.RaceWeekendSessionType
import net.battaglini.fantaf1appbackend.model.DriverResult
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper

@Repository
class DriverResultRepository(
    val firestoreInstance: Firestore,
    private val objectMapper: ObjectMapper
) {
    suspend fun <K : DriverResult> saveDriverResults(driverResults: List<K>) {
        withContext(Dispatchers.IO) {
            firestoreInstance.runTransaction { transaction ->
                driverResults.forEach { driverResult ->
                    val docReference = firestoreInstance.collection(COLLECTION_PATH)
                        .document(driverResult.raceId)
                        .collection(driverResult.sessionType.name)
                        .document(driverResult.driverId)
                    transaction.set(docReference, objectMapper.convertValue(driverResult, Map::class.java))
                }
            }.get()
        }
    }

    private final suspend inline fun <reified K : DriverResult> getResultsForSession(
        raceId: String,
        sessionType: RaceWeekendSessionType
    ): Flow<K> {
        val querySnapshot = withContext(Dispatchers.IO) {
            firestoreInstance.collection(COLLECTION_PATH).document(raceId)
                .collection(sessionType.name).get().get()
        }
        return querySnapshot.map { objectMapper.convertValue(it.data, K::class.java) }.asFlow()
    }

    companion object {
        const val COLLECTION_PATH = "driver_results"
    }
}