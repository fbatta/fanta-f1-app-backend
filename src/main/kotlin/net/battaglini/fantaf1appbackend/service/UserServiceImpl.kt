package net.battaglini.fantaf1appbackend.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import net.battaglini.fantaf1appbackend.model.User
import net.battaglini.fantaf1appbackend.repository.TeamRepository
import net.battaglini.fantaf1appbackend.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val teamRepository: TeamRepository
) : UserService {
    override suspend fun getUsersWithTeamIdByLobbyId(lobbyId: String): Flow<Pair<String, User>> {
        val teamsInLobby = teamRepository.getTeamsByLobbyId(lobbyId).toList()

        val ownerIds = teamsInLobby.map { team ->
            team.ownerId
        }

        val users = userRepository.getUsersByIds(ownerIds)
        return users.map { user ->
            val team = teamsInLobby.first { it.ownerId == user.userId }
            Pair(team.teamId, user)
        }
    }
}