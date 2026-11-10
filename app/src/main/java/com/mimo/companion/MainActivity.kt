package com.mimo.companion

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mimo.companion.domain.dialogue.Lang
import com.mimo.companion.ui.MimoRoot
import com.mimo.companion.ui.MimoTheme
import com.mimo.companion.ui.OnboardingScreen
import com.mimo.companion.util.LocaleHelper

class MainActivity : ComponentActivity() {

    // Tracks whether to show onboarding; survives recompositions but NOT recreate()
    // (after recreate the flag is re-read from Graph.onboarding which is already updated).
    private var showOnboarding by mutableStateOf(false)

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOnboarding = !Graph.onboarding.isDone()
        enableEdgeToEdge()
        setContent {
            MimoTheme {
                if (showOnboarding) {
                    OnboardingScreen { lang, cityId ->
                        finishOnboarding(lang, cityId)
                    }
                } else {
                    MimoRoot(activity = this)
                }
            }
        }
    }

    /**
     * Called from SettingsScreen when the user switches language.
     * Persists the choice and recreates the Activity so every string reloads.
     */
    fun applyLang(lang: Lang) {
        LocaleHelper.setLang(this, if (lang == Lang.AR) "ar" else "en")
        Graph.settings.update { it.copy(dialogue = it.dialogue.copy(lang = lang)) }
        recreate()
    }

    /**
     * Called by [OnboardingScreen] when the user taps "Start" or "Skip".
     * Saves the language, optionally enables prayer times with the chosen city,
     * marks onboarding done, and recreates the Activity so the new locale loads.
     */
    private fun finishOnboarding(lang: Lang, cityId: String?) {
        // 1. Language
        LocaleHelper.setLang(this, if (lang == Lang.AR) "ar" else "en")
        Graph.settings.update { it.copy(dialogue = it.dialogue.copy(lang = lang)) }

        // 2. City / prayer times
        if (cityId != null) {
            Graph.prayerSettings.update { it.copy(enabled = true, cityId = cityId) }
        }

        // 3. Mark done — must happen BEFORE recreate so onCreate reads isDone()=true
        Graph.onboarding.markDone()

        // 4. Reload UI with the chosen locale
        recreate()
    }
}
