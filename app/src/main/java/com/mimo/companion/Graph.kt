package com.mimo.companion

import android.content.Context
import com.mimo.companion.ads.DisabledRewardedAdProvider
import com.mimo.companion.ads.RewardedAdProvider
import com.mimo.companion.ads.StartIoRewardedProvider
import com.mimo.companion.billing.BillingRepository
import com.mimo.companion.billing.NotConfiguredBillingRepository
import com.mimo.companion.data.db.MimoDatabase
import com.mimo.companion.data.db.RoomLedgerStorage
import com.mimo.companion.domain.DEFAULT_SKIN_ID
import com.mimo.companion.domain.PointsManager
import com.mimo.companion.domain.RewardConfig
import com.mimo.companion.domain.RewardedAdsManager
import com.mimo.companion.domain.SkinCatalog
import com.mimo.companion.domain.SkinUnlockManager
import com.mimo.companion.domain.dialogue.DialogueContext
import com.mimo.companion.domain.dialogue.DialogueDecision
import com.mimo.companion.domain.dialogue.DialogueManager
import com.mimo.companion.domain.dialogue.PhraseCategory
import com.mimo.companion.domain.custom.CustomPhrase
import com.mimo.companion.domain.custom.CustomPhraseRepository
import com.mimo.companion.domain.custom.PhraseEvent
import com.mimo.companion.domain.mood.MoodLevel
import com.mimo.companion.domain.mood.MoodRepository
import com.mimo.companion.mood.MoodStore
import com.mimo.companion.domain.weather.WeatherRepository
import com.mimo.companion.custom.CustomPhraseStore
import com.mimo.companion.settings.SettingsStore
import com.mimo.companion.weather.OWMClient
import com.mimo.companion.weather.WeatherStore
import com.mimo.companion.settings.PrayerSettingsStore
import com.mimo.companion.data.db.RoomPrayerStorage
import com.mimo.companion.domain.prayer.PrayerService
import com.mimo.companion.data.db.RoomReminderStorage
import com.mimo.companion.domain.reminders.ReminderManager
import com.mimo.companion.speech.SpeechController
import android.media.AudioManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/** Tiny manual DI container (no framework needed for a project this size). Created once in [MimoApp]. */
object Graph {
    lateinit var storage: RoomLedgerStorage; private set
    lateinit var points: PointsManager; private set
    lateinit var skins: SkinUnlockManager; private set
    lateinit var rewards: RewardedAdsManager; private set
    lateinit var adProvider: RewardedAdProvider; private set
    var billing: BillingRepository = NotConfiguredBillingRepository; private set

    lateinit var settings: SettingsStore; private set
    lateinit var onboarding: com.mimo.companion.onboarding.OnboardingStore; private set
    lateinit var reminders: ReminderManager; private set
    lateinit var prayer: PrayerService; private set
    lateinit var prayerSettings: PrayerSettingsStore; private set
    lateinit var customVoices: com.mimo.companion.speech.CustomVoiceStore; private set
    lateinit var speech: SpeechController; private set
    lateinit var steps: com.mimo.companion.steps.StepCounter; private set
    lateinit var stepActivity: com.mimo.companion.domain.steps.StepActivityTracker; private set
    lateinit var voiceInput: com.mimo.companion.voice.VoiceInputLauncher; private set
    lateinit var contextEngine: com.mimo.companion.context.ContextEngine; private set
    lateinit var focus: com.mimo.companion.focus.FocusManager; private set
    val pomodoro = com.mimo.companion.focus.PomodoroManager()
    lateinit var dailyVerse: com.mimo.companion.domain.verse.DailyVerseRepository; private set
    lateinit var weather: WeatherRepository; private set
    lateinit var weatherSettings: com.mimo.companion.settings.WeatherSettingsStore; private set
    lateinit var customPhrases: CustomPhraseRepository; private set
    lateinit var mood: MoodRepository; private set
    lateinit var water: com.mimo.companion.water.WaterTracker; private set
    lateinit var sleep: com.mimo.companion.sleep.SleepTracker; private set
    lateinit var dialogue: DialogueManager; private set
    private lateinit var appContext: Context
    fun appCtx(): Context = appContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var bubbleJob: Job? = null

    private val _bubble = MutableStateFlow<String?>(null)
    /** Text currently shown next to the companion (null = nothing). */
    val bubble: StateFlow<String?> get() = _bubble

