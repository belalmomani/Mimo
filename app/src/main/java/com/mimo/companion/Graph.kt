package com.mimo.companion

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import com.mimo.companion.ads.RewardedAdProvider
import com.mimo.companion.ads.StubRewardedAdProvider
import com.mimo.companion.domain.custom.CustomPhrase
import com.mimo.companion.domain.custom.CustomPhraseRepository
import com.mimo.companion.domain.custom.CustomPhraseStore
import com.mimo.companion.domain.custom.PhraseEvent
import com.mimo.companion.domain.dialogue.Dialogue
import com.mimo.companion.domain.mood.MoodLevel
import com.mimo.companion.domain.mood.MoodRepository
import com.mimo.companion.focus.PomodoroManager
import com.mimo.companion.mood.MoodStore
import com.mimo.companion.points.PointsRepository
import com.mimo.companion.notifications.NotificationHelper
import com.mimo.companion.quotes.Quote
import com.mimo.companion.quotes.QuoteBank
import com.mimo.companion.sleep.SleepTracker
import com.mimo.companion.store.StoreRepository
import com.mimo.companion.water.WaterTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.roundToInt

object Graph {

    // ── Core ─────────────────────────────────────────────────────────────────
    lateinit var appContext: Context
    lateinit var windowManager: WindowManager
    lateinit var tts: TextToSpeech
    lateinit var dialogue: Dialogue
    lateinit var customPhrases: CustomPhraseRepository

    // ── Feature trackers ─────────────────────────────────────────────────────
    lateinit var mood: MoodRepository
    lateinit var water: WaterTracker
    lateinit var sleep: SleepTracker
    val pomodoro = PomodoroManager()

    // ── Points & Store ────────────────────────────────────────────────────────
    lateinit var points: PointsRepository
    lateinit var storeRepo: StoreRepository
    var rewardedAds: RewardedAdProvider = StubRewardedAdProvider()

    // ── UI state ─────────────────────────────────────────────────────────────
    val bubbleText = mutableStateOf("")
    val isBubbleVisible = mutableStateOf(false)
    val characterExpression = mutableStateOf("neutral")

    // ── Window params ─────────────────────────────────────────────────────────
    var layoutParams: WindowManager.LayoutParams? = null
    var posX = 0
    var posY = 0

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true

        appContext = context.applicationContext
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        dialogue = Dialogue()
        customPhrases = CustomPhraseRepository(CustomPhraseStore(context))
        mood = MoodRepository(MoodStore(context))
        water = WaterTracker(context)
        sleep = SleepTracker(context)
        points = PointsRepository(context)
        storeRepo = StoreRepository(context, points)

        // إنشاء قنوات الإشعارات
        NotificationHelper.createChannels(appContext)

