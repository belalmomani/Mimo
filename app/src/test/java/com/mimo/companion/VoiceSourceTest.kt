package com.mimo.companion

import com.mimo.companion.domain.dialogue.ClipRotation
import com.mimo.companion.domain.dialogue.CompanionSettings
import com.mimo.companion.domain.dialogue.VoiceSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceSourceTest {
    @Test fun defaultKeepsBuiltInVoice() = assertEquals(VoiceSource.TTS_ONLY, CompanionSettings().voiceSource)

    @Test fun emptyListPicksNothing() = assertNull(ClipRotation.pick(emptyList<String>(), null) { 0 })

    @Test fun singleClipIsAlwaysPicked() = assertEquals("a", ClipRotation.pick(listOf("a"), "a") { 7 })

    @Test fun neverRepeatsTheSameClipWhenThereIsAChoice() {
        val clips = listOf("a", "b", "c")
        for (r in 0..50) assertNotEquals("b", ClipRotation.pick(clips, "b") { r })
    }

    @Test fun negativeRandomValuesStayInRange() = assertEquals("c", ClipRotation.pick(listOf("a", "b", "c"), null) { -1 })
}
