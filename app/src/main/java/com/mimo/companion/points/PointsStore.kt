package com.mimo.companion.points

import android.content.Context
import android.content.SharedPreferences

class PointsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("mimo_points", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BALANCE = "balance"
        private const val KEY_TRANSACTIONS = "transactions"
        private const val KEY_USED_AD_IDS = "used_ad_ids"
        private const val KEY_WELCOME_GIVEN = "welcome_given"
        private const val SEP = "|"
        private const val REC_SEP = "§"
    }

    fun getBalance(): Int = prefs.getInt(KEY_BALANCE, 0)

    fun setBalance(v: Int) = prefs.edit().putInt(KEY_BALANCE, v).apply()

    fun getTransactions(): List<PointsTransaction> {
        val raw = prefs.getString(KEY_TRANSACTIONS, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(REC_SEP).mapNotNull { rec ->
            val p = rec.split(SEP)
            if (p.size < 7) null
            else try {
                PointsTransaction(
                    id = p[0],
                    type = TransactionType.valueOf(p[1]),
                    amount = p[2].toInt(),
                    balanceAfter = p[3].toInt(),
                    note = p[4],
                    timestamp = p[5].toLong()
                )
            } catch (e: Exception) { null }
        }
    }

    fun appendTransaction(tx: PointsTransaction) {
        val existing = prefs.getString(KEY_TRANSACTIONS, "") ?: ""
        val entry = "${tx.id}$SEP${tx.type.name}$SEP${tx.amount}$SEP${tx.balanceAfter}$SEP${tx.note}$SEP${tx.timestamp}$SEP"
        val updated = if (existing.isBlank()) entry else "$existing$REC_SEP$entry"
        // Keep last 200 transactions
        val recs = updated.split(REC_SEP).takeLast(200).joinToString(REC_SEP)
        prefs.edit().putString(KEY_TRANSACTIONS, recs).apply()
    }

    fun isAdIdUsed(adId: String): Boolean {
        val ids = prefs.getStringSet(KEY_USED_AD_IDS, emptySet()) ?: emptySet()
        return ids.contains(adId)
    }

    fun markAdIdUsed(adId: String) {
        val ids = (prefs.getStringSet(KEY_USED_AD_IDS, emptySet()) ?: emptySet()).toMutableSet()
        ids.add(adId)
        // Keep last 500 ids
        val trimmed = if (ids.size > 500) ids.take(500).toSet() else ids
        prefs.edit().putStringSet(KEY_USED_AD_IDS, trimmed).apply()
    }

    fun isWelcomeGiven(): Boolean = prefs.getBoolean(KEY_WELCOME_GIVEN, false)
    fun setWelcomeGiven() = prefs.edit().putBoolean(KEY_WELCOME_GIVEN, true).apply()
}
