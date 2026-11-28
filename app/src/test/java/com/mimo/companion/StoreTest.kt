package com.mimo.companion

import com.mimo.companion.points.*
import com.mimo.companion.store.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * اختبارات نظام فتح الأشكال - JVM فقط
 */
class StoreTest {

    private lateinit var pointsStore: FakePointsStore2
    private lateinit var points: TestablePointsRepo2
    private lateinit var shapeStore: FakeShapeStore
    private lateinit var storeRepo: TestableStoreRepo

    @Before
    fun setup() {
        pointsStore = FakePointsStore2()
        points = TestablePointsRepo2(pointsStore)
        shapeStore = FakeShapeStore()
        storeRepo = TestableStoreRepo(shapeStore, points)
    }

    // 1. فتح شكل مجاني
    @Test
    fun `free shape is always owned`() {
        val free = ShapeCatalog.all.first { it.unlockType == UnlockType.FREE }
        assertTrue(storeRepo.isOwned(free.id))
    }

    // 2. فتح شكل بالنقاط - يملك رصيداً كافياً
    @Test
    fun `unlock with points succeeds when sufficient balance`() {
        val shape = ShapeCatalog.all.first { it.unlockType == UnlockType.POINTS }
        points.credit(shape.pointsCost + 10, TransactionType.WELCOME_BONUS)
        val result = storeRepo.unlockWithPoints(shape.id)
        assertEquals(UnlockResult.Unlocked, result)
        assertTrue(storeRepo.isOwned(shape.id))
    }

    // 3. فتح شكل بالنقاط - رصيد غير كافٍ
    @Test
    fun `unlock with points fails when insufficient balance`() {
        val shape = ShapeCatalog.all.first { it.unlockType == UnlockType.POINTS }
        val result = storeRepo.unlockWithPoints(shape.id)
        assertEquals(UnlockResult.InsufficientPoints, result)
        assertFalse(storeRepo.isOwned(shape.id))
    }

    // 4. فتح شكل بإعلان واحد
    @Test
    fun `shape needing 1 ad unlocks after 1 ad`() {
        val shape = ShapeCatalog.all.first { it.unlockType == UnlockType.AD_1 }
        val result = storeRepo.recordAdForShape(shape.id, "ad_x1")
        assertEquals(UnlockResult.Unlocked, result)
        assertTrue(storeRepo.isOwned(shape.id))
    }

    // 5. فتح شكل بإعلانين
    @Test
    fun `shape needing 2 ads unlocks after 2 ads`() {
        val shape = ShapeCatalog.all.first { it.unlockType == UnlockType.AD_2 }
        val r1 = storeRepo.recordAdForShape(shape.id, "ad_a")
        assertEquals(UnlockResult.AdProgressSaved, r1)
        assertFalse(storeRepo.isOwned(shape.id))
        val r2 = storeRepo.recordAdForShape(shape.id, "ad_b")
        assertEquals(UnlockResult.Unlocked, r2)
        assertTrue(storeRepo.isOwned(shape.id))
    }

    // 6. فتح شكل بثلاثة إعلانات
    @Test
    fun `shape needing 3 ads unlocks after 3 ads`() {
        val shape = ShapeCatalog.all.first { it.unlockType == UnlockType.AD_3 }
        storeRepo.recordAdForShape(shape.id, "ad_1")
        storeRepo.recordAdForShape(shape.id, "ad_2")
        val r3 = storeRepo.recordAdForShape(shape.id, "ad_3")
        assertEquals(UnlockResult.Unlocked, r3)
        assertTrue(storeRepo.isOwned(shape.id))
    }

    // 7. محاولة فتح شكل مملوك
    @Test
    fun `unlocking already owned shape returns AlreadyOwned`() {
        val free = ShapeCatalog.all.first { it.unlockType == UnlockType.FREE }
        val r = storeRepo.unlockWithPoints(free.id)
        assertEquals(UnlockResult.AlreadyOwned, r)
    }

    // 8. تقدم الإعلانات محفوظ
    @Test
    fun `ad progress is saved between calls`() {
        val shape = ShapeCatalog.all.first { it.unlockType == UnlockType.AD_3 }
        storeRepo.recordAdForShape(shape.id, "ad_p1")
        val watched = shapeStore.getAdsWatched(shape.id)
        assertEquals(1, watched)
    }

