package net.battaglini.fantaf1appbackend.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Request failed")
class InternalServerException(override val message: String) : RuntimeException(message)