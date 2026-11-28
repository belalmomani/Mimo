package com.mimo.companion.points

object PointsConfig {
    /** نقاط مكتسبة من كل إعلان مكتمل */
    const val POINTS_PER_AD: Int = 10

    /** الحد الأقصى للإعلانات المكافئة يومياً */
    const val MAX_ADS_PER_DAY: Int = 10

    /** مكافأة ترحيبية */
    const val WELCOME_BONUS: Int = 20

    /** هل مكافآت الإعلانات مفعّلة */
    var adsEnabled: Boolean = true
}
