package net.battaglini.fantaf1appbackend.repository

import com.google.cloud.firestore.Firestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.battaglini.fantaf1appbackend.model.User
import org.springframework.stereotype.Repository

@Repository
class UserRepository(
    private val firestore: Firestore,
    private val firebaseAuth: FirebaseAuth
) {
    suspend fun getUser(userId: String): Pair<UserRecord, User>? {
        return withContext(Dispatchers.IO) {
            val userRecord = firebaseAuth.getUserAsync(userId).get()
            val user = firestore.collection(COLLECTION_PATH)
                .document(userId)
                .get().get()
                .toObject(User::class.java)

            user?.let { Pair(userRecord, user) }
        }
    }

    companion object {
        private const val COLLECTION_PATH = "users"
    }
}