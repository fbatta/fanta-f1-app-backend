package net.battaglini.fantaf1appbackend.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter
import org.springframework.security.web.server.SecurityWebFilterChain
import reactor.core.publisher.Flux

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity(useAuthorizationManager = true)
class WebfluxSecurityConfiguration {
    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers(
                        "/docs/**",
                        "/v1/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/swagger-ui/**"
                    ).permitAll()
                    .pathMatchers("/actuator/**").permitAll()
                    .anyExchange().authenticated()
            }
            .oauth2ResourceServer { resourceServerCustomizer ->
                resourceServerCustomizer.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
            }

        return http.build()
    }

    @Bean
    fun jwtAuthenticationConverter(): ReactiveJwtAuthenticationConverter {
        val converter = ReactiveJwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter(Converter<Jwt, Flux<GrantedAuthority>> { jwt ->
            val role = jwt.claims["user_role"] as? String
            if (role != null) {
                Flux.just(SimpleGrantedAuthority("ROLE_${role.uppercase()}"))
            } else {
                Flux.empty()
            }
        })
        return converter
    }
}
