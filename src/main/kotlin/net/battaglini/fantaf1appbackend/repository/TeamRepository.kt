package net.battaglini.fantaf1appbackend.repository

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.withContext
import net.battaglini.fantaf1appbackend.model.Team

class TeamRepository(
    val defaultFirestoreInstance: Firestore
) {
    private val collectionName = "teams"

    suspend fun getTeamsByLobbyId(lobbyId: String): Flow<Team> {
        val querySnapshot = withContext(Dispatchers.IO) {
            defaultFirestoreInstance.collection(collectionName).whereEqualTo(Team::lobbyId.name, lobbyId).get().get()
        }
        return querySnapshot.map { team -> team.toObject(Team::class.java) }.asFlow()
    }
}