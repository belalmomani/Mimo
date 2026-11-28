package com.mimo.companion.points

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PointsRepository(context: Context) {

    private val store = PointsStore(context)

    private val _balance = MutableStateFlow(store.getBalance())
    val balance: StateFlow<Int> = _balance

    private val _transactions = MutableStateFlow(store.getTransactions())
    val transactions: StateFlow<List<PointsTransaction>> = _transactions

    // Mutex-like flag to prevent double debit
    @Volatile private var processing = false

    @Synchronized
    fun credit(amount: Int, type: TransactionType, note: String = ""): Boolean {
        if (amount <= 0) return false
        val newBalance = _balance.value + amount
        store.setBalance(newBalance)
        val tx = PointsTransaction(type = type, amount = amount, balanceAfter = newBalance, note = note)
        store.appendTransaction(tx)
        _balance.value = newBalance
        _transactions.value = store.getTransactions()
        return true
    }

    @Synchronized
    fun debit(amount: Int, type: TransactionType, note: String = ""): Boolean {
        if (processing) return false
        if (amount <= 0) return false
        val current = _balance.value
        if (current < amount) return false
        processing = true
        return try {
            val newBalance = current - amount
            store.setBalance(newBalance)
            val tx = PointsTransaction(type = type, amount = -amount, balanceAfter = newBalance, note = note)
            store.appendTransaction(tx)
            _balance.value = newBalance
            _transactions.value = store.getTransactions()
            true
        } finally {
            processing = false
        }
    }

    fun creditAdReward(adId: String): Boolean {
        if (!PointsConfig.adsEnabled) return false
        if (store.isAdIdUsed(adId)) return false
        store.markAdIdUsed(adId)
        return credit(PointsConfig.POINTS_PER_AD, TransactionType.AD_REWARD, "إعلان: $adId")
    }

    fun giveWelcomeBonus(): Boolean {
        if (store.isWelcomeGiven()) return false
        store.setWelcomeGiven()
        return credit(PointsConfig.WELCOME_BONUS, TransactionType.WELCOME_BONUS, "مكافأة ترحيبية")
    }

    fun hasEnough(amount: Int): Boolean = _balance.value >= amount

    fun reload() {
        _balance.value = store.getBalance()
        _transactions.value = store.getTransactions()
    }
}
