package net.battaglini.fantaf1appbackend.configuration

import com.google.genai.Client
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GenAIConfiguration {
    @Bean
    fun googleGenAIClient(properties: GoogleGenAIProperties): Client {
        return Client.builder()
            .apiKey(properties.apiKey)
            .build()
    }
}