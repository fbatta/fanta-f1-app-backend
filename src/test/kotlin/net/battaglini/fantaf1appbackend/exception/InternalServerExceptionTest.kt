package net.battaglini.fantaf1appbackend.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

class InternalServerExceptionTest {

    @Test
    fun `should create exception with message and have ResponseStatus annotation`() {
        val message = "Internal error occurred"
        val exception = InternalServerException(message)

        assertEquals(message, exception.message)

        val annotation = InternalServerException::class.java.getAnnotation(ResponseStatus::class.java)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, annotation?.value)
        assertEquals("Request failed", annotation?.reason)
    }
}
