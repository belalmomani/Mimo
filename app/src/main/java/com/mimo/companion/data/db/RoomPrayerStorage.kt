package com.mimo.companion.data.db

import com.mimo.companion.domain.prayer.Prayer
import com.mimo.companion.domain.prayer.PrayerState
import com.mimo.companion.domain.prayer.PrayerStorage

/** Local prayer log + reminder state. Background thread only. Never leaves the device. */
class RoomPrayerStorage(db: MimoDatabase) : PrayerStorage {
    private val dao = db.prayerDao()
    override fun doneSet(dayKey: String) = dao.done(dayKey).mapNotNull { n -> Prayer.values().firstOrNull { it.name == n } }.toSet()
    override fun setDone(dayKey: String, prayer: Prayer, done: Boolean) {
        if (done) dao.addDone(PrayerDoneEntity(dayKey, prayer.name)) else dao.removeDone(dayKey, prayer.name)
    }
    override fun state(dayKey: String, prayer: Prayer): PrayerState =
        dao.state(dayKey, prayer.name)?.let { PrayerState(it.sentCount, it.nextAt) } ?: PrayerState()
    override fun saveState(dayKey: String, prayer: Prayer, state: PrayerState) =
        dao.putState(PrayerStateEntity(dayKey, prayer.name, state.sentCount, state.nextAtMillis))
    override fun pruneBefore(dayKey: String) { dao.pruneDone(dayKey); dao.pruneState(dayKey) }
    override fun clearAll() { dao.clearDone(); dao.clearState() }
}
