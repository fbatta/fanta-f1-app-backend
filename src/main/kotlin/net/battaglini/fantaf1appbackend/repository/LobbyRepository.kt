package net.battaglini.fantaf1appbackend.repository

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.battaglini.fantaf1appbackend.model.Lobby
import org.springframework.stereotype.Repository
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
    private val firestoreInstance: Firestore
) {
    private val collectionName = "lobbies"

    /**
     * Retrieves a flow of lobbies for a specific year.
     *
     * @param year The year to filter lobbies by. Defaults to the current year in UTC.
     * @return A [Flow] emitting [Lobby] objects that match the specified year.
     */
    suspend fun getLobbies(
        year: Int = Clock.System.now().toLocalDateTime(
            TimeZone.UTC
        ).year
    ): Flow<Lobby> {
        val querySnapshot = withContext(Dispatchers.IO) {
            firestoreInstance.collection(collectionName).whereEqualTo(Lobby::year.name, year).get().get()
        }
        return querySnapshot.map { lobby -> lobby.toObject(Lobby::class.java) }.asFlow()
    }

    /**
     * Finds a lobby by its unique identifier.
     *
     * @param lobbyId The unique ID of the lobby.
     * @return The [Lobby] object if found, or null if no lobby exists with the given ID.
     */
    suspend fun findLobbyById(lobbyId: String): Lobby? {
        val snapshot = withContext(Dispatchers.IO) {
            firestoreInstance.collection(collectionName).document(lobbyId).get().get()
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
            firestoreInstance.collection(collectionName).whereEqualTo(Lobby::ownerId.name, ownerId).get().get()
        }
        return querySnapshot.map { lobby -> lobby.toObject(Lobby::class.java) }.asFlow()
    }
}