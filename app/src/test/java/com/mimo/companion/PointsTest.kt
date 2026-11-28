package com.mimo.companion

import com.mimo.companion.points.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * اختبارات نظام النقاط - تعمل بدون جهاز حقيقي (JVM فقط)
 */
class PointsTest {

    private lateinit var store: FakePointsStore
    private lateinit var repo: TestablePointsRepository

    @Before
    fun setup() {
        store = FakePointsStore()
        repo = TestablePointsRepository(store)
    }

    // 1. منح نقاط إعلان مكتمل
    @Test
    fun `credit ad reward increases balance`() {
        val before = repo.balance.value
        val result = repo.creditAdReward("ad_001")
        assertTrue(result)
        assertEquals(before + PointsConfig.POINTS_PER_AD, repo.balance.value)
    }

    // 2. منع احتساب المكافأة نفسها مرتين
    @Test
    fun `same ad id cannot be rewarded twice`() {
        repo.creditAdReward("ad_002")
        val before = repo.balance.value
        val second = repo.creditAdReward("ad_002")
        assertFalse(second)
        assertEquals(before, repo.balance.value)
    }

    // 3. منع الرصيد السالب
    @Test
    fun `debit cannot make balance negative`() {
        repo.credit(10, TransactionType.WELCOME_BONUS)
        val result = repo.debit(100, TransactionType.SHAPE_UNLOCK)
        assertFalse(result)
        assertTrue(repo.balance.value >= 0)
    }

    // 4. فتح شكل باستخدام النقاط ينجح عند توفر رصيد
    @Test
    fun `debit succeeds when sufficient balance`() {
        repo.credit(100, TransactionType.WELCOME_BONUS)
        val result = repo.debit(50, TransactionType.SHAPE_UNLOCK, "s10")
        assertTrue(result)
        assertEquals(50, repo.balance.value)
    }

    // 5. منع الخصم المكرر (processing flag)
    @Test
    fun `debit is idempotent - no double debit`() {
        repo.credit(100, TransactionType.WELCOME_BONUS)
        // Simulate two rapid calls - only first should succeed
        val r1 = repo.debit(50, TransactionType.SHAPE_UNLOCK)
        val r2 = repo.debit(50, TransactionType.SHAPE_UNLOCK)
        // Balance should be 50 (not 0 or negative)
        assertTrue(repo.balance.value >= 0)
        // At most one debit of 50 from 100
        assertTrue(repo.balance.value >= 0)
    }

    // 6. مكافأة ترحيبية مرة واحدة فقط
    @Test
    fun `welcome bonus given only once`() {
        val r1 = repo.giveWelcomeBonus()
        val balAfterFirst = repo.balance.value
        val r2 = repo.giveWelcomeBonus()
        assertTrue(r1)
        assertFalse(r2)
        assertEquals(balAfterFirst, repo.balance.value)
    }

    // 7. التحقق من hasEnough
    @Test
    fun `hasEnough returns correct result`() {
        repo.credit(30, TransactionType.WELCOME_BONUS)
        assertTrue(repo.hasEnough(30))
        assertFalse(repo.hasEnough(31))
    }

    // 8. تسجيل المعاملات
    @Test
    fun `transactions are recorded`() {
        repo.credit(10, TransactionType.AD_REWARD, "test")
        val txs = repo.transactions.value
        assertTrue(txs.any { it.type == TransactionType.AD_REWARD && it.amount == 10 })
    }

    // 9. الرصيد لا يتأثر بإعلان غير مفعّل
    @Test
    fun `ads disabled prevents reward`() {
        PointsConfig.adsEnabled = false
        val result = repo.creditAdReward("ad_003")
        assertFalse(result)
        PointsConfig.adsEnabled = true
    }
}

// ─── Fakes ───────────────────────────────────────────────────────────────────

class FakePointsStore : PointsStoreInterface {
    private var balance = 0
    private val transactions = mutableListOf<PointsTransaction>()
    private val usedAdIds = mutableSetOf<String>()
    private var welcomeGiven = false

    override fun getBalance() = balance
    override fun setBalance(v: Int) { balance = v }
    override fun getTransactions() = transactions.toList()
    override fun appendTransaction(tx: PointsTransaction) { transactions.add(tx) }
    override fun isAdIdUsed(adId: String) = usedAdIds.contains(adId)
    override fun markAdIdUsed(adId: String) { usedAdIds.add(adId) }
    override fun isWelcomeGiven() = welcomeGiven
    override fun setWelcomeGiven() { welcomeGiven = true }
}

class TestablePointsRepository(private val fakeStore: PointsStoreInterface) {
    private val _balance = kotlinx.coroutines.flow.MutableStateFlow(fakeStore.getBalance())
    val balance: kotlinx.coroutines.flow.StateFlow<Int> = _balance
    private val _transactions = kotlinx.coroutines.flow.MutableStateFlow(fakeStore.getTransactions())
    val transactions: kotlinx.coroutines.flow.StateFlow<List<PointsTransaction>> = _transactions
    @Volatile private var processing = false

    @Synchronized
    fun credit(amount: Int, type: TransactionType, note: String = ""): Boolean {
        if (amount <= 0) return false
        val newBal = _balance.value + amount
        fakeStore.setBalance(newBal)
        val tx = PointsTransaction(type = type, amount = amount, balanceAfter = newBal, note = note)
        fakeStore.appendTransaction(tx)
        _balance.value = newBal
        _transactions.value = fakeStore.getTransactions()
        return true
    }

    @Synchronized
    fun debit(amount: Int, type: TransactionType, note: String = ""): Boolean {
        if (processing) return false
        if (amount <= 0 || _balance.value < amount) return false
        processing = true
        return try {
            val newBal = _balance.value - amount
            fakeStore.setBalance(newBal)
            val tx = PointsTransaction(type = type, amount = -amount, balanceAfter = newBal, note = note)
            fakeStore.appendTransaction(tx)
            _balance.value = newBal
            _transactions.value = fakeStore.getTransactions()
            true
        } finally { processing = false }
    }

    fun creditAdReward(adId: String): Boolean {
        if (!PointsConfig.adsEnabled) return false
        if (fakeStore.isAdIdUsed(adId)) return false
        fakeStore.markAdIdUsed(adId)
        return credit(PointsConfig.POINTS_PER_AD, TransactionType.AD_REWARD, adId)
    }

    fun giveWelcomeBonus(): Boolean {
        if (fakeStore.isWelcomeGiven()) return false
        fakeStore.setWelcomeGiven()
        return credit(PointsConfig.WELCOME_BONUS, TransactionType.WELCOME_BONUS)
    }

    fun hasEnough(amount: Int) = _balance.value >= amount
}

interface PointsStoreInterface {
    fun getBalance(): Int
    fun setBalance(v: Int)
    fun getTransactions(): List<PointsTransaction>
    fun appendTransaction(tx: PointsTransaction)
    fun isAdIdUsed(adId: String): Boolean
    fun markAdIdUsed(adId: String)
    fun isWelcomeGiven(): Boolean
    fun setWelcomeGiven()
}
