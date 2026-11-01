package com.mimo.companion

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mimo.companion.domain.dialogue.Lang
import com.mimo.companion.ui.MimoRoot
import com.mimo.companion.ui.MimoTheme
import com.mimo.companion.util.LocaleHelper

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MimoTheme { MimoRoot(activity = this) } }
    }

    /**
     * Called from SettingsScreen when the user switches language.
     * Persists the choice and recreates the Activity so every string reloads.
     */
    fun applyLang(lang: Lang) {
        LocaleHelper.setLang(this, if (lang == Lang.AR) "ar" else "en")
        // Also update the dialogue setting so the companion speaks the right language.
        Graph.settings.update { it.copy(dialogue = it.dialogue.copy(lang = lang)) }
        recreate()
    }
}
