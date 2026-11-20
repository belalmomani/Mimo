package com.mimo.companion.domain.dialogue

import com.mimo.companion.domain.dialogue.Dialect.JORDANIAN
import com.mimo.companion.domain.dialogue.PhraseCategory.ACK_DONE
import com.mimo.companion.domain.dialogue.PhraseCategory.BREAK_SUGGESTION
import com.mimo.companion.domain.dialogue.PhraseCategory.DAILY_VERSE
import com.mimo.companion.domain.dialogue.PhraseCategory.ENCOURAGE
import com.mimo.companion.domain.dialogue.PhraseCategory.GREETING
import com.mimo.companion.domain.dialogue.PhraseCategory.IDLE_CHAT
import com.mimo.companion.domain.dialogue.PhraseCategory.MOVE_REMINDER
import com.mimo.companion.domain.dialogue.PhraseCategory.PRAYER_ACK
import com.mimo.companion.domain.dialogue.PhraseCategory.TAP_REPLY
import com.mimo.companion.domain.dialogue.PhraseCategory.WEATHER

/**
 * Adult, natural tone - no childish forms. Suggestions are phrased as possibilities, never as claims about
 * the user's body or whereabouts. The companion does not claim real feelings.
 * Add phrases freely: ids must stay unique (checked at startup).
 */
object PhraseBank {
    private fun p(id: String, c: PhraseCategory, ar: String, en: String, d: Dialect? = null) = Phrase(id, c, ar, en, d)

