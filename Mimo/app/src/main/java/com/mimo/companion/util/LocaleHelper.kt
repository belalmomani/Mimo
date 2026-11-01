package com.mimo.companion.util

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/**
 * Applies the user-chosen language to a Context and persists the choice.
 *
 * Usage:
 *   - In Application.attachBaseContext  → LocaleHelper.wrap(base)
 *   - In Activity.attachBaseContext     → LocaleHelper.wrap(base)
 *   - To change language at run-time    → LocaleHelper.setLang(context, "ar") then recreate()
 */
object LocaleHelper {
    private const val PREFS = "locale_pref"
    private const val KEY = "lang"

    /** Returns the persisted language code, defaulting to "ar" (Arabic). */
    fun getSavedLang(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "ar") ?: "ar"

    /** Persists [lang] ("ar" or "en") and returns a locale-wrapped Context. */
    fun setLang(context: Context, lang: String): Context {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, lang).apply()
        return applyLocale(context, lang)
    }

    /** Wraps [base] with the persisted language — call from attachBaseContext. */
    fun wrap(base: Context): Context = applyLocale(base, getSavedLang(base))

    private fun applyLocale(context: Context, lang: String): Context {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }
}
