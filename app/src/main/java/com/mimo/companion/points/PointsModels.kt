package com.mimo.companion.points

import java.util.UUID

enum class TransactionType(val labelAr: String) {
    AD_REWARD("مكافأة إعلان"),
    SHAPE_UNLOCK("فتح شكل"),
    POINTS_PURCHASE("شراء نقاط"),
    WELCOME_BONUS("مكافأة ترحيبية"),
    EVENT_BONUS("مكافأة حدث"),
    REFUND("استرداد"),
    CORRECTION("تصحيح")
}

data class PointsTransaction(
    val id: String = UUID.randomUUID().toString(),
    val type: TransactionType,
    val amount: Int,            // positive = credit, negative = debit
    val balanceAfter: Int,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
