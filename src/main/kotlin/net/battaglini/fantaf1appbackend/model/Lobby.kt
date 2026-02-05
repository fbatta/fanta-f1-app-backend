package net.battaglini.fantaf1appbackend.model

data class Lobby(
    val lobbyId: String,
    val lobbyName: String,
    val lobbyPassword: String,
    val ownerId: String,
    val year: Int
)
