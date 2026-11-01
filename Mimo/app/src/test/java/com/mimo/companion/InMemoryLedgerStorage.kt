package com.mimo.companion

import com.mimo.companion.domain.LedgerStorage
import com.mimo.companion.domain.OwnedSkin
import com.mimo.companion.domain.PointsTransaction
import com.mimo.companion.domain.TimeSource
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Test double with REAL transaction semantics: [inTransaction] snapshots the state and restores it
 * if the block throws, exactly like Room's rollback. Failure injection lets tests prove that
 * a failed write leaves balance, ledger and ownership untouched.
 */
class InMemoryLedgerStorage : LedgerStorage {
    private data class State(
        var balance: Long = 0,
        val txs: MutableList<PointsTransaction> = mutableListOf(),
        val owned: MutableMap<String, OwnedSkin> = linkedMapOf(),
        val progress: MutableMap<String, Int> = mutableMapOf(),
        val events: MutableSet<String> = mutableSetOf(),
        val daily: MutableMap<String, Int> = mutableMapOf(),
        var lastAd: Long = 0,
        var active: String? = null,
    ) {
        fun copyDeep() = State(balance, txs.toMutableList(), owned.toMutableMap(), progress.toMutableMap(), events.toMutableSet(), daily.toMutableMap(), lastAd, active)
    }

    private var s = State()
    private var depth = 0

    /** When true, the next [insertTransaction] throws (simulates a failing disk write). */
    var failNextInsertTransaction = false
    var failNextAddOwned = false

    override fun <T> inTransaction(block: () -> T): T {
        if (depth > 0) { depth++; try { return block() } finally { depth-- } }
        val snapshot = s.copyDeep()
        depth = 1
        try {
            return block()
        } catch (t: Throwable) {
            s = snapshot
            throw t
        } finally {
            depth = 0
        }
    }

    override fun getBalance() = s.balance
    override fun setBalance(value: Long) { s.balance = value }

    override fun insertTransaction(tx: PointsTransaction): Boolean {
        if (failNextInsertTransaction) { failNextInsertTransaction = false; throw IllegalStateException("disk write failed") }
        if (s.txs.any { it.id == tx.id }) return false
        s.txs.add(tx); return true
    }

    override fun transactions(limit: Int) = s.txs.asReversed().take(limit)
    override fun isOwned(skinId: String) = s.owned.containsKey(skinId)

    override fun addOwned(skinId: String, atMillis: Long, method: String): Boolean {
        if (failNextAddOwned) { failNextAddOwned = false; throw IllegalStateException("disk write failed") }
        if (s.owned.containsKey(skinId)) return false
        s.owned[skinId] = OwnedSkin(skinId, atMillis, method); return true
    }

    override fun ownedSkins() = s.owned.values.toList()
    override fun adProgress(skinId: String) = s.progress[skinId] ?: 0
    override fun setAdProgress(skinId: String, count: Int) { s.progress[skinId] = count }
    override fun markRewardEvent(eventId: String, atMillis: Long) = s.events.add(eventId)
    override fun dailyAdCount(dayKey: String) = s.daily[dayKey] ?: 0
    override fun setDailyAdCount(dayKey: String, count: Int) { s.daily[dayKey] = count }
    override fun lastAdRewardAt() = s.lastAd
    override fun setLastAdRewardAt(millis: Long) { s.lastAd = millis }
    override fun activeSkinId() = s.active
    override fun setActiveSkinId(id: String) { s.active = id }

    /** Simulates "app restarted": same data, new storage object. */
    fun reopen(): InMemoryLedgerStorage = InMemoryLedgerStorage().also { it.s = this.s.copyDeep() }
}

class FakeTime(var now: Long = 1_800_000_000_000L) : TimeSource {
    override fun nowMillis() = now
    override fun zone(): ZoneId = ZoneOffset.UTC
    fun advanceSeconds(sec: Long) { now += sec * 1000 }
    fun advanceDays(days: Long) { now += days * 24 * 3600 * 1000 }
}
