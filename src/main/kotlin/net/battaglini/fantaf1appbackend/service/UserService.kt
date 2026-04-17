package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.Flow
import net.battaglini.fantaf1appbackend.model.User

interface UserService {
    suspend fun getUsersByLobbyId(lobbyId: String): Flow<User>
}