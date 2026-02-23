package net.battaglini.fantaf1appbackend.repository

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.battaglini.fantaf1appbackend.model.RaceWeekend
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper

@Repository
class RaceRepository(
    private val firestore: Firestore,
    private val objectMapper: ObjectMapper
) {
    suspend fun createOrUpdateRaces(races: List<RaceWeekend>) {
        withContext(Dispatchers.IO) {
            firestore.runTransaction { transaction ->
                races.forEach { race ->
                    val reference = firestore.collection(COLLECTION_PATH).document(race.raceId)
                    transaction.set(reference, objectMapper.convertValue(race, Map::class.java))
                }
            }.get()
        }
    }

    companion object {
        private const val COLLECTION_PATH = "races"
    }
}