package net.battaglini.fantaf1appbackend.repository

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.battaglini.fantaf1appbackend.model.RaceWeekendRecap
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper

@Repository
class RaceWeekendRecapRepository(
    private val firestore: Firestore,
    private val objectMapper: ObjectMapper
) {
    suspend fun saveRaceWeekendRecap(recap: RaceWeekendRecap) {
        withContext(Dispatchers.IO) {
            firestore.collection(COLLECTION_PATH).document(recap.raceId).set(
                objectMapper.convertValue(
                    recap,
                    Map::class.java
                ).orEmpty()
            ).get()
        }
    }

    companion object {
        private const val COLLECTION_PATH = "race_weekend_recaps"
    }
}
