package net.battaglini.fantaf1appbackend.service

import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import net.battaglini.fantaf1appbackend.repository.TeamRepository
import net.battaglini.fantaf1appbackend.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class UserServiceTest {

    @MockK
    lateinit var userRepository: UserRepository

    @MockK
    lateinit var teamRepository: TeamRepository

    @InjectMockKs
    lateinit var userService: UserServiceImpl

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `getUsersByLobbyId should return users for teams in the lobby`() = runTest {
        val teams = listOf(
            TestFactories.createTeam(teamId = "team1", ownerId = "user1", lobbyId = "lobby1"),
            TestFactories.createTeam(teamId = "team2", ownerId = "user2", lobbyId = "lobby1")
        )
        val users = listOf(
            TestFactories.createUser(userId = "user1"),
            TestFactories.createUser(userId = "user2")
        )

        coEvery { teamRepository.getTeamsByLobbyId("lobby1") } returns flowOf(teams[0], teams[1])
        coEvery { userRepository.getUsersByIds(listOf("user1", "user2")) } returns flowOf(users[0], users[1])

        val result = userService.getUsersByLobbyId("lobby1").toList()

        assertEquals(2, result.size)
        assertTrue(result.any { it.userId == "user1" })
        assertTrue(result.any { it.userId == "user2" })
        coVerify { teamRepository.getTeamsByLobbyId("lobby1") }
        coVerify { userRepository.getUsersByIds(listOf("user1", "user2")) }
    }

    @Test
    fun `getUsersByLobbyId should return emptyFlow when lobby has no teams`() = runTest {
        coEvery { teamRepository.getTeamsByLobbyId("empty_lobby") } returns emptyFlow()
        // When no teams, ownerIds is empty list, so getUsersByIds is called with []
        coEvery { userRepository.getUsersByIds(emptyList()) } returns emptyFlow()

        val result = userService.getUsersByLobbyId("empty_lobby").toList()

        assertTrue(result.isEmpty())
        coVerify { teamRepository.getTeamsByLobbyId("empty_lobby") }
        coVerify { userRepository.getUsersByIds(emptyList()) }
    }

    @Test
    fun `getUsersByLobbyId should collect owner IDs from multiple teams and return all users`() = runTest {
        val teams = listOf(
            TestFactories.createTeam(teamId = "team1", ownerId = "owner1", lobbyId = "lobby1"),
            TestFactories.createTeam(teamId = "team2", ownerId = "owner2", lobbyId = "lobby1"),
            TestFactories.createTeam(teamId = "team3", ownerId = "owner3", lobbyId = "lobby1")
        )
        val users = listOf(
            TestFactories.createUser(userId = "owner1"),
            TestFactories.createUser(userId = "owner2"),
            TestFactories.createUser(userId = "owner3")
        )

        coEvery { teamRepository.getTeamsByLobbyId("lobby1") } returns flowOf(teams[0], teams[1], teams[2])
        coEvery { userRepository.getUsersByIds(listOf("owner1", "owner2", "owner3")) } returns flowOf(
            users[0], users[1], users[2]
        )

        val result = userService.getUsersByLobbyId("lobby1").toList()

        assertEquals(3, result.size)
        val userIds = result.map { it.userId }
        assertTrue(userIds.contains("owner1"))
        assertTrue(userIds.contains("owner2"))
        assertTrue(userIds.contains("owner3"))
    }

    @Test
    fun `getUsersByLobbyId should pass duplicate owner IDs to repository without deduplication`() = runTest {
        // Two teams share the same owner — service does not deduplicate
        val teams = listOf(
            TestFactories.createTeam(teamId = "team1", ownerId = "owner1", lobbyId = "lobby1"),
            TestFactories.createTeam(teamId = "team2", ownerId = "owner1", lobbyId = "lobby1")
        )

        coEvery { teamRepository.getTeamsByLobbyId("lobby1") } returns flowOf(teams[0], teams[1])
        coEvery { userRepository.getUsersByIds(listOf("owner1", "owner1")) } returns flowOf(TestFactories.createUser(userId = "owner1"))

        val result = userService.getUsersByLobbyId("lobby1").toList()

        assertEquals(1, result.size)
        // Service passes duplicate owner IDs to repository as-is
        coVerify {
            userRepository.getUsersByIds(withArg { ids ->
                assertEquals(2, ids.size)
                assertEquals("owner1", ids[0])
                assertEquals("owner1", ids[1])
            })
        }
    }

    @Test
    fun `getUsersByLobbyId should pass owner IDs to repository even when no users found`() = runTest {
        val team = TestFactories.createTeam(teamId = "team1", ownerId = "nonexistent", lobbyId = "lobby1")

        coEvery { teamRepository.getTeamsByLobbyId("lobby1") } returns flowOf(team)
        coEvery { userRepository.getUsersByIds(listOf("nonexistent")) } returns emptyFlow()

        val result = userService.getUsersByLobbyId("lobby1").toList()

        assertTrue(result.isEmpty())
        coVerify {
            userRepository.getUsersByIds(withArg { ids ->
                assertEquals(1, ids.size)
                assertEquals("nonexistent", ids[0])
            })
        }
    }
}
