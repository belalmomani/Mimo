package com.mimo.companion

import com.mimo.companion.domain.verse.DailyVerseRepository
import com.mimo.companion.domain.verse.DailyVerseStorage
import org.junit.Assert.*
import org.junit.Test

/** Fake storage for tests */
private class FakeStorage(
    private var saidDate: String? = null,
    private var nextIndex: Int = 0,
) : DailyVerseStorage {
    override fun getSaidDate() = saidDate
    override fun getNextIndex() = nextIndex
    override fun save(saidDate: String, nextIndex: Int) { this.saidDate = saidDate; this.nextIndex = nextIndex }
}

class DailyVerseRepositoryTest {

    private fun repo(storage: DailyVerseStorage = FakeStorage(), day: Long = 20_000L) =
        DailyVerseRepository(storage, clock = { day * 86_400_000L })

    @Test fun freshInstall_returnsVerse() {
        assertNotNull(repo().todayVerse("ar"))
    }

    @Test fun afterMarkSaid_returnsNull_sameDay() {
        val storage = FakeStorage()
        val r = repo(storage)
        r.todayVerse("ar")
        r.markSaidToday()
        assertNull(r.todayVerse("ar"))
    }

    @Test fun nextDay_returnsVerse_again() {
        val storage = FakeStorage()
        val r1 = repo(storage, day = 20_000L)
        r1.todayVerse("ar"); r1.markSaidToday()

        val r2 = repo(storage, day = 20_001L)   // next day
        assertNotNull(r2.todayVerse("ar"))
    }

    @Test fun verseText_notBlank() {
        val text = repo().todayVerse("ar")
        assertNotNull(text)
        assertTrue(text!!.isNotBlank())
    }

    @Test fun englishVerse_notBlank() {
        val text = repo().todayVerse("en")
        assertNotNull(text)
        assertTrue(text!!.isNotBlank())
    }

    @Test fun rotates_index_after_markSaid() {
        val storage = FakeStorage(nextIndex = 0)
        val r = repo(storage)
        r.markSaidToday()
        assertEquals(1, storage.getNextIndex())
    }

    @Test fun wraps_around_after_last_item() {
        // set index to last item
        val storage = FakeStorage(nextIndex = 9)   // 10 items in list, 0-based
        val r = repo(storage, day = 1L)
        r.markSaidToday()
        assertEquals(0, storage.getNextIndex())    // wraps to 0
    }

    @Test fun twoDifferentDays_differentVerses_possible() {
        // Just ensure no crash and content is non-null for two sequential days
        val storage = FakeStorage()
        val r1 = repo(storage, day = 100L); val t1 = r1.todayVerse("ar"); r1.markSaidToday()
        val r2 = repo(storage, day = 101L); val t2 = r2.todayVerse("ar")
        assertNotNull(t1); assertNotNull(t2)
    }
}
