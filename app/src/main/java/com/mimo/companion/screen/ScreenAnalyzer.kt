package com.mimo.companion.screen

import android.view.WindowManager

/**
 * Analyses a captured screen JPEG and returns a short comment for ميمو to say.
 *
 * This is intentionally rule-based and LOCAL — no image is sent to any server.
 * Future versions may add on-device ML (e.g., Google ML Kit image labelling).
 *
 * Currently implemented rules (heuristic, based on file size patterns):
 *  • Very small image (almost blank / lock screen) → encourage the user to put the phone down.
 *  • Large image (rich content, likely video or game) → suggest a break.
 *  • Medium image (normal use) → random positive comment or null (silent).
 *
 * Returns null to stay silent; non-null strings are spoken by the companion.
 */
object ScreenAnalyzer {

    // Arabic phrases for each scenario (kept short for the bubble)
    private val blankScreenPhrases = listOf(
        "يبدو الشاشة فارغة، هل تريد أخذ استراحة؟ 😊",
        "وقت مناسب لتمدد قليلاً 🧘",
    )

    private val heavyUsePhrases = listOf(
        "لاحظت أنك منشغل! لا تنسَ تريّح عيونك كل فترة 👀",
        "خذ نفساً عميقاً، أنت تفعل رائعاً 💪",
    )

    private val normalPhrases = listOf(
        "أنا هنا إذا احتجتني 😊",
        "كل شيء تمام؟",
    )

    /**
     * @param jpeg   Compressed JPEG bytes of the screen capture.
     * @param wm     WindowManager — reserved for future use (density / rotation checks).
     * @return Short Arabic comment, or null to stay silent.
     */
    fun analyze(jpeg: ByteArray, wm: WindowManager): String? {
        val kb = jpeg.size / 1024
        return when {
            // Very small = blank / static screen
            kb < 15  -> if (shouldSpeak()) blankScreenPhrases.random() else null
            // Large = video, game, heavy graphics
            kb > 200 -> if (shouldSpeak()) heavyUsePhrases.random() else null
            // Normal use — speak rarely (1 in 4 chance to avoid being annoying)
            else     -> if (shouldSpeak(probability = 0.25)) normalPhrases.random() else null
        }
    }

    // Simple probabilistic gate so the companion doesn't speak every 30s
    private fun shouldSpeak(probability: Double = 0.5) = Math.random() < probability
}
