package net.battaglini.fantaf1appbackend.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "firebase")
data class FirebaseProperties(
    val appName: String,
    val projectId: String,
    val credentialsPath: String,
    val databaseId: String,
    val storageBucket: String,
    val firestore: FirestoreProperties
) {
    companion object {
        @ConfigurationProperties(prefix = "firebase.firestore")
        data class FirestoreProperties(
            val pagination: FirestorePaginationProperties
        )

        @ConfigurationProperties(prefix = "firebase.firestore.pagination")
        data class FirestorePaginationProperties(
            val queryLimit: Int = 25
        )
    }
}