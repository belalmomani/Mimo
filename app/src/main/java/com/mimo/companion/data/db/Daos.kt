package com.mimo.companion.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LedgerDao {
    @Query("SELECT balance FROM wallet WHERE id = 1") fun balance(): Long?
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun putWallet(w: WalletEntity)

    /** @return the new row id, or -1 when the id already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE) fun insertTx(t: TransactionEntity): Long
    @Query("SELECT * FROM points_transactions ORDER BY createdAt DESC, rowid DESC LIMIT :limit") fun txs(limit: Int): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM owned_skins WHERE skinId = :id") fun ownedCount(id: String): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE) fun insertOwned(o: OwnedSkinEntity): Long
    @Query("SELECT * FROM owned_skins ORDER BY unlockedAt") fun allOwned(): List<OwnedSkinEntity>

    @Query("SELECT count FROM ad_progress WHERE skinId = :id") fun progress(id: String): Int?
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun putProgress(p: AdProgressEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE) fun insertEvent(e: RewardEventEntity): Long

    @Query("SELECT value FROM kv WHERE `key` = :key") fun kvGet(key: String): String?
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun kvPut(e: KvEntity)
}
