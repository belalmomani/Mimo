package com.mimo.companion.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mimo.companion.MainActivity
import com.mimo.companion.R

object NotificationHelper {

    // ─── Channels ────────────────────────────────────────────────────────────
    const val CHANNEL_WATER    = "mimo_water"
    const val CHANNEL_SLEEP    = "mimo_sleep"
    const val CHANNEL_POMODORO = "mimo_pomodoro"
    const val CHANNEL_MOOD     = "mimo_mood"
    const val CHANNEL_QUOTE    = "mimo_quote"
    const val CHANNEL_POINTS   = "mimo_points"
    const val CHANNEL_FLOATING = "mimo_floating"   // خدمة الواجهة العائمة

    // ─── Notification IDs ────────────────────────────────────────────────────
    const val ID_WATER    = 100
    const val ID_SLEEP    = 101
    const val ID_POMODORO = 102
    const val ID_MOOD     = 103
    const val ID_QUOTE    = 104
    const val ID_POINTS   = 105

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        listOf(
            Triple(CHANNEL_WATER,    "تذكير الماء",        NotificationManager.IMPORTANCE_DEFAULT),
            Triple(CHANNEL_SLEEP,    "تذكير النوم",        NotificationManager.IMPORTANCE_HIGH),
            Triple(CHANNEL_POMODORO, "جلسة بومودورو",      NotificationManager.IMPORTANCE_HIGH),
            Triple(CHANNEL_MOOD,     "مزاجي اليومي",       NotificationManager.IMPORTANCE_DEFAULT),
            Triple(CHANNEL_QUOTE,    "اقتباس اليوم",       NotificationManager.IMPORTANCE_LOW),
            Triple(CHANNEL_POINTS,   "النقاط والمكافآت",   NotificationManager.IMPORTANCE_DEFAULT),
            Triple(CHANNEL_FLOATING, "Mimo – يعمل في الخلفية", NotificationManager.IMPORTANCE_LOW),
        ).forEach { (id, name, importance) ->
            nm.createNotificationChannel(NotificationChannel(id, name, importance))
        }
    }

    // ─── Water ───────────────────────────────────────────────────────────────
    fun notifyWater(context: Context, glasses: Int, goal: Int) {
        val remaining = goal - glasses
        val msg = when {
            remaining <= 0 -> "أحسنت! وصلت لهدفك اليومي من الماء 🎉"
            remaining == 1 -> "كوب واحد متبقٍّ لتكمل هدفك 💧"
            else           -> "اشرب ماءً! تبقّى $remaining أكواب من أصل $goal 💧"
        }
        show(context, CHANNEL_WATER, ID_WATER, "تذكير الماء", msg, openMainIntent(context))
    }

    // ─── Sleep ───────────────────────────────────────────────────────────────
    fun notifySleepBedtime(context: Context) {
        show(context, CHANNEL_SLEEP, ID_SLEEP,
            "وقت النوم 😴",
            "حان وقت النوم — استرح جيداً وأنهِ شاشاتك!",
            openMainIntent(context))
    }

    fun notifySleepWakeup(context: Context) {
        show(context, CHANNEL_SLEEP, ID_SLEEP,
            "صباح الخير ☀️",
            "وقت الاستيقاظ — يوم جديد وفرص جديدة!",
            openMainIntent(context))
    }

    // ─── Pomodoro ────────────────────────────────────────────────────────────
    fun notifyPomodoroWorkStart(context: Context, session: Int) {
        show(context, CHANNEL_POMODORO, ID_POMODORO,
            "ابدأ التركيز 🍅",
            "جلسة رقم $session — ركّز لمدة 25 دقيقة",
            openMainIntent(context))
    }

    fun notifyPomodoroBreak(context: Context, isLong: Boolean) {
        val (title, msg) = if (isLong)
            "استراحة طويلة ☕" to "أحسنت! خذ استراحة 15 دقيقة"
        else
            "استراحة قصيرة 🌿" to "5 دقائق راحة — ثم نكمل!"
        show(context, CHANNEL_POMODORO, ID_POMODORO, title, msg, openMainIntent(context))
    }

    fun notifyPomodoroDone(context: Context, totalSessions: Int) {
        show(context, CHANNEL_POMODORO, ID_POMODORO,
            "انتهت جلسات اليوم 🏆",
            "أكملت $totalSessions جلسة بومودورو اليوم — رائع!",
            openMainIntent(context))
    }

    // ─── Mood ────────────────────────────────────────────────────────────────
    fun notifyMoodReminder(context: Context) {
        show(context, CHANNEL_MOOD, ID_MOOD,
            "كيف حالك؟ 😊",
            "لم تسجّل مزاجك اليوم بعد — دقيقة واحدة فقط!",
            openMainIntent(context))
    }

    // ─── Quote ───────────────────────────────────────────────────────────────
    fun notifyDailyQuote(context: Context, quote: String) {
        show(context, CHANNEL_QUOTE, ID_QUOTE,
            "اقتباس اليوم ✨",
            quote,
            openMainIntent(context))
    }

    // ─── Points ──────────────────────────────────────────────────────────────
    fun notifyPointsEarned(context: Context, amount: Int, total: Int) {
        show(context, CHANNEL_POINTS, ID_POINTS,
            "ربحت $amount نقطة! 🌟",
            "رصيدك الآن $total نقطة — تحقق من المتجر!",
            openMainIntent(context))
    }

    fun notifyWelcomeBonus(context: Context, amount: Int) {
        show(context, CHANNEL_POINTS, ID_POINTS,
            "مرحباً بك في Mimo! 🎊",
            "حصلت على $amount نقطة كمكافأة ترحيب — افتح المتجر لاستكشاف الأشكال",
            openMainIntent(context))
    }

    // ─── Custom phrase ───────────────────────────────────────────────────────
    fun notifyCustomPhrase(context: Context, text: String) {
        show(context, CHANNEL_QUOTE, ID_QUOTE,
            "رسالة من Mimo 💌",
            text,
            openMainIntent(context))
    }

    // ─── Internal ────────────────────────────────────────────────────────────
    private fun show(
        context: Context,
        channelId: String,
        notifId: Int,
        title: String,
        message: String,
        intent: PendingIntent
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        nm.notify(notifId, notif)
    }

    private fun openMainIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
