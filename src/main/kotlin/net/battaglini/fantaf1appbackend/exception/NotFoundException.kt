package net.battaglini.fantaf1appbackend.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Resource not found")
class NotFoundException(override val message: String) : RuntimeException(message)