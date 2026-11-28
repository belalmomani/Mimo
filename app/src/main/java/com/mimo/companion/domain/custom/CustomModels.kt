package com.mimo.companion.domain.custom

import java.util.UUID

enum class PhraseEvent { TAP, SCHEDULE, SLEEP, REMINDER, PRAYER }

data class CustomPhrase(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val event: PhraseEvent = PhraseEvent.TAP,
    val enabled: Boolean = true,
    val scheduledHour: Int = -1,
    val scheduledMinute: Int = -1,
    val clipId: String? = null
)