    /** Edit [RewardConfig] defaults (or pass values here) to change points per ad, daily limit, interval, welcome bonus... */
    val rewardConfig = RewardConfig()

    private val _activeSkin = MutableStateFlow(DEFAULT_SKIN_ID)
    /** The look currently worn by the floating companion; the overlay service observes it. */
    val activeSkin: StateFlow<String> get() = _activeSkin

    fun init(context: Context) {
        appContext = context.applicationContext
        settings = SettingsStore(context)
        onboarding = com.mimo.companion.onboarding.OnboardingStore(context)
        speech = SpeechController(context).also { it.init() }
        dialogue = DialogueManager({ settings.value.dialogue })
        val db = MimoDatabase.build(context)
        storage = RoomLedgerStorage(db)
        reminders = ReminderManager(RoomReminderStorage(db))
        prayer = PrayerService(RoomPrayerStorage(db))
        prayerSettings = PrayerSettingsStore(context)
        customVoices = com.mimo.companion.speech.CustomVoiceStore(context)
        stepActivity = com.mimo.companion.domain.steps.StepActivityTracker()
        steps = com.mimo.companion.steps.StepCounter(context).also { sc ->
            sc.start()
            // Wire step updates into the activity tracker
            scope.launch {
                sc.state.collect { s ->
                    if (s.available && !s.permissionNeeded) {
                        stepActivity.onSteps(s.todaySteps)
                    }
                }
            }
        }
        voiceInput = com.mimo.companion.voice.VoiceInputLauncher(context)
        focus = com.mimo.companion.focus.FocusManager()
        dailyVerse = com.mimo.companion.domain.verse.DailyVerseRepository(
            com.mimo.companion.verse.DailyVerseStore(context)
        )
        weatherSettings = com.mimo.companion.settings.WeatherSettingsStore(context)
        customPhrases = CustomPhraseRepository(CustomPhraseStore(context))
        mood = MoodRepository(MoodStore(context))
        water = com.mimo.companion.water.WaterTracker(context)
        sleep = com.mimo.companion.sleep.SleepTracker(context)
        weather = WeatherRepository(WeatherStore(context)) { lat, lon, apiKey ->
            OWMClient.fetchBlocking(lat, lon, apiKey)   // called from IO thread via buildSnapshotAsync
        }
        contextEngine = com.mimo.companion.context.ContextEngine(
            context = appContext,
            stepCounter = steps,
            prayerSettings = prayerSettings,
            lang = { settings.value.dialogue.lang },
        )
        points = PointsManager(storage)
        skins = SkinUnlockManager(storage, points, SkinCatalog.ALL)
        rewards = RewardedAdsManager(storage, points, skins, rewardConfig)
        adProvider = if (BuildConfig.ADS_ENABLED) StartIoRewardedProvider(context.applicationContext).also { it.init() } else DisabledRewardedAdProvider
        // Wire Pomodoro callbacks → ميمو speaks
        pomodoro.onWorkStart   = { showScreenComment("🎯 يلا نركز! وقت العمل بدأ.") }
        pomodoro.onBreakStart  = { showScreenComment("☕ أحسنت! خذ راحة قصيرة.") }
        pomodoro.onSessionDone = { showScreenComment("✅ جولة كاملة، أنت رائع!") }
        startScheduleLoop()
    }

