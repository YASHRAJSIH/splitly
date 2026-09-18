package com.example.myapplication

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Exclude
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.math.round

/**
 * FILE PURPOSE: PersonExpense model + Firebase read/write.
 *
 * MONEY MODEL
 *   amount         the bill total as a plain number. No symbol, no currency code, no text.
 *   currency       ISO code that `amount` is denominated in. Formatting happens in the UI.
 *   splitMethod    which option the account holder picked on the Add Expense form.
 *                  Stored verbatim. This is what lets a reader interpret the two
 *                  numbers below — see the INVARIANT note.
 *   accountHolder  the account holder's side of this expense, as written by that method.
 *   anotherPerson  the other party's side, as written by that method.
 *
 * INVARIANT — READ THIS BEFORE SUMMING ANYTHING
 * The old invariant (accountHolder + anotherPerson == 0.0 on every row) no longer
 * holds. Each split method now sets both numbers explicitly, and two of them do
 * not sum to zero:
 *
 *   splitMethod                          accountHolder  anotherPerson   sum
 *   "You Paid - Split Equally"              -half          +half          0
 *   "You Owed - Full Amount"                -full            0         -full
 *   "Another Person Paid - Split Equally"   +half          -half          0
 *   "Another Person Owed - Full Amount"        0           -full       -full
 *   "LumSum"                                -mine        +theirs          0
 *
 * So neither field is a net position on its own, and the sign does not encode the
 * direction of the debt consistently. `splitMethod` is stored so a reader can
 * branch on it instead of guessing from the numbers.
 *
 * Firebase derives JSON keys from the Kotlin getters, so the stored keys are
 * exactly: name, date, expenseName, amount, currency, splitMethod, accountHolder,
 * anotherPerson. Property names start lowercase on purpose — capitalised first
 * letters make the derived key ambiguous, which is how you end up with both
 * "Amount" and "amount" in the same database.
 *
 * Rows written before splitMethod existed will deserialise with splitMethod = ""
 * (the default), not crash. Anything branching on it needs an else/unknown path.
 */

data class PersonExpense(
    val name: String = "",
    val date: String = "",
    val expenseName: String = "",
    val amount: Double = 0.0,
    val currency: String = "USD",
    val splitMethod: String = "",
    val accountHolder: Double = 0.0,
    val anotherPerson: Double = 0.0,
    // The Firebase push key this row lives under. NOT written to the database —
    // @get:Exclude keeps it out of the JSON payload, since the key already lives
    // in the path ("expenses/<key>"), not in the value stored there. Populated
    // only when reading a row back (see toPersonExpenseOrNull below); a
    // PersonExpense you build by hand before saving has key = "" and can't be
    // edited or deleted until it's been round-tripped through Firebase once.
    @get:Exclude val key: String = "",
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

/**
 * Deletes one expense from both copies (the global list and the person's own
 * transaction history) as a single atomic multi-path update — same reasoning as
 * uploadPersonExpenses: either both paths clear or neither does, so the two
 * copies never disagree after a partial failure.
 *
 * Needs expense.key, which only a row read back from Firebase has (see
 * toPersonExpenseOrNull). A PersonExpense built by hand and never saved has no
 * key and can't be deleted.
 */
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

/**
 * Overwrites an existing expense in place, at the SAME key, in both copies.
 *
 * Does not support moving an expense to a different person: `updated.name` must
 * match what the row already belongs to, or the two copies end up filed under
 * different people. Reassigning to someone else needs a delete from the old
 * person's node plus an insert under the new one — a bigger change, not built
 * here since nothing has asked for it yet.
 */
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

// ============ SPLIT CALCULATION ============

/**
 * The (accountHolder, anotherPerson) pair for a given split method — pulled out
 * of AddExpenseScreen's button handler so EditExpenseScreen can compute the
 * exact same numbers instead of carrying a second copy of this logic that could
 * quietly drift from this one. lumSumAccountHolder/lumSumOtherPerson are only
 * read for "LumSum"; pass null for every other method.
 *
 * See the WARNING in the class doc above: the sign alone never tells you the
 * direction of the debt, which is exactly why every branch here is explicit
 * rather than derived from the others.
 */
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

    // Settling up moves the balance the OPPOSITE way from an expense: paying
    // someone reduces what you owe them (or increases what they owe you),
    // rather than creating new debt. That's why the sign here is flipped
    // relative to every branch above it.
    SETTLEMENT_YOU_PAID -> amountValue to -amountValue
    SETTLEMENT_THEY_PAID -> -amountValue to amountValue

    else -> 0.0 to 0.0
}

// ============ SETTLE UP ============
// A settle-up record reduces (or reverses) a running balance instead of
// creating new debt from a shared bill. It's stored as an ordinary
// PersonExpense — same table, same read/write/aggregate path as every other
// row (totalsPerPerson, uploadPersonExpenses, deletePersonExpense, etc.) — just
// tagged with one of these two splitMethod values instead of a real split, so
// there's no second parallel data model to keep in sync.

const val SETTLEMENT_YOU_PAID = "Settlement - You Paid"
const val SETTLEMENT_THEY_PAID = "Settlement - They Paid"

fun isSettlement(expense: PersonExpense): Boolean =
    expense.splitMethod == SETTLEMENT_YOU_PAID || expense.splitMethod == SETTLEMENT_THEY_PAID

