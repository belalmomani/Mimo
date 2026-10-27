package com.mimo.companion

import android.app.Application
import kotlin.concurrent.thread

class MimoApp : Application() {
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
