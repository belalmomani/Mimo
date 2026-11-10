package com.mimo.companion

import com.mimo.companion.domain.voice.VoiceInputHelper
import org.junit.Assert.*
import org.junit.Test

class VoiceInputHelperTest {

    // -- clean() --

    @Test fun clean_trimsLeadingWhitespace() {
        assertEquals("Hello", VoiceInputHelper.clean("  Hello"))
    }

    @Test fun clean_trimsTrailingWhitespace() {
        assertEquals("Hello", VoiceInputHelper.clean("Hello  "))
    }

    @Test fun clean_removesTrailingPeriod() {
        assertEquals("Take medicine", VoiceInputHelper.clean("Take medicine."))
    }

    @Test fun clean_uppercasesFirstChar() {
        assertEquals("Drink water", VoiceInputHelper.clean("drink water"))
    }

    @Test fun clean_emptyStringReturnsEmpty() {
        assertEquals("", VoiceInputHelper.clean(""))
    }

    @Test fun clean_whitespaceOnlyReturnsEmpty() {
        assertEquals("", VoiceInputHelper.clean("   "))
    }

    @Test fun clean_periodOnlyReturnsEmpty() {
        assertEquals("", VoiceInputHelper.clean("."))
    }

    @Test fun clean_alreadyCapitalizedUnchanged() {
        assertEquals("Study Arabic", VoiceInputHelper.clean("Study Arabic"))
    }

    @Test fun clean_multipleTrailingPeriodsRemovesAll() {
        // trimEnd('.') removes ALL trailing periods
        assertEquals("Done", VoiceInputHelper.clean("Done..."))
    }

    @Test fun clean_arabicTextCapitalizationSafe() {
        // Arabic has no concept of uppercase — clean() must not crash
        val arabic = "اشرب الماء"
        val result = VoiceInputHelper.clean(arabic)
        assertEquals(arabic, result) // no change, no crash
    }

    // -- isUsable() --

    @Test fun isUsable_singleChar_false() {
        assertFalse(VoiceInputHelper.isUsable("A"))
    }

    @Test fun isUsable_twoChars_true() {
        assertTrue(VoiceInputHelper.isUsable("Go"))
    }

    @Test fun isUsable_empty_false() {
        assertFalse(VoiceInputHelper.isUsable(""))
    }

    @Test fun isUsable_whitespaceOnly_false() {
        assertFalse(VoiceInputHelper.isUsable("  "))
    }

    @Test fun isUsable_periodOnly_false() {
        assertFalse(VoiceInputHelper.isUsable("."))
    }

    @Test fun isUsable_normalSentence_true() {
        assertTrue(VoiceInputHelper.isUsable("Call mum."))
    }
}
