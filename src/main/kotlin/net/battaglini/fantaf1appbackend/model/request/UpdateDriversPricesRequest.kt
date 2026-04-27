package net.battaglini.fantaf1appbackend.model.request

data class UpdateDriversPricesRequest(
    val acronyms: List<String>? = null,
    val updateAllDrivers: Boolean = false
)
