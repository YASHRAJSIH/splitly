package com.example.myapplication

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.math.round

/**
 * FILE PURPOSE: PersonExpense model + Firebase read/write.
 *
 * MONEY MODEL
 *   amount         the bill total as a plain number. No symbol, no currency code, no text.
 *   currency       ISO code that `amount` is denominated in. Formatting happens in the UI.
 *   accountHolder  YOUR net position for this expense.  + = you are owed,  - = you owe.
 *   anotherPerson  The other party's net position. Always exactly -accountHolder.
 *
 * INVARIANT: accountHolder + anotherPerson == 0.0 on every single row.
 * Because of that, "what do I owe Ravi" is just: sum(accountHolder) over Ravi's
 * transactions. No separate balance table, nothing to keep in sync, nothing to drift.
 *
 * Firebase derives JSON keys from the Kotlin getters, so the stored keys are
 * exactly: name, date, expenseName, amount, currency, accountHolder, anotherPerson.
 * Property names start lowercase on purpose — capitalised first letters make the
 * derived key ambiguous, which is how you end up with both "Amount" and "amount"
 * in the same database.
 */

data class PersonExpense(
    val name: String = "",
    val date: String = "",
    val expenseName: String = "",
    val amount: Double = 0.0,
    val currency: String = "USD",
    val accountHolder: Double = 0.0,
    val anotherPerson: Double = 0.0,
)

// ============ MONEY HELPERS ============

/**
 * Snaps a Double to 2 decimal places. Double cannot represent 0.1 exactly, so
 * every division (splitting a bill) leaves dust like 21.249999999999996.
 * Round at the point of calculation, never at the point of display, or the dust
 * compounds and two people's balances stop agreeing.
 */
fun Double.toMoney(): Double = round(this * 100.0) / 100.0

val currencySymbols = mapOf(
    "USD" to "$", "EUR" to "€", "GBP" to "£", "INR" to "₹",
    "JPY" to "¥", "AUD" to "A$", "CAD" to "C$", "CHF" to "CHF",
    "CNY" to "¥", "MXN" to "$"
)

fun symbolFor(currency: String): String = currencySymbols[currency] ?: currency

/** Display only. Never write this string back into the database. */
fun PersonExpense.displayAmount(): String =
    "${symbolFor(currency)} ${"%.2f".format(amount)} ($currency)"

// ============ FIREBASE KEYS ============

/**
 * Firebase rejects keys containing . # $ [ ] / — a person named "Dr. Shah" would
 * crash the write. This sanitises the name so it stops crashing today.
 *
 * It is NOT the real fix: two people whose names sanitise to the same string share
 * one transaction history, and renaming a person orphans their whole history.
 * The real fix is a stable generated id on your person object, with the name as a
 * plain display field. That needs a change to samplePeople, which I don't have.
 */
private val illegalKeyChars = Regex("[.#$\\[\\]/]")

fun personKey(name: String): String = name.trim().replace(illegalKeyChars, "_")

private val database = FirebaseDatabase.getInstance()
private val expensesRef = database.reference.child("expenses")

// ============ WRITE ============

/**
 * One atomic multi-path update instead of nested write-then-write.
 *
 * The old version pushed to /expenses, waited for success, then pushed to
 * /people/<name>/transactions with a *different* key. Two failure modes came free
 * with that: the second write could fail leaving the global list and the person's
 * history disagreeing, and onComplete fired once per expense inside a forEach, so
 * the caller's navigation ran N times.
 *
 * updateChildren applies every path or none, and calls back exactly once. Both
 * copies share the same key, so a future edit or delete can reach both.
 */
fun uploadPersonExpenses(
    expenses: List<PersonExpense>,
    onComplete: (success: Boolean, message: String) -> Unit = { _, _ -> }
) {
    if (expenses.isEmpty()) {
        onComplete(false, "Nothing to upload")
        return
    }

    val updates = mutableMapOf<String, Any>()

    for (expense in expenses) {
        if (expense.name.isBlank()) {
            onComplete(false, "Expense has no person attached")
            return
        }
        val key = expensesRef.push().key
        if (key == null) {
            onComplete(false, "Could not generate a key")
            return
        }
        updates["expenses/$key"] = expense
        updates["people/${personKey(expense.name)}/transactions/$key"] = expense
    }

    database.reference.updateChildren(updates)
        .addOnSuccessListener {
            onComplete(true, "Saved ${expenses.size} expense(s)")
        }
        .addOnFailureListener { error ->
            onComplete(false, error.message ?: "Upload failed")
        }
}

// ============ READ ============

/**
 * Parses one snapshot without taking the screen down with it.
 *
 * getValue() throws DatabaseException on a type mismatch — and every row written by
 * the old code has amount/credit/Debit as Strings, which no longer match. One
 * legacy row would otherwise crash inside onDataChange.
 */
private fun DataSnapshot.toPersonExpenseOrNull(): PersonExpense? =
    try {
        getValue(PersonExpense::class.java)
    } catch (e: Exception) {
        println("⚠️ Skipping unreadable row ${key}: ${e.message}")
        null
    }

/**
 * Returns a cancel handle. Call it from onDispose in the composable — an
 * addValueEventListener that is never removed keeps firing after the screen is
 * gone, which is a leak and a source of "why did my UI update twice" bugs.
 */
fun getPersonTransactions(
    personName: String,
    onComplete: (transactions: List<PersonExpense>) -> Unit
): () -> Unit {
    val ref = database.reference
        .child("people")
        .child(personKey(personName))
        .child("transactions")

    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val transactions = snapshot.children.mapNotNull { it.toPersonExpenseOrNull() }
            println("📖 Loaded ${transactions.size} transactions for $personName")
            onComplete(transactions)
        }

        override fun onCancelled(error: DatabaseError) {
            println("❌ Failed to read transactions: ${error.message}")
            onComplete(emptyList())
        }
    }

    ref.addValueEventListener(listener)
    return { ref.removeEventListener(listener) }
}


fun getAllExpenses(
    onComplete: (expenses: List<PersonExpense>) -> Unit
): () -> Unit {
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val expenses = snapshot.children.mapNotNull { it.toPersonExpenseOrNull() }
            println("📖 Loaded ${expenses.size} total expenses")
            onComplete(expenses)
        }

        override fun onCancelled(error: DatabaseError) {
            println("❌ Failed to read expenses: ${error.message}")
            onComplete(emptyList())
        }
    }

    expensesRef.addValueEventListener(listener)
    return { expensesRef.removeEventListener(listener) }
}

/** Your net balance with one person. Positive = they owe you. */
fun netBalance(transactions: List<PersonExpense>): Double =
    transactions.sumOf { it.accountHolder }.toMoney()