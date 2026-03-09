package net.battaglini.fantaf1appbackend.exception

class DriverNotFoundException(override val message: String) : RuntimeException(message) {
}