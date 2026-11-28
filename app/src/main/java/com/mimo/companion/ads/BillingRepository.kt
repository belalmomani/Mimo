package com.mimo.companion.ads

/**
 * طبقة الفوترة — جاهزة لتكامل Google Play Billing.
 *
 * للتفعيل الحقيقي:
 * 1. أضف dependency: com.android.billingclient:billing-ktx:7.x.x
 * 2. أنشئ منتجات في Google Play Console من نوع "One-time product"
 * 3. أضف معرفات المنتجات أدناه
 * 4. نفّذ BillingClient.startConnection() في init()
 * 5. اربط queryProductDetailsAsync / launchBillingFlow
 * 6. تحقق من acknowledgePurchase لكل عملية
 */
object BillingRepository {

    // معرفات المنتجات — يجب إنشاؤها في Google Play Console
    const val PRODUCT_STARTER  = "mimo_points_100"   // 100 نقطة
    const val PRODUCT_PLUS     = "mimo_points_300"   // 300 نقطة
    const val PRODUCT_PREMIUM  = "mimo_points_700"   // 700 نقطة
    const val PRODUCT_ULTIMATE = "mimo_points_1500"  // 1500 نقطة

    data class PointsPackage(
        val productId: String,
        val nameAr: String,
        val points: Int,
        val displayPrice: String  // يُجلب من Google Play عند التفعيل
    )

    val packages = listOf(
        PointsPackage(PRODUCT_STARTER,  "Starter",  100,  "—"),
        PointsPackage(PRODUCT_PLUS,     "Plus",     300,  "—"),
        PointsPackage(PRODUCT_PREMIUM,  "Premium",  700,  "—"),
        PointsPackage(PRODUCT_ULTIMATE, "Ultimate", 1500, "—"),
    )

    /**
     * الشراء الحقيقي غير مفعّل — يحتاج إعداد Google Play Billing.
     * الأسعار مُعطلة حتى ربط المتجر.
     */
    val isAvailable: Boolean = false
}
