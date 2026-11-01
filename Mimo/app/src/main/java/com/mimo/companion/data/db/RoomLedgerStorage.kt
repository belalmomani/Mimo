package com.mimo.companion.data.db

import androidx.room.withTransaction
import com.mimo.companion.domain.LedgerStorage
import com.mimo.companion.domain.OwnedSkin
import com.mimo.companion.domain.PointsTransaction
import com.mimo.companion.domain.TransactionType
import kotlinx.coroutines.runBlocking

/**
 * Room implementation of [LedgerStorage]. Must be used from a background thread (see Graph.io):
 * [inTransaction] blocks the caller while Room runs the block inside one SQLite transaction;
 * an exception thrown by the block rolls every write back.
 */
class RoomLedgerStorage(private val db: MimoDatabase) : LedgerStorage {
    private val dao = db.ledgerDao()

    override fun <T> inTransaction(block: () -> T): T = runBlocking { db.withTransaction { block() } }

    override fun getBalance(): Long = dao.balance() ?: 0L
    override fun setBalance(value: Long) = dao.putWallet(WalletEntity(1, value))

    override fun insertTransaction(tx: PointsTransaction): Boolean =
        dao.insertTx(TransactionEntity(tx.id, tx.type.name, tx.amount, tx.balanceAfter, tx.refId, tx.note, tx.createdAtMillis)) != -1L

    override fun transactions(limit: Int): List<PointsTransaction> = dao.txs(limit).map {
        PointsTransaction(it.id, TransactionType.valueOf(it.type), it.amount, it.balanceAfter, it.refId, it.note, it.createdAt)
    }

    override fun isOwned(skinId: String) = dao.ownedCount(skinId) > 0
    override fun addOwned(skinId: String, atMillis: Long, method: String) = dao.insertOwned(OwnedSkinEntity(skinId, atMillis, method)) != -1L
    override fun ownedSkins() = dao.allOwned().map { OwnedSkin(it.skinId, it.unlockedAt, it.method) }

    override fun adProgress(skinId: String) = dao.progress(skinId) ?: 0
    override fun setAdProgress(skinId: String, count: Int) = dao.putProgress(AdProgressEntity(skinId, count))

    override fun markRewardEvent(eventId: String, atMillis: Long) = dao.insertEvent(RewardEventEntity(eventId, atMillis)) != -1L

    override fun dailyAdCount(dayKey: String) = dao.kvGet("daily:$dayKey")?.toIntOrNull() ?: 0
    override fun setDailyAdCount(dayKey: String, count: Int) = dao.kvPut(KvEntity("daily:$dayKey", count.toString()))
    override fun lastAdRewardAt() = dao.kvGet("lastAdReward")?.toLongOrNull() ?: 0L
    override fun setLastAdRewardAt(millis: Long) = dao.kvPut(KvEntity("lastAdReward", millis.toString()))

    override fun activeSkinId() = dao.kvGet("activeSkin")
    override fun setActiveSkinId(id: String) = dao.kvPut(KvEntity("activeSkin", id))
}
