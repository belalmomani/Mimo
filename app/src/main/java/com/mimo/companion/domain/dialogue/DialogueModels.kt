package com.mimo.companion.domain.dialogue

/** 1 = quiet (never starts a conversation), 2 = social, 3 = custom (per-category switches). */
enum class InitiativeMode { QUIET, SOCIAL, CUSTOM }

enum class Dialect { NEUTRAL, JORDANIAN }

enum class Lang { AR, EN }

enum class PhraseCategory {
    /** The user tapped the companion: always allowed to answer. */
    TAP_REPLY,
    /** Opening line when the companion is shown. */
    GREETING,
    /** Short friendly remark, proactive. */
    IDLE_CHAT,
    /** Suggests a pause after long phone use (only when the Context Engine has real data). */
    BREAK_SUGGESTION,
    /** Short encouragement, proactive. */
    ENCOURAGE,
    /** Reply after the user marked something as done. */
    ACK_DONE,
    /** Reply after the user logged a prayer. */
    PRAYER_ACK,
}

data class Phrase(
    val id: String,
    val category: PhraseCategory,
    val ar: String,
    val en: String,
    /** null = fits every dialect; JORDANIAN phrases are used only when the user chose that dialect. */
    val dialect: Dialect? = null,
) {
    fun text(lang: Lang) = if (lang == Lang.AR) ar else en
}

data class DialogueSettings(
    val mode: InitiativeMode = InitiativeMode.QUIET,
    /** In CUSTOM mode only these proactive categories may start a conversation. */
    val allowedProactive: Set<PhraseCategory> = emptySet(),
    val dialect: Dialect = Dialect.NEUTRAL,
    val lang: Lang = Lang.AR,
    /** Quiet hours as minutes since midnight; equal start/end = disabled. May wrap midnight (e.g. 22:00-07:00). */
    val quietStartMin: Int = 23 * 60,
    val quietEndMin: Int = 7 * 60,
    val maxProactivePerHour: Int = 3,
    val minSecondsBetweenProactive: Int = 20 * 60,
) {
    init {
        require(quietStartMin in 0..1439 && quietEndMin in 0..1439)
        require(maxProactivePerHour >= 0 && minSecondsBetweenProactive >= 0)
    }
}

/** What the rest of the app knows right now. Unknown = false: the engine never guesses. */
data class DialogueContext(
    val inCall: Boolean = false,
    val focusSession: Boolean = false,
    /** User opted out of interruptions during focus sessions. */
    val respectFocus: Boolean = true,
)

/** All user-tunable companion behaviour (voice + dialogue). Persisted by settings/SettingsStore. */
data class CompanionSettings(
    val voiceEnabled: Boolean = true,
    val showText: Boolean = true,
    /** TextToSpeech rate, 0.5 .. 1.5 (1.0 = normal). */
    val speechRate: Float = 1.0f,
    val dialogue: DialogueSettings = DialogueSettings(),
    val voiceSource: VoiceSource = VoiceSource.TTS_ONLY,
) {
    init { require(speechRate in 0.5f..1.5f) }
}
