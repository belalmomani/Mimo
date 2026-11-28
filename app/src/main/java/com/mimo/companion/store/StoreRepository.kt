package com.mimo.companion.store

import android.content.Context
import com.mimo.companion.points.PointsRepository
import com.mimo.companion.points.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class UnlockResult {
    object AlreadyOwned : UnlockResult()
    object Unlocked : UnlockResult()
    object InsufficientPoints : UnlockResult()
    object AdProgressSaved : UnlockResult()   // تقدم محفوظ، لم يفتح بعد
    object InvalidShape : UnlockResult()
}

class StoreRepository(context: Context, private val points: PointsRepository) {

    private val store = ShapeStore(context)

    private val _ownerships = MutableStateFlow(store.getAllOwnerships())
    val ownerships: StateFlow<List<ShapeOwnership>> = _ownerships

    private val _activeShapeId = MutableStateFlow(store.getActiveShapeId())
    val activeShapeId: StateFlow<String> = _activeShapeId

    // --- Mutex flag
    @Volatile private var unlocking = false

    fun isOwned(shapeId: String): Boolean {
        val shape = ShapeCatalog.byId(shapeId) ?: return false
        return shape.isFree || store.isOwned(shapeId)
    }

    fun getOwnership(shapeId: String): ShapeOwnership? =
        _ownerships.value.find { it.shapeId == shapeId }

    @Synchronized
    fun unlockWithPoints(shapeId: String): UnlockResult {
        if (unlocking) return UnlockResult.InvalidShape
        val shape = ShapeCatalog.byId(shapeId) ?: return UnlockResult.InvalidShape
        if (isOwned(shapeId)) return UnlockResult.AlreadyOwned
        if (!shape.canUsePoints) return UnlockResult.InvalidShape
        if (!points.hasEnough(shape.pointsCost)) return UnlockResult.InsufficientPoints

        unlocking = true
        return try {
            val debited = points.debit(shape.pointsCost, TransactionType.SHAPE_UNLOCK, "فتح: ${shape.nameAr}")
            if (debited) {
                store.setOwned(shapeId)
                refresh()
                UnlockResult.Unlocked
            } else {
                UnlockResult.InsufficientPoints
            }
        } finally {
            unlocking = false
        }
    }

    @Synchronized
    fun recordAdForShape(shapeId: String, adId: String): UnlockResult {
        val shape = ShapeCatalog.byId(shapeId) ?: return UnlockResult.InvalidShape
        if (isOwned(shapeId)) return UnlockResult.AlreadyOwned
        if (!shape.needsAds) return UnlockResult.InvalidShape

        val newCount = store.incrementAds(shapeId)
        refresh()

        return if (newCount >= shape.adCount) {
            store.setOwned(shapeId)
            refresh()
            UnlockResult.Unlocked
        } else {
            UnlockResult.AdProgressSaved
        }
    }

    fun setActive(shapeId: String): Boolean {
        if (!isOwned(shapeId)) return false
        store.setActiveShapeId(shapeId)
        _activeShapeId.value = shapeId
        return true
    }

    private fun refresh() {
        _ownerships.value = store.getAllOwnerships()
    }
}
