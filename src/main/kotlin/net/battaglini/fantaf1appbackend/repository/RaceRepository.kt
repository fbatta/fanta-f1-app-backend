package net.battaglini.fantaf1appbackend.repository

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
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

    suspend fun getRacesByYear(year: Int): Flow<RaceWeekend> {
        val startOfYearTimestamp =
            LocalDateTime.parse("$year-01-01T00:00:00").toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val endOfYearTimestamp =
            LocalDateTime.parse("$year-12-31T23:59:59").toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        return withContext(Dispatchers.IO) {
            firestore.collection(COLLECTION_PATH)
                .whereGreaterThanOrEqualTo(RaceWeekend::dateStart.name, startOfYearTimestamp)
                .whereLessThanOrEqualTo(RaceWeekend::dateEnd.name, endOfYearTimestamp)
                .get().get()
        }.map { objectMapper.convertValue(it, RaceWeekend::class.java) }.asFlow()
    }

    companion object {
        private const val COLLECTION_PATH = "races"
    }
}