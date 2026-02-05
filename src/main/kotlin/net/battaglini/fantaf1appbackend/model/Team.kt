package net.battaglini.fantaf1appbackend.model

data class Team(
    val teamId: String,
    val teamName: String,
    val teamAvatarUrl: String,
    val ownerId: String,
    val lobbyId: String,
)
