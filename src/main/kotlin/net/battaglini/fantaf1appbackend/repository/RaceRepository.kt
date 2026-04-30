package net.battaglini.fantaf1appbackend.repository

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import net.battaglini.fantaf1appbackend.model.RaceWeekend
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper
import kotlin.time.Instant

@Repository
class RaceRepository(
    private val firestore: Firestore,
    private val objectMapper: ObjectMapper,
    private val timeZone: TimeZone
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

    suspend fun findNextRace(now: Instant): RaceWeekend? {
        return withContext(Dispatchers.IO) {
            val querySnapshot = firestore.collection(COLLECTION_PATH)
                .whereGreaterThan(RaceWeekend::dateEnd.name, now.toEpochMilliseconds())
                .orderBy(RaceWeekend::dateEnd.name)
                .limit(1)
                .get()
                .get()

            if (querySnapshot.isEmpty) {
                null
            } else {
                objectMapper.convertValue(querySnapshot.documents[0].data, RaceWeekend::class.java)
            }
        }
    }

    suspend fun getRacesByYear(year: Int): Flow<RaceWeekend> {
        val startOfYearTimestamp =
            LocalDateTime.parse("$year-01-01T00:00:00").toInstant(timeZone).toEpochMilliseconds()
        val endOfYearTimestamp =
            LocalDateTime.parse("$year-12-31T23:59:59").toInstant(timeZone).toEpochMilliseconds()
        return withContext(Dispatchers.IO) {
            firestore.collection(COLLECTION_PATH)
                .whereGreaterThanOrEqualTo(RaceWeekend::dateStart.name, startOfYearTimestamp)
                .whereLessThanOrEqualTo(RaceWeekend::dateEnd.name, endOfYearTimestamp)
                .get().get()
        }.map { objectMapper.convertValue(it.data, RaceWeekend::class.java) }.asFlow()
    }

    suspend fun getRaceById(raceId: String): Flow<RaceWeekend> {
        return withContext(Dispatchers.IO) {
            val document = firestore.collection(COLLECTION_PATH).document(raceId).get().get()
            if (document.exists()) {
                flow {
                    emit(objectMapper.convertValue(document.data, RaceWeekend::class.java))
                }
            } else {
                emptyFlow()
            }
        }
    }

    companion object {
        private const val COLLECTION_PATH = "races"
    }
}