    /**
     * Checks every minute for SCHEDULE/SLEEP custom phrases whose time matches the current clock.
     * Fires at most once per minute; DialogueManager rate-limits are NOT applied here — the user
     * explicitly scheduled these phrases.
     */
    private fun startScheduleLoop() {
        scope.launch(Dispatchers.Default) {
            while (true) {
                delay(60_000L)  // check every minute
                if (!com.mimo.companion.overlay.FloatingService.running) continue
                val cal = java.util.Calendar.getInstance()
                val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
                val m = cal.get(java.util.Calendar.MINUTE)
                // SCHEDULE: exact hour:minute match
                val scheduled = customPhrases.scheduledFor(h, m)
                if (scheduled.isNotEmpty()) presentCustom(scheduled.random())
                // SLEEP: bedtime phrases
                val sleep = customPhrases.sleepPhrases().filter { it.scheduleHour == h && it.scheduleMin == m }
                if (sleep.isNotEmpty()) presentCustom(sleep.random())
                // MOOD: ask once a day at 20:00 if not logged yet
                if (h == 20 && m == 0 && !mood.loggedToday()) {
                    showScreenComment("كيف كان يومك اليوم؟ 😊 سجّل مزاجك من الإعدادات")
                }
                // SLEEP: bedtime and wake reminders
                if (h == sleep.bedtimeHour && m == sleep.bedtimeMin) {
                    showScreenComment("🌙 حان وقت النوم! استرح جيداً.")
                }
                if (h == sleep.wakeHour && m == sleep.wakeMin) {
                    showScreenComment("☀️ صباح الخير! يوم جديد مليء بالفرص.")
                }
                // QUOTE: share today's quote every morning at 8:00
                if (h == 8 && m == 0) {
                    val q = com.mimo.companion.quotes.QuoteBank.todayQuote()
                    showScreenComment("✨ ${q.text}")
                }
                // WATER: remind every hour (between 8am and 10pm) if goal not reached
                if (m == 0 && h in 8..22 && !water.goalReached()) {
                    val remaining = water.dailyGoal - water.glasses.value
                    showScreenComment("💧 لا تنسَ شرب الماء! باقي $remaining كوب لهدفك")
                }
            }
        }
    }

    /** Must be called off the main thread (reads the database). */
    fun loadActiveSkin() { _activeSkin.value = skins.activeSkinId() }
    fun publishActiveSkin(id: String) { _activeSkin.value = id }

    /** What we can reliably tell about the moment. Unknown things stay false (never guessed). */
    fun currentContext(): DialogueContext {
        val am = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val inCall = am.mode == AudioManager.MODE_IN_CALL || am.mode == AudioManager.MODE_IN_COMMUNICATION
        return DialogueContext(inCall = inCall)
    }

    /** Full context snapshot for the Context Engine (call status + focus + step + prayer + screen). */
    fun buildSnapshot(): com.mimo.companion.domain.context.ContextSnapshot {
        val am = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val inCall = am.mode == AudioManager.MODE_IN_CALL || am.mode == AudioManager.MODE_IN_COMMUNICATION
        val lang = settings.value.dialogue.lang.name.lowercase()
        return contextEngine.snapshot().copy(
            inCall = inCall,
            focusSession = focus.isActive,
            minutesSinceMove = stepActivity.minutesSinceMove(),
            dailyVerseText = dailyVerse.todayVerse(lang),
            weather = weather.cachedOrNull(),
        )
    }

    /**
     * Async variant: fetches fresh weather from the network if the cache is stale,
     * then returns a snapshot with up-to-date weather data.
     * Call this from the idle loop (background coroutine); never from the main thread.
     */
    suspend fun buildSnapshotAsync(): com.mimo.companion.domain.context.ContextSnapshot =
        withContext(Dispatchers.IO) {
            val ws = weatherSettings
            val weatherData = if (ws.enabled && ws.apiKey.isNotBlank()) {
                weather.current(ws.lat, ws.lon, ws.apiKey) // blocking fetch on IO thread
            } else null
            buildSnapshot().copy(weather = weatherData)
        }

    /**
     * The user tapped the companion.
     * Custom TAP phrases take priority over the built-in PhraseBank reply.
     */
    fun onTapped() {
        val custom = customPhrases.pickForTap()
        if (custom != null) {
            presentCustom(custom)
        } else {
            present(dialogue.reply())
        }
    }

    /**
     * Play a custom phrase: uses its linked clip (if any) or TTS.
     * Called for TAP, PRAYER, SLEEP, SCHEDULE events.
     */
    fun presentCustom(phrase: CustomPhrase) {
        val s = settings.value
        val clip = phrase.clipId?.let { cid -> customVoices.clips.value.firstOrNull { it.id == cid } }
        val tts = { speech.speak(phrase.text, s.dialogue.lang, s.speechRate) }
        val spoke = when {
            !s.voiceEnabled -> false
            clip != null    -> speech.playClip(clip.file, null, tts) // use clip only, TTS fallback
            else            -> tts()
        }
        if (s.showText) _bubble.value = phrase.text
        bubbleJob?.cancel()
        bubbleJob = scope.launch {
            if (spoke) { delay(400); while (speech.speaking.value) delay(150); delay(2_000) }
            else delay(3_500)
            _bubble.value = null
        }
    }

