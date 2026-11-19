package com.mimo.companion.domain.verse

/**
 * Supplies one piece of Islamic content (Quran verse or hadith) per day.
 *
 * Design decisions:
 * - Offline-first: a built-in fallback list is always available.
 * - One delivery per calendar day: once the companion says it, [markSaidToday] is called
 *   and [todayVerse] returns null for the rest of the day.
 * - The companion says it via PhraseCategory.DAILY_VERSE; the text is injected into
 *   ContextSnapshot.dailyVerseText so the existing adviser/dialogue pipeline handles rate-limits.
 * - Network fetch (alquran.cloud or similar) can be wired in later; for now we use the
 *   built-in list only.
 *
 * Pure Kotlin — no Android imports.
 */
class DailyVerseRepository(
    private val storage: DailyVerseStorage,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    /**
     * Returns today's verse/hadith text if it hasn't been said yet today, or null otherwise.
     * Never returns the same item two days in a row (rotates through the list).
     */
    fun todayVerse(lang: String = "ar"): String? {
        val today = todayKey()
        if (storage.getSaidDate() == today) return null   // already said today
        val index = storage.getNextIndex() % CONTENT.size
        return if (lang == "en") CONTENT[index].en else CONTENT[index].ar
    }

    /** Call after the companion actually speaks the verse. */
    fun markSaidToday() {
        val today = todayKey()
        val next = (storage.getNextIndex() + 1) % CONTENT.size
        storage.save(saidDate = today, nextIndex = next)
    }

    private fun todayKey(): String {
        // Simple ISO-date key (UTC): YYYY-MM-DD
        val ms = clock()
        val days = ms / 86_400_000L
        // days since epoch → rough year/month/day (good enough for a daily key)
        return days.toString()   // unique per UTC day
    }

    // ── built-in content ─────────────────────────────────────────────────────

    private data class VerseItem(val ar: String, val en: String)

    private companion object {
        val CONTENT = listOf(
            VerseItem(
                "وَمَن يَتَوَكَّلْ عَلَى ٱللَّهِ فَهُوَ حَسْبُهُۥ — الطلاق: ٣",
                "Whoever relies upon Allah — then He is sufficient for him. (65:3)"
            ),
            VerseItem(
                "إِنَّ مَعَ ٱلْعُسْرِ يُسْرًا — الشرح: ٦",
                "Indeed, with hardship will be ease. (94:6)"
            ),
            VerseItem(
                "وَقُل رَّبِّ زِدْنِى عِلْمًا — طه: ١١٤",
                "And say: My Lord, increase me in knowledge. (20:114)"
            ),
            VerseItem(
                "فَإِذَا عَزَمْتَ فَتَوَكَّلْ عَلَى ٱللَّهِ — آل عمران: ١٥٩",
                "And when you have decided, then rely upon Allah. (3:159)"
            ),
            VerseItem(
                "ٱدْعُونِىٓ أَسْتَجِبْ لَكُمْ — غافر: ٦٠",
                "Call upon Me; I will respond to you. (40:60)"
            ),
            VerseItem(
                "خَيْرُ النَّاسِ أَنْفَعُهُمْ لِلنَّاسِ — رواه الطبراني",
                "The best of people are those most beneficial to others. (Al-Tabarani)"
            ),
            VerseItem(
                "الْمُؤْمِنُ الْقَوِيُّ خَيْرٌ وَأَحَبُّ إِلَى اللَّهِ مِنَ الْمُؤْمِنِ الضَّعِيفِ — رواه مسلم",
                "The strong believer is better and more beloved to Allah than the weak believer. (Muslim)"
            ),
            VerseItem(
                "مَنْ سَلَكَ طَرِيقًا يَلْتَمِسُ فِيهِ عِلْمًا سَهَّلَ اللَّهُ لَهُ طَرِيقًا إِلَى الْجَنَّةِ — رواه مسلم",
                "Whoever takes a path seeking knowledge, Allah will ease his way to Paradise. (Muslim)"
            ),
            VerseItem(
                "اتَّقِ اللَّهَ حَيْثُمَا كُنْتَ — رواه الترمذي",
                "Fear Allah wherever you are. (Al-Tirmidhi)"
            ),
            VerseItem(
                "إِنَّ اللَّهَ يُحِبُّ إِذَا عَمِلَ أَحَدُكُمْ عَمَلًا أَنْ يُتْقِنَهُ — رواه البيهقي",
                "Allah loves that when one of you does a task, he does it with excellence. (Al-Bayhaqi)"
            ),
        )
    }
}
