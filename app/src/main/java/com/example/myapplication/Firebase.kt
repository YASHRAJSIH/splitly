package com.example.myapplication

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Exclude
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.math.round

data class PersonExpense(
    val name: String = "",
    val date: String = "",
    val expenseName: String = "",
    val amount: Double = 0.0,
    val currency: String = "USD",
    val splitMethod: String = "",
    val accountHolder: Double = 0.0,
    val anotherPerson: Double = 0.0,
    @get:Exclude val key: String = "",
)



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


private val illegalKeyChars = Regex("[.#$\\[\\]/]")

fun personKey(name: String): String = name.trim().replace(illegalKeyChars, "_")

private val database = FirebaseDatabase.getInstance()
private val expensesRef = database.reference.child("expenses")

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

fun deletePersonExpense(
    expense: PersonExpense,
    onComplete: (success: Boolean, message: String) -> Unit = { _, _ -> }
) {
    if (expense.key.isBlank()) {
        onComplete(false, "This expense has no id — can't delete it safely")
        return
    }

    val updates: Map<String, Any?> = mapOf(
        "expenses/${expense.key}" to null,
        "people/${personKey(expense.name)}/transactions/${expense.key}" to null
    )

    database.reference.updateChildren(updates)
        .addOnSuccessListener { onComplete(true, "Deleted") }
        .addOnFailureListener { error -> onComplete(false, error.message ?: "Delete failed") }
}

fun updatePersonExpense(
    updated: PersonExpense,
    onComplete: (success: Boolean, message: String) -> Unit = { _, _ -> }
) {
    if (updated.key.isBlank()) {
        onComplete(false, "This expense has no id — can't update it safely")
        return
    }

    val updates = mapOf<String, Any>(
        "expenses/${updated.key}" to updated,
        "people/${personKey(updated.name)}/transactions/${updated.key}" to updated
    )

    database.reference.updateChildren(updates)
        .addOnSuccessListener { onComplete(true, "Updated") }
        .addOnFailureListener { error -> onComplete(false, error.message ?: "Update failed") }
}

fun computeSplitAmounts(
    splitMethod: String,
    amountValue: Double,
    lumSumAccountHolder: Double? = null,
    lumSumOtherPerson: Double? = null,
): Pair<Double, Double> = when (splitMethod) {
    "You Paid - Split Equally" ->
        -(amountValue / 2) to (amountValue / 2)

    "You Owed - Full Amount" ->
        -amountValue to 0.0

    "Another Person Paid - Split Equally" ->
        (amountValue / 2) to -(amountValue / 2)

    "Another Person Owed - Full Amount" ->
        0.0 to -amountValue

    "LumSum" ->
        -(lumSumAccountHolder ?: 0.0) to (lumSumOtherPerson ?: 0.0)
    SETTLEMENT_YOU_PAID -> -amountValue to amountValue
    SETTLEMENT_THEY_PAID -> amountValue to -amountValue

    else -> 0.0 to 0.0
}

const val SETTLEMENT_YOU_PAID = "Settlement - You Paid"
const val SETTLEMENT_THEY_PAID = "Settlement - They Paid"

fun isSettlement(expense: PersonExpense): Boolean =
    expense.splitMethod == SETTLEMENT_YOU_PAID || expense.splitMethod == SETTLEMENT_THEY_PAID
fun settlementDisplayText(expense: PersonExpense): String = when (expense.splitMethod) {
    SETTLEMENT_YOU_PAID -> "You paid ${expense.name} ${symbolFor(expense.currency)} ${"%.2f".format(expense.amount)}"
    SETTLEMENT_THEY_PAID -> "${expense.name} paid you ${symbolFor(expense.currency)} ${"%.2f".format(expense.amount)}"
    else -> ""
}


private fun DataSnapshot.toPersonExpenseOrNull(): PersonExpense? =
    try {
        // key here is DataSnapshot.key (this row's Firebase push id) — attached
        // after deserialising, since @get:Exclude means getValue() never sets it.
        getValue(PersonExpense::class.java)?.copy(key = key ?: "")
    } catch (e: Exception) {
        println("⚠️ Skipping unreadable row ${key}: ${e.message}")
        null
    }

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

fun totalsPerPerson(expenses: List<PersonExpense>): Map<String, Double> =
    expenses
        .groupBy { it.name.trim() }
        .mapValues { (_, rows) -> rows.sumOf { it.anotherPerson }.toMoney() }

fun totalsPerPersonSorted(expenses: List<PersonExpense>): List<Pair<String, Double>> =
    totalsPerPerson(expenses).toList().sortedBy { it.second }


data class BalanceSummary(
    val getBack: Double,  // sum of negative per-person totals, flipped to positive
    val due: Double,      // sum of positive per-person totals
    val net: Double       // getBack - due
)

fun balanceSummary(expenses: List<PersonExpense>): BalanceSummary {
    val totals = totalsPerPerson(expenses).values

    val due = totals.filter { it < 0 }.sum()
    val getBack = totals.filter { it > 0 }.sum()

    return BalanceSummary(
        getBack = getBack.toMoney(),
        due = due.toMoney(),
        net = (getBack + due).toMoney()
    )
}

enum class TransactionDirection { LENT, BORROWED, SETTLED }

fun directionAndAmount(expense: PersonExpense): Pair<TransactionDirection, Double> {
    val (direction, magnitude) = when (expense.splitMethod) {
        "You Paid - Split Equally" ->
            TransactionDirection.LENT to expense.anotherPerson

        "You Owed - Full Amount" ->
            TransactionDirection.BORROWED to -expense.accountHolder

        "Another Person Paid - Split Equally" ->
            TransactionDirection.BORROWED to -expense.anotherPerson

        "Another Person Owed - Full Amount" ->
            TransactionDirection.LENT to -expense.anotherPerson


        "LumSum" ->
            if (expense.anotherPerson >= 0) TransactionDirection.LENT to expense.anotherPerson
            else TransactionDirection.BORROWED to -expense.anotherPerson


        else ->
            if (expense.anotherPerson >= 0) TransactionDirection.LENT to expense.anotherPerson
            else TransactionDirection.BORROWED to -expense.anotherPerson
    }
    return if (magnitude == 0.0) TransactionDirection.SETTLED to 0.0 else direction to magnitude
}


fun payerLabel(expense: PersonExpense): String? = when (expense.splitMethod) {
    "You Paid - Split Equally", "Another Person Owed - Full Amount", "LumSum" -> "You"
    "You Owed - Full Amount", "Another Person Paid - Split Equally" -> expense.name
    else -> null // unknown legacy method (splitMethod == "") — don't guess
}