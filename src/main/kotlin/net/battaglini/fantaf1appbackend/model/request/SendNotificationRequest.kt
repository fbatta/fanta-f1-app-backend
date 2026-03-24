package net.battaglini.fantaf1appbackend.model.request

import net.battaglini.fantaf1appbackend.enums.UserNotificationType
import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.databind.annotation.JsonSerialize

data class SendNotificationRequest(
    @JsonSerialize(using = UserNotificationType.Companion.Serializer::class)
    @JsonDeserialize(using = UserNotificationType.Companion.Deserializer::class)
    val type: UserNotificationType,
    val raceId: String?
)
