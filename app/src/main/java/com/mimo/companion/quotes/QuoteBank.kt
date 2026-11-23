package com.mimo.companion.quotes

data class Quote(val text: String, val author: String = "")

/**
 * Built-in Arabic motivational quotes shown daily by ميمو.
 * The daily quote is picked by (dayOfYear % size) for consistency across the day.
 */
object QuoteBank {

    val quotes = listOf(
        Quote("إذا كانت لديك القدرة على الحلم، فلديك القدرة على تحقيق حلمك.", "والت ديزني"),
        Quote("النجاح ليس نهائياً، والفشل ليس قاتلاً، الشجاعة على الاستمرار هي ما يهم.", "ونستون تشرشل"),
        Quote("لا تقل لا أستطيع، قل لم أتعلم بعد."),
        Quote("كل يوم هو فرصة جديدة لتكون أفضل مما كنت عليه بالأمس."),
        Quote("الإرادة تصنع المستحيل."),
        Quote("ابدأ من حيث أنت، استخدم ما لديك، افعل ما تستطيع.", "آرثر آش"),
        Quote("الحياة قصيرة جداً لتضييعها في الندم."),
        Quote("لا تنتظر الفرصة المثالية، اصنع فرصتك."),
        Quote("الأمل هو حلم اليقظة.", "أرسطو"),
        Quote("أعظم مجد في الحياة ليس في عدم السقوط، بل في النهوض في كل مرة نسقط.", "نيلسون مانديلا"),
        Quote("إن شئت أن تعيش مرتاح البال، فلا تشتغل بما لا يعنيك."),
        Quote("اسعَ إلى ما تحب، وأحب ما تسعى إليه."),
        Quote("الوقت أثمن من الذهب، فلا تُضيّعه."),
        Quote("مَن صبر ظفر."),
        Quote("العلم نور، والجهل ظلام."),
        Quote("لكل مشكلة حل، وأما اليأس فليس حلاً."),
        Quote("ركّز على ما تملك، لا على ما تفتقر إليه."),
        Quote("التفاؤل إيمان يقود إلى الإنجاز."),
        Quote("ابتسم، فإن الابتسامة صدقة."),
        Quote("إذا لم تجد من تحب، فأحبّ ما تجد."),
        Quote("الإنسان عدو ما يجهله."),
        Quote("خير الأصدقاء من يرشدك إلى الصواب."),
        Quote("ليس الجمال بالوجه، الجمال في الروح."),
        Quote("الكلمة الطيبة صدقة."),
        Quote("من جد وجد، ومن زرع حصد."),
        Quote("قيمة الإنسان بعمله لا بكلامه."),
        Quote("لا يفل الحديد إلا الحديد."),
        Quote("التواضع يرفع، والغرور يضع."),
        Quote("العقل السليم في الجسم السليم."),
        Quote("الصبر مفتاح الفرج."),
    )

    /** Returns the same quote all day (changes at midnight). */
    fun todayQuote(): Quote {
        val day = java.time.LocalDate.now().dayOfYear
        return quotes[day % quotes.size]
    }

    /** Returns a random quote different from today's. */
    fun randomQuote(): Quote = quotes.random()
}
