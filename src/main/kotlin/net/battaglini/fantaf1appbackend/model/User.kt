package net.battaglini.fantaf1appbackend.model

data class User(
    val userId: String,
    val deviceRegistrationTokens: Map<String, String>,
)
