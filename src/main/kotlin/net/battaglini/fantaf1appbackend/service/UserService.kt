package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.Flow
import net.battaglini.fantaf1appbackend.model.User

/**
 * Service responsible for managing user-related data and operations.
 */
interface UserService {
    /**
     * Retrieves all users who are participating in a specific lobby.
     *
     * @param lobbyId The unique identifier of the lobby.
     * @return A [Flow] of [User] objects.
     */
    suspend fun getUsersByLobbyId(lobbyId: String): Flow<User>
}