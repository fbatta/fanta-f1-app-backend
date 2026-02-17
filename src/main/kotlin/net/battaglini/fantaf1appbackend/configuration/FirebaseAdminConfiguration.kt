package net.battaglini.fantaf1appbackend.configuration

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.cloud.FirestoreClient
import com.google.firebase.messaging.FirebaseMessaging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.io.ClassPathResource

@Configuration
class FirebaseAdminConfiguration(
    val firebaseProperties: FirebaseProperties
) {
    @Bean
    fun serviceAccountCredentials(): GoogleCredentials {
        val resource = ClassPathResource(firebaseProperties.credentialsPath)
        return GoogleCredentials.fromStream(resource.inputStream)
    }

    @Bean
    @Primary
    fun defaultFirebaseApp(serviceAccountCredentials: GoogleCredentials): FirebaseApp {
        val firestoreOptions = FirestoreOptions.newBuilder()
            .setDatabaseId(firebaseProperties.databaseId)
            .build()

        val firebaseOptions = FirebaseOptions.builder()
            .setFirestoreOptions(firestoreOptions)
            .setStorageBucket(firebaseProperties.storageBucket)
            .setCredentials(serviceAccountCredentials)
            .build()

        return FirebaseApp.initializeApp(firebaseOptions)
    }

    @Bean
    @Primary
    fun defaultFirestoreInstance(defaultFirebaseApp: FirebaseApp): Firestore {
        return FirestoreClient.getFirestore(defaultFirebaseApp)
    }

    @Bean
    @Primary
    fun defaultMessagingInstance(defaultFirebaseApp: FirebaseApp): FirebaseMessaging {
        return FirebaseMessaging.getInstance(defaultFirebaseApp)
    }

    @Bean
    @Primary
    fun defaultAuthInstance(defaultFirebaseApp: FirebaseApp): FirebaseAuth {
        return FirebaseAuth.getInstance(defaultFirebaseApp)
    }
}