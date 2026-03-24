package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import net.battaglini.fantaf1appbackend.model.User
import net.battaglini.fantaf1appbackend.repository.TeamRepository
import net.battaglini.fantaf1appbackend.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val teamRepository: TeamRepository
) {
    suspend fun getUsersByLobbyId(lobbyId: String): Flow<User> {
        val teamsInLobby = teamRepository.getTeamsByLobbyId(lobbyId)

        val ownerIds = teamsInLobby.map { team ->
            team.ownerId
        }.toList()

        return userRepository.getUsersByIds(ownerIds)
    }
}