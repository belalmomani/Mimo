package com.mimo.companion.domain.dialogue

import com.mimo.companion.domain.dialogue.Dialect.JORDANIAN
import com.mimo.companion.domain.dialogue.PhraseCategory.ACK_DONE
import com.mimo.companion.domain.dialogue.PhraseCategory.BREAK_SUGGESTION
import com.mimo.companion.domain.dialogue.PhraseCategory.ENCOURAGE
import com.mimo.companion.domain.dialogue.PhraseCategory.GREETING
import com.mimo.companion.domain.dialogue.PhraseCategory.IDLE_CHAT
import com.mimo.companion.domain.dialogue.PhraseCategory.TAP_REPLY

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

        p("ak1", ACK_DONE, "تمام، سجّلتها.", "Done, noted."),
        p("ak2", ACK_DONE, "ممتاز، سجّلت ذلك.", "Great, I've logged it."),
        p("ak3", ACK_DONE, "الله يعطيك العافية.", "Well done.", JORDANIAN),
    )

    init { require(ALL.map { it.id }.toSet().size == ALL.size) { "duplicate phrase ids" } }

    // Phrases that must never appear (childish forms). Enforced by a unit test as well.
    val FORBIDDEN_FRAGMENTS = listOf("يا بطل", "يا صغيري", "يا حبيبي الصغير")
}
