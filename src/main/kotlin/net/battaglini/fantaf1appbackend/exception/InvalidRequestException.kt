package net.battaglini.fantaf1appbackend.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Request failed")
class InvalidRequestException(override val message: String) : RuntimeException(message)