    /** Fire custom phrases tied to a [PhraseEvent] (prayer, sleep, schedule, reminder). */
    fun fireEvent(event: PhraseEvent) {
        val phrases = customPhrases.forEvent(event)
        if (phrases.isEmpty()) return
        scope.launch { presentCustom(phrases.random()) }
    }

    /** The app wants to speak on its own (greeting, suggestions...). Silent unless mode/quiet hours/limits allow it. */
    fun maybeSpeak(category: PhraseCategory) = present(dialogue.proactive(category, currentContext()))

    /**
     * Let the Context Engine choose the best proactive category and speak it
     * (if the DialogueManager allows it at this moment).
     * Call this from the overlay service's idle timer instead of [maybeSpeak].
     */
    fun maybeSpeakFromContext() {
        scope.launch { maybeSpeakFromContextAsync() }
    }

    private suspend fun maybeSpeakFromContextAsync() {
        val snapshot = buildSnapshotAsync()
        val category = com.mimo.companion.domain.context.ContextAdvisor.bestCategory(snapshot)
            ?: return // nothing relevant to say right now
        val ctx = DialogueContext(inCall = snapshot.inCall, focusSession = snapshot.focusSession)
        when (category) {
            PhraseCategory.DAILY_VERSE -> {
                val decision = dialogue.proactive(category, ctx)
                if (decision is com.mimo.companion.domain.dialogue.DialogueDecision.Say && snapshot.dailyVerseText != null) {
                    val fullText = "${decision.text}\n${snapshot.dailyVerseText}"
                    present(decision.copy(text = fullText))
                    dailyVerse.markSaidToday()
                }
            }
            PhraseCategory.WEATHER -> {
                val condition = snapshot.weatherCondition ?: return
                val decision = dialogue.proactiveWeather(condition, ctx)
                if (decision is com.mimo.companion.domain.dialogue.DialogueDecision.Say) {
                    val tempStr = snapshot.weather?.tempC?.let { it.roundToInt().toString() } ?: ""
                    val text = decision.text.replace("{temp}", tempStr)
                    present(decision.copy(text = text))
                }
            }
            else -> {
                val decision = dialogue.proactive(category, ctx)
                if (decision is com.mimo.companion.domain.dialogue.DialogueDecision.Say) {
                    present(decision)
                }
            }
        }
    }

    /**
     * Shows a short comment triggered by the screen-watch analyser.
     * Uses TTS only (no clip); respects quiet hours via the bubble timeout.
     */
    fun showScreenComment(text: String) {
        val s = settings.value
        if (!s.voiceEnabled && !s.showText) return
        if (s.voiceEnabled) speech.speak(text, s.dialogue.lang, s.speechRate)
        if (s.showText) _bubble.value = text
        bubbleJob?.cancel()
        bubbleJob = scope.launch {
            delay(5_000)
            _bubble.value = null
        }
    }

    /** The user confirmed a reminder: acknowledge briefly (if the companion is visible and allowed to speak). */
    fun onReminderDone() {
        if (!com.mimo.companion.overlay.FloatingService.running) return
        val custom = customPhrases.forEvent(PhraseEvent.REMINDER)
        if (custom.isNotEmpty()) presentCustom(custom.random())
        else present(dialogue.reply(PhraseCategory.ACK_DONE))
    }

    /**
     * The user logged their mood: ميمو reacts with an appropriate phrase.
     * Called from the IO thread (MoodScreen saves first, then calls this).
     */
    fun onMoodLogged(level: MoodLevel) {
        if (!com.mimo.companion.overlay.FloatingService.running) return
        val phrase = when (level) {
            MoodLevel.GREAT    -> listOf("رائع! يومك يبدو جميلاً 😄", "وااو، مزاجك عالي اليوم! 🎉").random()
            MoodLevel.GOOD     -> listOf("الحمد لله، يوم جيد 🙂", "ممتاز، استمر في يومك بإيجابية!").random()
            MoodLevel.OKAY     -> listOf("يوم عادي، لكن أنت تفعل رائعاً 💪", "كل يوم جديد فرصة جديدة 🌟").random()
            MoodLevel.TIRED    -> listOf("خذ راحة قصيرة، تستحق ذلك 😴", "أنت بذلت جهداً كبيراً، استرح قليلاً").random()
            MoodLevel.STRESSED -> listOf("خذ نفساً عميقاً... كل شيء سيكون بخير 🌸", "أنا هنا إذا احتجتني 💙").random()
            MoodLevel.SAD      -> listOf("أنا معك، الأمور ستتحسن 🤗", "لا بأس أن تشعر بالحزن أحياناً، أنا هنا 💙").random()
        }
        showScreenComment(phrase)
    }

