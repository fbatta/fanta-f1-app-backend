package net.battaglini.fantaf1appbackend.repository

import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.withContext
import net.battaglini.fantaf1appbackend.configuration.FirebaseProperties
import net.battaglini.fantaf1appbackend.model.Team
import org.springframework.stereotype.Repository

@Repository
class TeamRepository(
    private val firestoreInstance: Firestore,
    private val firebaseProperties: FirebaseProperties
) {
    private val collectionName = "teams"

    suspend fun getAllTeams(cursor: DocumentSnapshot?): Flow<Pair<DocumentSnapshot, Team>> {
        val querySnapshot = withContext(Dispatchers.IO) {
            var query = firestoreInstance
                .collection(collectionName)
                .orderBy(Team::teamName.name, Query.Direction.ASCENDING)
                .limit(firebaseProperties.firestore.pagination.queryLimit)

            cursor?.let { query = query.startAfter(cursor) }

            query.get().get()
        }

        return querySnapshot.map { documentSnapshot ->
            Pair(
                documentSnapshot,
                documentSnapshot.toObject(Team::class.java)
            )
        }.asFlow()
    }

    suspend fun getTeamsByLobbyId(lobbyId: String): Flow<Team> {
        val querySnapshot = withContext(Dispatchers.IO) {
            firestoreInstance.collection(collectionName).whereEqualTo(Team::lobbyId.name, lobbyId).get().get()
        }
        return querySnapshot.map { team -> team.toObject(Team::class.java) }.asFlow()
    }
}