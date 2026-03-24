package net.battaglini.fantaf1appbackend.repository

import com.google.cloud.firestore.Firestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.withContext
import net.battaglini.fantaf1appbackend.model.User
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper

@Repository
class UserRepository(
    private val firestore: Firestore,
    private val firebaseAuth: FirebaseAuth,
    private val objectMapper: ObjectMapper
) {
    suspend fun findUser(userId: String): Pair<UserRecord, User>? {
        return withContext(Dispatchers.IO) {
            val userRecord = firebaseAuth.getUserAsync(userId).get()
            val user = firestore.collection(COLLECTION_PATH)
                .document(userId)
                .get().get()
                .toObject(User::class.java)

            user?.let { Pair(userRecord, user) }
        }
    }

    suspend fun getUsersByIds(userIds: List<String>): Flow<User> {
        return withContext(Dispatchers.IO) {
            firestore.collection(COLLECTION_PATH)
                .whereIn("userId", userIds)
                .get().get()
                .map { objectMapper.convertValue(it.data, User::class.java) }
                .asFlow()
        }
    }

    companion object {
        private const val COLLECTION_PATH = "users"
    }
}