        // مكافأة ترحيبية
        val welcomed = points.giveWelcomeBonus()
        if (welcomed) {
            NotificationHelper.notifyWelcomeBonus(appContext, com.mimo.companion.points.PointsConfig.WELCOME_BONUS)
        }

        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = java.util.Locale("ar")
            }
        }

        // Pomodoro callbacks
        pomodoro.onWorkStart = { showScreenComment("ابدأ التركيز الآن، أنت قادر! 💪") }
        pomodoro.onBreakStart = { showScreenComment("وقت الراحة، خذ نفساً عميقاً 🌿") }
        pomodoro.onSessionDone = {
            showScreenComment("أحسنت! أكملت جلسة تركيز كاملة 🎉")
            fireEvent(PhraseEvent.REMINDER)
        }

        startScheduleLoop()
    }

    // ── Bubble / TTS ──────────────────────────────────────────────────────────

    fun showScreenComment(text: String) {
        scope.launch(Dispatchers.Main) {
            bubbleText.value = text
            isBubbleVisible.value = true
            voice(text)
            delay(6000)
            isBubbleVisible.value = false
        }
    }

    fun voice(text: String) {
        if (::tts.isInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun speakQuote(quote: Quote) {
        showScreenComment("\"${quote.text}\" — ${quote.author}")
    }

    // ── Interaction handlers ──────────────────────────────────────────────────

    fun onTapped() {
        val custom = customPhrases.pickForTap()
        if (custom != null) presentCustom(custom)
        else showScreenComment(dialogue.reply().text)
    }

    fun onMoodLogged(level: MoodLevel) {
        val phrase = when (level) {
            MoodLevel.GREAT    -> "رائع! يوم مميز بانتظارك 😄"
            MoodLevel.GOOD     -> "جيد جداً، استمر في تألقك 🙂"
            MoodLevel.OKAY     -> "يوم عادي، وهذا مقبول تماماً 😐"
            MoodLevel.TIRED    -> "خذ قسطاً من الراحة، أنت تستحقها 😴"
            MoodLevel.STRESSED -> "تنفّس ببطء، كل شيء سيكون بخير 😟"
            MoodLevel.SAD      -> "أنا هنا معك، الغد أفضل 😢"
        }
        showScreenComment(phrase)
    }

    fun onWaterDrunk(current: Int, goal: Int) {
        val phrase = if (current >= goal) "أحسنت! وصلت لهدف الماء اليومي 💧🎉"
                     else "كوب ${current} من ${goal}، استمر! 💧"
        showScreenComment(phrase)
    }

    fun onSleepLogged(hours: Int) {
        val phrase = when {
            hours >= 8 -> "نوم ممتاز! جسمك يشكرك 🌙✨"
            hours >= 6 -> "نوم جيد، حاول الوصول لـ 8 ساعات 🌙"
            else       -> "نوم قليل، اهتم بصحتك 😴"
        }
        showScreenComment(phrase)
    }

    // ── Custom Phrases ────────────────────────────────────────────────────────

    fun presentCustom(phrase: CustomPhrase) { showScreenComment(phrase.text) }

    fun fireEvent(event: PhraseEvent) {
        val phrases = customPhrases.allEnabled().filter { it.event == event }
        if (phrases.isNotEmpty()) presentCustom(phrases.random())
    }

    // ── Schedule Loop ─────────────────────────────────────────────────────────

    private fun startScheduleLoop() {
        scope.launch {
            while (true) {
                delay(60_000)
                val cal = Calendar.getInstance()
                val h = cal.get(Calendar.HOUR_OF_DAY)
                val m = cal.get(Calendar.MINUTE)

                // Custom SCHEDULE phrases
                val schedulePhrases = customPhrases.allEnabled()
                    .filter { it.event == PhraseEvent.SCHEDULE && it.scheduledHour == h && it.scheduledMinute == m }
                if (schedulePhrases.isNotEmpty()) {
                    withContext(Dispatchers.Main) { presentCustom(schedulePhrases.random()) }
                }

                // Custom SLEEP phrases (sleepPhrases ≠ Graph.sleep)
                val sleepPhrases = customPhrases.allEnabled().filter { it.event == PhraseEvent.SLEEP }
                if (sleepPhrases.isNotEmpty() && h == 22 && m == 0) {
                    withContext(Dispatchers.Main) { presentCustom(sleepPhrases.random()) }
                }

                // Sleep tracker reminders
                if (::sleep.isInitialized) {
                    if (h == sleep.bedtimeHour && m == sleep.bedtimeMin) {
                        withContext(Dispatchers.Main) { showScreenComment("حان وقت النوم 🌙") }
                        NotificationHelper.notifySleepBedtime(appContext)
                    }
                    if (h == sleep.wakeHour && m == sleep.wakeMin) {
                        withContext(Dispatchers.Main) { showScreenComment("صباح الخير! ☀️") }
                        NotificationHelper.notifySleepWakeup(appContext)
                    }
                }

                // Mood reminder 20:00
                if (h == 20 && m == 0) {
                    if (::mood.isInitialized && !mood.loggedToday()) {
                        withContext(Dispatchers.Main) { showScreenComment("كيف حالك اليوم؟ سجّل مزاجك 😊") }
                        NotificationHelper.notifyMoodReminder(appContext)
                    }
                }

                // Quote at 08:00
                if (h == 8 && m == 0) {
                    val q = QuoteBank.todayQuote()
                    withContext(Dispatchers.Main) { speakQuote(q) }
                    NotificationHelper.notifyDailyQuote(appContext, "\"${q.text}\" — ${q.author}")
                }

                // Water reminder hourly 08–21
                if (h in 8..21 && m == 0) {
                    if (::water.isInitialized && !water.goalReached()) {
                        val pct = (water.glasses.value * 100.0 / water.dailyGoal).roundToInt()
                        withContext(Dispatchers.Main) {
                            showScreenComment("لا تنسَ شرب الماء! أنجزت $pct% 💧")
                        }
                        NotificationHelper.notifyWater(appContext, water.glasses.value, water.dailyGoal)
                    }
                }
            }
        }
    }

    // ── Window helpers ────────────────────────────────────────────────────────

    fun defaultLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).also {
            it.gravity = Gravity.TOP or Gravity.START
            it.x = 0; it.y = 100
        }
    }
}
