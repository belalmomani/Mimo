package com.mimo.companion.ads

import android.app.Activity

/**
 * واجهة قابلة للتبديل لمزود إعلانات المكافآت.
 *
 * Start.io SDK v5 لا يدعم Rewarded Ads بشكل موثق.
 * هذه الواجهة تتيح تبديل المزود لاحقاً (AdMob، Unity Ads، إلخ)
 * دون تغيير منطق التطبيق.
 */
interface RewardedAdProvider {
    /** هل الإعلان محمّل وجاهز */
    val isReady: Boolean

    /** تحميل إعلان مسبق */
    fun load()

    /**
     * عرض الإعلان
     * @param activity النشاط الحالي
     * @param onRewarded يُستدعى عند إكمال المكافأة بـ adId فريد
     * @param onDismissed يُستدعى عند إغلاق الإعلان دون مكافأة
     * @param onFailed يُستدعى عند فشل التحميل أو العرض
     */
    fun show(
        activity: Activity,
        onRewarded: (adId: String) -> Unit,
        onDismissed: () -> Unit,
        onFailed: (reason: String) -> Unit
    )
}

/**
 * مزود وهمي للتطوير — يُحاكي مكافأة بعد تأخير.
 * استبدله بمزود حقيقي عند الإنتاج.
 */
class StubRewardedAdProvider : RewardedAdProvider {
    override val isReady: Boolean = true

    override fun load() { /* لا شيء */ }

    override fun show(
        activity: Activity,
        onRewarded: (adId: String) -> Unit,
        onDismissed: () -> Unit,
        onFailed: (reason: String) -> Unit
    ) {
        // في الإنتاج: استبدل بمزود حقيقي
        // هذا الـ stub يمنح المكافأة مباشرة (للاختبار فقط)
        val fakeAdId = "stub_${System.currentTimeMillis()}"
        onRewarded(fakeAdId)
    }
}
