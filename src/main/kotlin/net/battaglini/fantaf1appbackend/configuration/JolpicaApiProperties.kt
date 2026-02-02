package net.battaglini.fantaf1appbackend.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jolpica")
data class JolpicaApiProperties(
    val baseUrl: String
)