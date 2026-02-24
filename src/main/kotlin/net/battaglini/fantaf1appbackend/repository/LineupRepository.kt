package net.battaglini.fantaf1appbackend.repository

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.battaglini.fantaf1appbackend.model.Lineup
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper

/**
 * Repository for accessing Lineup data from Firestore.
 *
 * This repository provides methods to retrieve and update team lineups for specific races.
 *
 * @property firestore The Firestore instance used for database operations.
 */
@Repository
class LineupRepository(
    private val firestore: Firestore,
    private val objectMapper: ObjectMapper
) {
    /**
     * Retrieves a lineup for a specific team and race.
     *
     * @param teamId The unique identifier of the team.
     * @param raceId The unique identifier of the race.
     * @return The [Lineup] object if found, or null if no lineup exists for the given team and race.
     */
    suspend fun getLineup(teamId: String, raceId: String): Lineup? {
        return withContext(Dispatchers.IO) {
            firestore
                .collection(COLLECTION_PATH)
                .whereEqualTo(
                    Lineup::teamId.name, teamId
                ).whereEqualTo(Lineup::raceId.name, raceId)
                .get().get()
        }.map { objectMapper.convertValue(it.data, Lineup::class.java) }.firstOrNull()
    }

    /**
     * Creates a new lineup or updates an existing one.
     *
     * This method uses the lineup's ID to identify the document in Firestore.
     * If a document with the same ID exists, it will be overwritten.
     * Otherwise, a new document will be created.
     *
     * @param lineup The [Lineup] object to be created or updated.
     */
    suspend fun createOrUpdateLineup(lineup: Lineup) {
        withContext(Dispatchers.IO) {
            firestore
                .collection(COLLECTION_PATH)
                .document(lineup.lineupId)
                .set(objectMapper.convertValue(lineup, Map::class.java))
        }
    }

    fun updateLineupInTransaction(lineup: Lineup, transaction: Transaction) {
        transaction.update(
            firestore
                .collection(COLLECTION_PATH)
                .document(lineup.lineupId),
            objectMapper.convertValue(lineup, Map::class.java) as Map<String, Any>
        );
    }

    companion object {
        private const val COLLECTION_PATH = "lineups"
    }
}