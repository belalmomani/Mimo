package com.mimo.companion.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row wallet (id = 1). */
@Entity(tableName = "wallet")
data class WalletEntity(@PrimaryKey val id: Int = 1, val balance: Long)

/** Ledger. The primary key is the idempotency key: inserting the same id twice is ignored. */
@Entity(tableName = "points_transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amount: Long,
    val balanceAfter: Long,
    val refId: String?,
    val note: String?,
    val createdAt: Long,
)

@Entity(tableName = "owned_skins")
data class OwnedSkinEntity(@PrimaryKey val skinId: String, val unlockedAt: Long, val method: String)

@Entity(tableName = "ad_progress")
data class AdProgressEntity(@PrimaryKey val skinId: String, val count: Int)

/** One row per rewarded-ad session already paid out / counted. */
@Entity(tableName = "reward_events")
data class RewardEventEntity(@PrimaryKey val eventId: String, val createdAt: Long)

/** Small key/value table for counters and the active look (avoids a second storage technology). */
@Entity(tableName = "kv")
data class KvEntity(@PrimaryKey val key: String, val value: String)