    val ALL: List<Phrase> = listOf(
        p("tap1", TAP_REPLY, "أنا هنا.", "I'm here."),
        p("tap2", TAP_REPLY, "تفضّل، كيف أساعدك؟", "Go ahead, how can I help?"),
        p("tap3", TAP_REPLY, "أنا موجود إذا احتجتني.", "I'm around if you need me."),
        p("tap4", TAP_REPLY, "شو بدك نعمل؟", "What shall we do?", JORDANIAN),
        p("tap5", TAP_REPLY, "تمام، سامعك.", "Sure, I'm listening.", JORDANIAN),
        p("tap6", TAP_REPLY, "لحظة وأكون معك.", "One moment, I'm with you."),

        p("gr1", GREETING, "أهلًا، رجعنا.", "Hi, we're back."),
        p("gr2", GREETING, "مرحبًا، يومك سعيد.", "Hello, hope your day is going well."),
        p("gr3", GREETING, "يا هلا، شو الأخبار؟", "Hey, what's new?", JORDANIAN),
        p("gr4", GREETING, "أهلين، أنا جاهز.", "Hey, I'm ready.", JORDANIAN),

        p("ic1", IDLE_CHAT, "شو رأيك نغيّر جو اليوم؟", "How about a change of scenery today?", JORDANIAN),
        p("ic2", IDLE_CHAT, "إذا بتحب، فينا نرتب مهامك لليوم.", "If you like, we can sort out today's tasks.", JORDANIAN),
        p("ic3", IDLE_CHAT, "هل تريد أن نرتّب مهام اليوم؟", "Would you like to organise today's tasks?"),
        p("ic4", IDLE_CHAT, "أنا موجود إذا احتجتني.", "I'm here whenever you need me."),
        p("ic5", IDLE_CHAT, "ربما حان وقت كوب ماء.", "Maybe it's time for a glass of water."),
        p("ic6", IDLE_CHAT, "لو عندك وقت، جرّب شكلًا جديدًا من المتجر.", "If you have a minute, try a new look from the store."),

        p("bk1", BREAK_SUGGESTION, "صار لك وقت طويل على الهاتف، شو رأيك تريّح شوي؟", "You've been on the phone a while, how about a short break?", JORDANIAN),
        p("bk2", BREAK_SUGGESTION, "مضى وقت على استخدام الهاتف. ما رأيك باستراحة قصيرة؟", "It's been a while on the phone. How about a short break?"),
        p("bk3", BREAK_SUGGESTION, "ربما تفيدك استراحة قصيرة للعينين.", "A short rest for your eyes might help."),
        p("bk4", BREAK_SUGGESTION, "ما سجّلنا حركة تُذكر من فترة، شو رأيك تتمشى شوي؟", "We haven't noticed much movement for a while, fancy a short walk?", JORDANIAN),

        p("en1", ENCOURAGE, "خطوة خطوة، وبتوصل.", "One step at a time.", JORDANIAN),
        p("en2", ENCOURAGE, "أنت تتقدّم، استمر.", "You're making progress, keep going."),
        p("en3", ENCOURAGE, "وقت مناسب لإنجاز مهمة صغيرة.", "A good moment for one small task."),

        // ── Move reminder ─────────────────────────────────────────────────────
        p("mv1", MOVE_REMINDER, "ما سجّلنا حركة من فترة، شو رأيك تتمشى دقيقتين؟", "No movement in a while — how about a two-minute walk?", JORDANIAN),
        p("mv2", MOVE_REMINDER, "صار لك وقت من دون تحرّك. مشي قصير يفيدك.", "You've been still for a while. A short walk would do you good."),
        p("mv3", MOVE_REMINDER, "الحركة مهمة. بضع خطوات؟", "Movement matters. A few steps?"),
        p("mv4", MOVE_REMINDER, "لاحظنا ما في خطوات من فترة. شو رأيك تقوم شوي؟", "No steps for a while. How about getting up for a bit?", JORDANIAN),

        // ── Daily verse (intro line; full text injected by Graph.present) ────
        p("dv1", DAILY_VERSE, "آية اليوم:", "Today's verse:"),
        p("dv2", DAILY_VERSE, "حديث اليوم:", "Today's hadith:"),

        // ── Weather — {temp} replaced at runtime with rounded °C value ───────
        // HOT
        p("wt_hot1", WEATHER, "الجو حار اليوم، {temp} درجة. لا تنسَ تشرب ماء كافي.", "It's hot today, {temp}°C. Don't forget to stay hydrated."),
        p("wt_hot2", WEATHER, "جو حار هالأيام. اشرب ماي وابقَ بالظل لو قدرت.", "Hot weather today. Drink water and stay in the shade if you can.", JORDANIAN),
        // COLD
        p("wt_cold1", WEATHER, "الطقس بارد اليوم، {temp} درجة. ارتدِ ما يدفئك.", "Cold today, {temp}°C. Bundle up."),
        p("wt_cold2", WEATHER, "برد هالأيام. خلّي معك كنزة.", "It's cold. Keep a jacket with you.", JORDANIAN),
        // RAINY
        p("wt_rain1", WEATHER, "في مطر اليوم. خذ معك مظلة إن خرجت.", "Rain today — grab an umbrella if you're heading out."),
        p("wt_rain2", WEATHER, "يوم ممطر. المشي تحت المطر منيح للعقل لو ما عندك وين تروح.", "Rainy day. A walk in light rain is great if you don't mind getting damp.", JORDANIAN),
        // STORMY
        p("wt_storm1", WEATHER, "جو عاصف اليوم. حاول تقلّل الخروج غير الضروري.", "Stormy weather today. Best to stay in unless you really need to head out."),
        p("wt_storm2", WEATHER, "في عواصف. خلّي نفسك بأمان اليوم.", "Storm outside. Take care today.", JORDANIAN),
        // SNOWY
        p("wt_snow1", WEATHER, "في ثلج اليوم. الطرق قد تكون زلقة، انتبه.", "Snowy today. Roads may be slippery — take care."),
        p("wt_snow2", WEATHER, "الثلج نازل. لبّس وخذ خطواتك بحذر.", "It's snowing. Dress warm and watch your step.", JORDANIAN),
        // FOGGY
        p("wt_fog1", WEATHER, "يوم ضبابي. القيادة بالضباب تحتاج تركيز.", "Foggy day. Drive carefully if you're heading out."),
        p("wt_fog2", WEATHER, "ضباب كثير اليوم. خلّي الضوء شاغل لو بتسوق.", "Heavy fog today. Keep your lights on if you're driving.", JORDANIAN),
        // SUNNY
        p("wt_sun1", WEATHER, "الجو صافي اليوم، {temp} درجة. وقت مناسب لبضع خطوات بالخارج.", "Clear skies today, {temp}°C. A nice time for a few steps outside."),
        p("wt_sun2", WEATHER, "يوم مشمس. لو تقدر، اطلع شوي تاخذ هوا.", "Sunny day. Step out for a bit of fresh air if you can.", JORDANIAN),
        // CLOUDY
        p("wt_cloud1", WEATHER, "الجو غائم اليوم. مريح للمشي إن أردت.", "Overcast today — comfortable for a walk if you fancy one."),
        p("wt_cloud2", WEATHER, "يوم غيوم. مش حار ومش بارد، وقت مناسب تتمشى.", "Cloudy day. Not too hot, not too cold — good for a stroll.", JORDANIAN),
        // WINDY
        p("wt_wind1", WEATHER, "الرياح قوية اليوم. انتبه لو خرجت.", "Windy today. Watch out if you're heading out."),
        p("wt_wind2", WEATHER, "في هوا كثير اليوم. مناسب لبس خفيف مع كنزة.", "It's quite windy today. Light layers would work well.", JORDANIAN),

        p("ak1", ACK_DONE, "تمام، سجّلتها.", "Done, noted."),
        p("ak2", ACK_DONE, "ممتاز، سجّلت ذلك.", "Great, I've logged it."),
        p("pa1", PRAYER_ACK, "الله يتقبل منك.", "May it be accepted."),
        p("pa2", PRAYER_ACK, "تقبّل الله طاعتك.", "May Allah accept it from you."),
        p("ak3", ACK_DONE, "الله يعطيك العافية.", "Well done.", JORDANIAN),
    )

    init { require(ALL.map { it.id }.toSet().size == ALL.size) { "duplicate phrase ids" } }

    // Phrases that must never appear (childish forms). Enforced by a unit test as well.
    val FORBIDDEN_FRAGMENTS = listOf("يا بطل", "يا صغيري", "يا حبيبي الصغير")
}
