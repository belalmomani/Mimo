package com.mimo.companion.domain

/**
 * Persistence contract for everything that must stay consistent: balance, ledger, owned skins,
 * ad progress, rewarded-ad events and daily counters.
 *
 * All methods are blocking and must be called off the main thread.
 *
 * [inTransaction] is the atomicity boundary: if [block] throws, every write made inside it is
 * rolled back and the exception propagates. Calls may be nested. The production implementation
 * is Room ([com.mimo.companion.data.db.RoomLedgerStorage]); tests use an in-memory one.
 */
interface LedgerStorage {
    fun <T> inTransaction(block: () -> T): T

    // wallet + ledger
    fun getBalance(): Long
    fun setBalance(value: Long)

    /** @return false (and writes nothing) when a row with the same id already exists. */
    fun insertTransaction(tx: PointsTransaction): Boolean
    fun transactions(limit: Int): List<PointsTransaction>

    // ownership
    fun isOwned(skinId: String): Boolean

    /** @return false when the skin was already owned. */
    fun addOwned(skinId: String, atMillis: Long, method: String): Boolean
    fun ownedSkins(): List<OwnedSkin>

    // rewarded-ad progress per skin
    fun adProgress(skinId: String): Int
    fun setAdProgress(skinId: String, count: Int)

    // rewarded-ad events (each completed ad session may be counted once)
    /** @return false when this event id was already recorded. */
    fun markRewardEvent(eventId: String, atMillis: Long): Boolean

    // limits
    fun dailyAdCount(dayKey: String): Int
    fun setDailyAdCount(dayKey: String, count: Int)
    fun lastAdRewardAt(): Long
    fun setLastAdRewardAt(millis: Long)

    // selection
    fun activeSkinId(): String?
    fun setActiveSkinId(id: String)
}
