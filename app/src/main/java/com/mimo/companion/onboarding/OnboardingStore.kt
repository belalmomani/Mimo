package com.mimo.companion.onboarding

import android.content.Context

/**
 * Tracks whether the user has completed the first-run onboarding wizard.
 *
 * A single boolean flag in SharedPreferences — nothing else.
 * Call [markDone] after the last onboarding step; call [isDone] in
 * MainActivity to decide whether to show the wizard or the main UI.
 */
class OnboardingStore(context: Context) {
    private val p = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)

    fun isDone(): Boolean = p.getBoolean("done", false)

    fun markDone() { p.edit().putBoolean("done", true).apply() }

    /** For testing / "reset onboarding" in debug builds. */
    fun reset() { p.edit().remove("done").apply() }
}
