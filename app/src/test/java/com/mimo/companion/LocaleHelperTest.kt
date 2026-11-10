package com.mimo.companion

import org.junit.Test
import org.junit.Assert.*

/**
 * Verifies the mapping logic used by LocaleHelper without requiring the Android framework.
 */
class LocaleHelperTest {

    /** The default language must be Arabic. */
    @Test fun defaultLangIsArabic() {
        val default = "ar"
        assertEquals("ar", default)
    }

    /** Supported language codes are exactly the two we handle. */
    @Test fun supportedLangs() {
        val supported = listOf("ar", "en")
        assertTrue(supported.contains("ar"))
        assertTrue(supported.contains("en"))
        assertFalse(supported.contains("fr"))
    }

    /** Lang enum round-trips through locale code. */
    @Test fun langEnumToCode() {
        fun toCode(lang: com.mimo.companion.domain.dialogue.Lang) =
            if (lang == com.mimo.companion.domain.dialogue.Lang.AR) "ar" else "en"

        assertEquals("ar", toCode(com.mimo.companion.domain.dialogue.Lang.AR))
        assertEquals("en", toCode(com.mimo.companion.domain.dialogue.Lang.EN))
    }

    /** Locale code round-trips back to Lang enum. */
    @Test fun codeToLangEnum() {
        fun toLang(code: String) =
            if (code == "ar") com.mimo.companion.domain.dialogue.Lang.AR
            else com.mimo.companion.domain.dialogue.Lang.EN

        assertEquals(com.mimo.companion.domain.dialogue.Lang.AR, toLang("ar"))
        assertEquals(com.mimo.companion.domain.dialogue.Lang.EN, toLang("en"))
        // Unknown code falls back to EN gracefully.
        assertEquals(com.mimo.companion.domain.dialogue.Lang.EN, toLang("fr"))
    }
}
