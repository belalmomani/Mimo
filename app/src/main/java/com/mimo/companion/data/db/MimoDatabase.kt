package com.mimo.companion.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WalletEntity::class, TransactionEntity::class, OwnedSkinEntity::class, AdProgressEntity::class, RewardEventEntity::class, KvEntity::class, ReminderEntity::class, ReminderDoneEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class MimoDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        /**
         * No destructive migration on purpose: points and ownership must survive app updates.
         * When the schema changes, bump `version` and add a real Migration here.
         */
        fun build(context: Context): MimoDatabase =
            Room.databaseBuilder(context.applicationContext, MimoDatabase::class.java, "mimo.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        /** v1 -> v2: adds the reminders tables. Existing points / ownership tables are untouched. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `reminders` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `timeMin` INTEGER NOT NULL, `repeatType` TEXT NOT NULL, `daysMask` INTEGER NOT NULL, `onceDate` TEXT, `enabled` INTEGER NOT NULL, `customMessage` TEXT, `deliveryMask` INTEGER NOT NULL, `snoozeMinutes` INTEGER NOT NULL, `snoozedUntil` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `reminder_done` (`reminderId` TEXT NOT NULL, `dayKey` TEXT NOT NULL, PRIMARY KEY(`reminderId`, `dayKey`))")
            }
        }
    }
}
