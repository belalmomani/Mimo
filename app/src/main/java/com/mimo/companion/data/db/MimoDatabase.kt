package com.mimo.companion.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WalletEntity::class, TransactionEntity::class, OwnedSkinEntity::class, AdProgressEntity::class, RewardEventEntity::class, KvEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MimoDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao

    companion object {
        /**
         * No destructive migration on purpose: points and ownership must survive app updates.
         * When the schema changes, bump `version` and add a real Migration here.
         */
        fun build(context: Context): MimoDatabase =
            Room.databaseBuilder(context.applicationContext, MimoDatabase::class.java, "mimo.db").build()
    }
}
