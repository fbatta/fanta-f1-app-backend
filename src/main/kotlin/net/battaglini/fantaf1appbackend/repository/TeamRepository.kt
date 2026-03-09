package net.battaglini.fantaf1appbackend.repository

import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import com.google.cloud.firestore.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.withContext
import net.battaglini.fantaf1appbackend.configuration.FirebaseProperties
import net.battaglini.fantaf1appbackend.model.Team
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper

@Repository
class TeamRepository(
    private val firestore: Firestore,
    private val firebaseProperties: FirebaseProperties,
    private val objectMapper: ObjectMapper
) {
    private val collectionName = "teams"

    suspend fun getAllTeams(cursor: DocumentSnapshot?): Flow<Pair<DocumentSnapshot, Team>> {
        val querySnapshot = withContext(Dispatchers.IO) {
            var query = firestore
                .collection(collectionName)
                .orderBy(Team::teamName.name, Query.Direction.ASCENDING)
                .limit(firebaseProperties.firestore.pagination.queryLimit)

            cursor?.let { query = query.startAfter(cursor) }

            query.get().get()
        }

        return querySnapshot.map { documentSnapshot ->
            Pair(
                documentSnapshot,
                objectMapper.convertValue(documentSnapshot.data, Team::class.java)
            )
        }.asFlow()
    }

    suspend fun getTeamsByLobbyId(lobbyId: String): Flow<Team> {
        val querySnapshot = withContext(Dispatchers.IO) {
            firestore.collection(collectionName).whereEqualTo(Team::lobbyId.name, lobbyId).get().get()
        }
        return querySnapshot.map { team -> team.toObject(Team::class.java) }.asFlow()
    }

    fun updateTeamInTransaction(team: Team, transaction: Transaction) {
        transaction.update(
            firestore.collection(collectionName).document(team.teamId),
            objectMapper.convertValue(team, Map::class.java) as Map<String, Any>
        )
    }
}