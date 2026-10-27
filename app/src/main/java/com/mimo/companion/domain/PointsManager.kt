package com.mimo.companion.domain

import java.util.UUID

/**
 * The only code path that changes the Mimo Points balance.
 *
 * Guarantees:
 *  - the balance never goes negative;
 *  - every change writes exactly one ledger row, in the same transaction as the balance update;
 *  - a transaction id is booked at most once (idempotency): replays return Duplicate and change nothing.
 */
class PointsManager(
    private val storage: LedgerStorage,
    private val time: TimeSource = SystemTimeSource,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
) {
    fun balance(): Long = storage.getBalance()

    fun history(limit: Int = 100): List<PointsTransaction> = storage.transactions(limit)

    fun credit(
        amount: Long,
        type: TransactionType,
        refId: String? = null,
        note: String? = null,
        txId: String = idGenerator(),
    ): CreditResult = try {
        storage.inTransaction { creditWithin(amount, type, refId, note, txId) }
    } catch (e: Exception) {
        CreditResult.Failed(e.message ?: e.javaClass.simpleName)
    }

    fun debit(
        amount: Long,
        type: TransactionType,
        refId: String? = null,
        note: String? = null,
        txId: String = idGenerator(),
    ): DebitResult = try {
        storage.inTransaction { debitWithin(amount, type, refId, note, txId) }
    } catch (e: Exception) {
        DebitResult.Failed(e.message ?: e.javaClass.simpleName)
    }

    /** One-time welcome bonus. Safe to call on every start: the fixed id makes it a no-op after the first time. */
    fun grantWelcomeBonus(points: Int): CreditResult =
        if (points <= 0) CreditResult.Failed("no welcome bonus configured")
        else credit(points.toLong(), TransactionType.WELCOME_BONUS, txId = WELCOME_BONUS_TX_ID, note = "welcome")

    /**
     * Manual correction / refund. A negative [delta] can never push the balance below zero:
     * it fails with InsufficientFunds instead.
     */
    fun adjust(delta: Long, note: String, txId: String): Any =
        if (delta >= 0) credit(delta, TransactionType.REFUND_OR_ADJUSTMENT, note = note, txId = txId)
        else debit(-delta, TransactionType.REFUND_OR_ADJUSTMENT, note = note, txId = txId)

    // ---- building blocks that must run inside an enclosing storage transaction -----------------

    internal fun creditWithin(amount: Long, type: TransactionType, refId: String?, note: String?, txId: String): CreditResult {
        if (amount <= 0) return CreditResult.Failed("amount must be positive")
        val after = storage.getBalance() + amount
        val inserted = storage.insertTransaction(PointsTransaction(txId, type, amount, after, refId, note, time.nowMillis()))
        if (!inserted) return CreditResult.Duplicate
        storage.setBalance(after)
        return CreditResult.Success(after)
    }

    internal fun debitWithin(amount: Long, type: TransactionType, refId: String?, note: String?, txId: String): DebitResult {
        if (amount <= 0) return DebitResult.Failed("amount must be positive")
        val before = storage.getBalance()
        if (before < amount) return DebitResult.InsufficientFunds(required = amount, available = before)
        val after = before - amount
        val inserted = storage.insertTransaction(PointsTransaction(txId, type, -amount, after, refId, note, time.nowMillis()))
        if (!inserted) return DebitResult.Duplicate
        storage.setBalance(after)
        return DebitResult.Success(after)
    }

    companion object {
        const val WELCOME_BONUS_TX_ID = "WELCOME_BONUS"
        fun adRewardTxId(eventId: String) = "AD_REWARD:$eventId"
        fun unlockTxId(skinId: String) = "UNLOCK:$skinId"
        fun purchaseTxId(orderKey: String) = "PURCHASE:$orderKey"
    }
}
