package net.battaglini.fantaf1appbackend.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "firebase")
data class FirebaseProperties(
    val appName: String,
    val projectId: String,
    val credentialsPath: String,
    val databaseId: String,
    val storageBucket: String
)