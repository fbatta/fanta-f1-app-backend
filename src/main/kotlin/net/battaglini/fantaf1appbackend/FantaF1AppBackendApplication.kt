package net.battaglini.fantaf1appbackend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.actuate.web.reactive.ReactiveManagementWebSecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration

@SpringBootApplication(exclude = [ReactiveWebSecurityAutoConfiguration::class, ReactiveManagementWebSecurityAutoConfiguration::class])
@ConfigurationPropertiesScan("net.battaglini.fantaf1appbackend.configuration")
class FantaF1AppBackendApplication

fun main(args: Array<String>) {
    runApplication<FantaF1AppBackendApplication>(*args)
}