    // 9. لا تخصم نقاطاً لشكل مجاني
    @Test
    fun `free shape does not deduct points`() {
        points.credit(100, TransactionType.WELCOME_BONUS)
        val free = ShapeCatalog.all.first { it.unlockType == UnlockType.FREE }
        storeRepo.unlockWithPoints(free.id)  // AlreadyOwned
        assertEquals(100, points.balance.value)
    }

    // 10. الشكل النشط
    @Test
    fun `setActive works for owned shape`() {
        val free = ShapeCatalog.all.first { it.unlockType == UnlockType.FREE }
        val ok = storeRepo.setActive(free.id)
        assertTrue(ok)
    }

    // 11. لا يمكن تعيين شكل مقفل نشطاً
    @Test
    fun `setActive fails for locked shape`() {
        val locked = ShapeCatalog.all.first { it.unlockType == UnlockType.POINTS }
        val ok = storeRepo.setActive(locked.id)
        assertFalse(ok)
    }
}

// ─── Fakes ───────────────────────────────────────────────────────────────────

class FakePointsStore2 {
    var balance = 0
    val transactions = mutableListOf<PointsTransaction>()
    val usedAdIds = mutableSetOf<String>()
    var welcomeGiven = false
}

class TestablePointsRepo2(private val store: FakePointsStore2) {
    private val _balance = kotlinx.coroutines.flow.MutableStateFlow(store.balance)
    val balance: kotlinx.coroutines.flow.StateFlow<Int> = _balance

    fun credit(amount: Int, type: TransactionType, note: String = ""): Boolean {
        val newBal = _balance.value + amount
        store.balance = newBal
        _balance.value = newBal
        return true
    }

    fun debit(amount: Int, type: TransactionType, note: String = ""): Boolean {
        if (_balance.value < amount) return false
        val newBal = _balance.value - amount
        store.balance = newBal
        _balance.value = newBal
        return true
    }

    fun hasEnough(amount: Int) = _balance.value >= amount
}

class FakeShapeStore {
    private val ads = mutableMapOf<String, Int>()
    private val owned = mutableMapOf<String, Boolean>()
    private var active = "s01"

    fun getAdsWatched(id: String) = ads[id] ?: 0
    fun incrementAds(id: String): Int { val n = (ads[id] ?: 0) + 1; ads[id] = n; return n }
    fun isOwned(id: String) = owned[id] == true
    fun setOwned(id: String) { owned[id] = true }
    fun getActiveShapeId() = active
    fun setActiveShapeId(id: String) { active = id }
    fun getAllOwnerships() = ShapeCatalog.all.map { ShapeOwnership(it.id, ads[it.id] ?: 0, owned[it.id] == true) }
}

class TestableStoreRepo(private val shapeStore: FakeShapeStore, private val points: TestablePointsRepo2) {
    private val _ownerships = kotlinx.coroutines.flow.MutableStateFlow(shapeStore.getAllOwnerships())
    @Volatile private var unlocking = false

    fun isOwned(id: String): Boolean {
        val shape = ShapeCatalog.byId(id) ?: return false
        return shape.isFree || shapeStore.isOwned(id)
    }

    @Synchronized
    fun unlockWithPoints(shapeId: String): UnlockResult {
        if (unlocking) return UnlockResult.InvalidShape
        val shape = ShapeCatalog.byId(shapeId) ?: return UnlockResult.InvalidShape
        if (isOwned(shapeId)) return UnlockResult.AlreadyOwned
        if (!shape.canUsePoints) return UnlockResult.InvalidShape
        if (!points.hasEnough(shape.pointsCost)) return UnlockResult.InsufficientPoints
        unlocking = true
        return try {
            val ok = points.debit(shape.pointsCost, TransactionType.SHAPE_UNLOCK)
            if (ok) { shapeStore.setOwned(shapeId); UnlockResult.Unlocked }
            else UnlockResult.InsufficientPoints
        } finally { unlocking = false }
    }

    @Synchronized
    fun recordAdForShape(shapeId: String, adId: String): UnlockResult {
        val shape = ShapeCatalog.byId(shapeId) ?: return UnlockResult.InvalidShape
        if (isOwned(shapeId)) return UnlockResult.AlreadyOwned
        if (!shape.needsAds) return UnlockResult.InvalidShape
        val newCount = shapeStore.incrementAds(shapeId)
        return if (newCount >= shape.adCount) {
            shapeStore.setOwned(shapeId)
            UnlockResult.Unlocked
        } else UnlockResult.AdProgressSaved
    }

    fun setActive(shapeId: String): Boolean {
        if (!isOwned(shapeId)) return false
        shapeStore.setActiveShapeId(shapeId)
        return true
    }
}
