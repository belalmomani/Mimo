package com.mimo.companion

import android.app.Application
import android.content.Context
import com.mimo.companion.util.LocaleHelper
import kotlin.concurrent.thread

class MimoApp : Application() {

    override fun attachBaseContext(base: Context) {
        // Apply the user's language choice before ANY resource is loaded.
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
        thread(name = "mimo-boot") {
            Graph.points.grantWelcomeBonus(Graph.rewardConfig.welcomeBonusPoints) // no-op after the first run
            Graph.loadActiveSkin()
            com.mimo.companion.reminders.ReminderAlarm.reschedule(this)
        }
    }
}