/**
 * Neutral "who paid whom" line for a settlement row. Deliberately does not use
 * directionAndAmount()/payerLabel() — those are "you lent / you borrowed"
 * framing for NEW debt, which is the wrong story for a row whose entire point
 * is paying debt down. Callers should check isSettlement() first and use this
 * instead, not run a settlement row through the expense-direction logic.
 */
fun settlementDisplayText(expense: PersonExpense): String = when (expense.splitMethod) {
    SETTLEMENT_YOU_PAID -> "You paid ${expense.name} ${symbolFor(expense.currency)} ${"%.2f".format(expense.amount)}"
    SETTLEMENT_THEY_PAID -> "${expense.name} paid you ${symbolFor(expense.currency)} ${"%.2f".format(expense.amount)}"
    else -> ""
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
        // key here is DataSnapshot.key (this row's Firebase push id) — attached
        // after deserialising, since @get:Exclude means getValue() never sets it.
        getValue(PersonExpense::class.java)?.copy(key = key ?: "")
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

/**
 * BROKEN FOR TWO SPLIT METHODS — left as-is on purpose, not yet fixed.
 *
 * This sums `anotherPerson` and treats the result as "what they owe me"
 * (positive) or "what I owe them" (negative). Against the table at the top of
 * this file:
 *
 *   "You Paid - Split Equally"             +half   -> they owe you half.  CORRECT
 *   "Another Person Paid - Split Equally"  -half   -> you owe them half.  CORRECT
 *   "LumSum"                             +theirs   -> they owe you theirs. CORRECT
 *   "You Owed - Full Amount"                   0   -> you owe them the FULL amount,
 *                                                     but this contributes nothing.
 *   "Another Person Owed - Full Amount"    -full   -> THEY owe YOU the full amount,
 *                                                     but this reads as you owing them.
 *                                                     Sign is backwards.
 *
 * Every row also has `splitMethod` now, so the direction is recoverable — the
 * function just doesn't use it yet.
 */
fun totalsPerPerson(expenses: List<PersonExpense>): Map<String, Double> =
    expenses
        .groupBy { it.name.trim() }
        .mapValues { (_, rows) -> rows.sumOf { it.anotherPerson }.toMoney() }

/** Same thing, ordered largest debt first, for feeding straight into a LazyColumn. */
fun totalsPerPersonSorted(expenses: List<PersonExpense>): List<Pair<String, Double>> =
    totalsPerPerson(expenses).toList().sortedBy { it.second }


/** Net balance split into the three numbers the card needs. All magnitudes are >= 0 except net. */
data class BalanceSummary(
    val getBack: Double,  // sum of negative per-person totals, flipped to positive
    val due: Double,      // sum of positive per-person totals
    val net: Double       // getBack - due
)

fun balanceSummary(expenses: List<PersonExpense>): BalanceSummary {
    // group first, then classify — a person with two rows that cancel out must land at 0,
    // not get counted on both sides
    val totals = totalsPerPerson(expenses).values

    val due = totals.filter { it < 0 }.sum()
    val getBack = totals.filter { it > 0 }.sum()

    return BalanceSummary(
        getBack = getBack.toMoney(),
        due = due.toMoney(),
        net = (getBack + due).toMoney()
    )
}

// ============ PER-ROW DIRECTION ============
// Which way ONE transaction's debt runs, and the correct magnitude for it — used
// by the transaction list and the expense detail screen, anywhere that needs a
// single row's "you lent / you borrowed" label rather than the totalsPerPerson()
// aggregate above.
//
// Deliberately does NOT reuse the anotherPerson-sign shortcut totalsPerPerson()
// uses — see the note on that function: it's backwards for "Another Person Owed
// - Full Amount" and blind to the debt entirely for "You Owed - Full Amount".
// Branching on splitMethod is what makes every method render correctly here.
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

        // LumSum only ever runs "you paid, they owe theirs" today, so
        // anotherPerson is always >= 0 in practice — branching on sign anyway
        // rather than assuming that never changes.
        "LumSum" ->
            if (expense.anotherPerson >= 0) TransactionDirection.LENT to expense.anotherPerson
            else TransactionDirection.BORROWED to -expense.anotherPerson

        // Rows written before splitMethod existed deserialise as "". No way to
        // know which of the 5 methods produced them, so fall back to the sum
        // heuristic — correct for 3 of 5 methods, same as totalsPerPerson().
        else ->
            if (expense.anotherPerson >= 0) TransactionDirection.LENT to expense.anotherPerson
            else TransactionDirection.BORROWED to -expense.anotherPerson
    }
    return if (magnitude == 0.0) TransactionDirection.SETTLED to 0.0 else direction to magnitude
}

// Who physically paid the bill, for a "X paid €Y" line. Inferred from
// splitMethod the same way the amounts are. NOTE: "You Owed - Full Amount" has
// no explicit payer stored anywhere — the only way that debt makes sense is if
// the other person paid the whole bill and you owe it back, so that's what's
// shown. If that's ever wrong for how you use that method, change the mapping
// below.
fun payerLabel(expense: PersonExpense): String? = when (expense.splitMethod) {
    "You Paid - Split Equally", "Another Person Owed - Full Amount", "LumSum" -> "You"
    "You Owed - Full Amount", "Another Person Paid - Split Equally" -> expense.name
    else -> null // unknown legacy method (splitMethod == "") — don't guess
}