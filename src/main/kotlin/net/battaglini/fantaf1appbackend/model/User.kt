package net.battaglini.fantaf1appbackend.model

data class User(
    val userId: String,
    val preferredName: String,
    val deviceRegistrationTokens: Map<String, String>,
    val avatarUrl: String,
)