    /** The user drank a glass of water — ميمو encourages. */
    fun onWaterDrunk(current: Int, goal: Int) {
        if (!com.mimo.companion.overlay.FloatingService.running) return
        val phrase = when {
            current >= goal -> "🎉 أحسنت! وصلت لهدفك اليوم، أنت رائع!"
            current == goal / 2 -> "💪 في المنتصف! استمر"
            else -> listOf("💧 ممتاز!", "أحسنت، كمّل!", "جسمك يشكرك 💙").random()
        }
        showScreenComment(phrase)
    }

    /** The user logged sleep — ميمو reacts based on duration. */
    fun onSleepLogged(hours: Float) {
        if (!com.mimo.companion.overlay.FloatingService.running) return
        val phrase = when {
            hours >= 8f -> "ممتاز! نمت ${hours.toInt()} ساعات، جسمك شكرك 😴✨"
            hours >= 6f -> "نمت ${hours.toInt()} ساعات، كافي لكن حاول تزيد قليلاً 🌙"
            hours > 0f  -> "نمت أقل من المثالي، حاول تنام مبكراً الليلة 💙"
            else        -> "لا تنسَ تسجيل نومك!"
        }
        showScreenComment(phrase)
    }

    /** Read a quote aloud using TTS. */
    fun speakQuote(quote: com.mimo.companion.quotes.Quote) {
        val s = settings.value
        if (!s.voiceEnabled) return
        val text = if (quote.author.isNotBlank()) "${quote.text} — ${quote.author}" else quote.text
        speech.speak(text, s.dialogue.lang, s.speechRate)
        if (s.showText) _bubble.value = quote.text
        bubbleJob?.cancel()
        bubbleJob = scope.launch {
            delay(400); while (speech.speaking.value) delay(150); delay(2_000)
            _bubble.value = null
        }
    }

    /** The user logged a prayer: short acknowledgement when the companion is visible. */
    fun onPrayerLogged() {
        if (!com.mimo.companion.overlay.FloatingService.running) return
        val custom = customPhrases.forEvent(PhraseEvent.PRAYER)
        if (custom.isNotEmpty()) presentCustom(custom.random())
        else present(dialogue.reply(PhraseCategory.PRAYER_ACK))
    }

    /** Text the user asked for (a reminder they set). Not filtered by initiative mode; callers decide about quiet hours. */
    fun announce(text: String, speak: Boolean, show: Boolean) {
        val s = settings.value
        val spoke = speak && s.voiceEnabled && voice(text, s)
        if (show && s.showText) _bubble.value = text
        bubbleJob?.cancel()
        bubbleJob = scope.launch {
            if (spoke) { delay(400); while (speech.speaking.value) delay(150); delay(2_000) } else delay(5_000)
            _bubble.value = null
        }
    }

    /** Voice-test button in settings. */
    fun testVoice() = present(dialogue.reply())

    /**
     * Voice output for built-in PhraseBank phrases.
     *
     * Custom clips are ONLY played through [presentCustom] where a specific phrase–clip pair is set.
     * This prevents a clip from "leaking" onto every random phrase in the bank.
     * The VoiceSource setting is preserved for legacy TTS-rate and speaking-style choices.
     */
    private fun voice(text: String, s: com.mimo.companion.domain.dialogue.CompanionSettings): Boolean {
        return speech.speak(text, s.dialogue.lang, s.speechRate)
    }

    private fun present(d: com.mimo.companion.domain.dialogue.DialogueDecision.Say) = present(d as DialogueDecision)

    private fun present(d: DialogueDecision) {
        if (d !is DialogueDecision.Say) return
        val s = settings.value
        val spoke = s.voiceEnabled && voice(d.text, s)
        if (s.showText) _bubble.value = d.text
        bubbleJob?.cancel()
        bubbleJob = scope.launch {
            // keep the text while the voice talks, then a short moment after; 3.5 s when silent
            if (spoke) { delay(400); while (speech.speaking.value) delay(150); delay(2_000) } else delay(3_500)
            _bubble.value = null
        }
    }
}
