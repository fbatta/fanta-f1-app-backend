package net.battaglini.fantaf1appbackend.repository

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.battaglini.fantaf1appbackend.model.DriverSummary
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper

@Repository
class DriverSummaryRepository(
    private val firestore: Firestore,
    private val objectMapper: ObjectMapper
) {
    suspend fun createOrUpdateDriverSummary(driverSummary: DriverSummary) {
        withContext(Dispatchers.IO) {
            firestore.collection(COLLECTION_PATH).document(driverSummary.driverId).set(
                objectMapper.convertValue(
                    driverSummary,
                    Map::class.java
                ).orEmpty()
            ).get()
        }
    }

    companion object {
        private const val COLLECTION_PATH = "driver_summaries"
    }
}