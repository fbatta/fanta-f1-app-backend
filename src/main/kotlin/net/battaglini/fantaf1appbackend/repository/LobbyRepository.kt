package net.battaglini.fantaf1appbackend.repository

import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.battaglini.fantaf1appbackend.configuration.FirebaseProperties
import net.battaglini.fantaf1appbackend.model.Lobby
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper
import kotlin.time.Clock

/**
 * Repository for accessing Lobby data from Firestore.
 *
 * This repository provides methods to retrieve lobbies based on year, ID, or owner ID.
 *
 * @property firestoreInstance The Firestore instance used for database operations.
 */
@Repository
class LobbyRepository(
    private val firestoreInstance: Firestore,
    private val firebaseProperties: FirebaseProperties,
    private val objectMapper: ObjectMapper
) {
    /**
     * Retrieves a paginated flow of lobbies for a specific year.
     *
     * @param cursor The document snapshot to start after for pagination. If null, starts from the beginning.
     * @param year The year to filter lobbies by. Defaults to the current year in UTC.
     * @return A [Flow] emitting [Lobby] objects that match the specified year.
     */
    suspend fun getLobbies(
        cursor: DocumentSnapshot? = null,
        year: Int = Clock.System.now().toLocalDateTime(
            TimeZone.UTC
        ).year
    ): Flow<Pair<DocumentSnapshot, Lobby>> {
        val querySnapshot = withContext(Dispatchers.IO) {
            var query = firestoreInstance
                .collection(COLLECTION_PATH)
                .limit(firebaseProperties.firestore.pagination.queryLimit)

            cursor?.also { query = query.startAfter(cursor) }

            query.get().get()
        }
        return querySnapshot.map { documentSnapshot ->
            Pair(
                documentSnapshot,
                objectMapper.convertValue(documentSnapshot.data, Lobby::class.java)
            )
        }.asFlow()
    }

    /**
     * Finds a lobby by its unique identifier.
     *
     * @param lobbyId The unique ID of the lobby.
     * @return The [Lobby] object if found, or null if no lobby exists with the given ID.
     */
    suspend fun findLobbyById(lobbyId: String): Lobby? {
        val snapshot = withContext(Dispatchers.IO) {
            firestoreInstance.collection(COLLECTION_PATH).document(lobbyId).get().get()
        }
        if (!snapshot.exists()) {
            return null
        }
        return snapshot.toObject(Lobby::class.java)
    }

    /**
     * Gets all lobbies owned by a specific user.
     *
     * @param ownerId The unique ID of the owner.
     * @return A [Flow] emitting [Lobby] objects owned by the specified user.
     */
    suspend fun getLobbiesByOwnerId(ownerId: String): Flow<Lobby> {
        val querySnapshot = withContext(Dispatchers.IO) {
            firestoreInstance.collection(COLLECTION_PATH).whereEqualTo(Lobby::ownerId.name, ownerId).get().get()
        }
        return querySnapshot.map { lobby -> lobby.toObject(Lobby::class.java) }.asFlow()
    }

    companion object {
        private const val COLLECTION_PATH = "lobbies"
    }
}