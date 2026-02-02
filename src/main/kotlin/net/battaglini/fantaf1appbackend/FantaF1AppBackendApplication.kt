package net.battaglini.fantaf1appbackend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan("net.battaglini.fantaf1appbackend.configuration")
class FantaF1AppBackendApplication

fun main(args: Array<String>) {
    runApplication<FantaF1AppBackendApplication>(*args)
}
