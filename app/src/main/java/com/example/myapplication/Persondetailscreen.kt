package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * FILE PURPOSE: "Person Details" screen with real Firebase data.
 * - Back button, name, net balance
 * - Real transaction history from Firebase
 * - Add Expense button
 */

private val Purple = Color(0xFF5B6EF5)
private val Green = Color(0xFF2ECC71)
private val Red = Color(0xFFE67E22)
private val Gray = Color(0xFF9AA0A6)
private val BgGray = Color(0xFFF5F6FA)

// ---------- DATA MODELS ----------
data class PersonDetails(
    val name: String,
    val amount: Double,
)

// ---------- TRANSACTION DIRECTION ----------
// Which way a single row's debt runs, and the correct magnitude for it.
//
// This does NOT reuse the anotherPerson-sign shortcut that totalsPerPerson() in
// Firebase.kt uses for the header/list totals — that shortcut is documented there
// as backwards for "Another Person Owed - Full Amount" and blind to the debt
// entirely for "You Owed - Full Amount" (contributes 0 to the sum). Branching on
// splitMethod, per the invariant table at the top of Firebase.kt, is what makes
// every method render correctly here instead of just 3 of the 5.
private enum class TransactionDirection { LENT, BORROWED, SETTLED }

private fun directionAndAmount(expense: PersonExpense): Pair<TransactionDirection, Double> {
    val (direction, magnitude) = when (expense.splitMethod) {
        "You Paid - Split Equally" ->
            TransactionDirection.LENT to expense.anotherPerson

        "You Owed - Full Amount" ->
            TransactionDirection.BORROWED to -expense.accountHolder

        "Another Person Paid - Split Equally" ->
            TransactionDirection.BORROWED to -expense.anotherPerson

        "Another Person Owed - Full Amount" ->
            TransactionDirection.LENT to -expense.anotherPerson

        // LumSum only ever runs "you paid, they owe theirs" today, so anotherPerson
        // is always >= 0 in practice — branching on sign anyway rather than assuming
        // that never changes.
        "LumSum" ->
            if (expense.anotherPerson >= 0) TransactionDirection.LENT to expense.anotherPerson
            else TransactionDirection.BORROWED to -expense.anotherPerson

        // Rows written before splitMethod existed deserialise as "" (see the note
        // in Firebase.kt). No way to know which of the 5 methods produced them, so
        // fall back to the sum heuristic — correct for 3 of 5 methods, same as
        // totalsPerPerson().
        else ->
            if (expense.anotherPerson >= 0) TransactionDirection.LENT to expense.anotherPerson
            else TransactionDirection.BORROWED to -expense.anotherPerson
    }
    return if (magnitude == 0.0) TransactionDirection.SETTLED to 0.0 else direction to magnitude
}

// Who physically paid the bill, for the "X paid €Y" subtitle. Inferred from
// splitMethod the same way the amounts are. NOTE: "You Owed - Full Amount" has no
// explicit payer stored anywhere — the only way that debt makes sense is if the
// other person paid the whole bill and you owe it back, so that's what's shown.
// If you ever use that method for a case where you paid and just owe them back
// some other way, this line will show the wrong name — tell me and I'll change it.
private fun payerLabel(expense: PersonExpense): String? = when (expense.splitMethod) {
    "You Paid - Split Equally", "Another Person Owed - Full Amount", "LumSum" -> "You"
    "You Owed - Full Amount", "Another Person Paid - Split Equally" -> expense.name
    else -> null // unknown legacy method (splitMethod == "") — don't guess
}

private val transactionDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
private val monthShortFormat = SimpleDateFormat("MMM", Locale.getDefault())
private val dayNumberFormat = SimpleDateFormat("dd", Locale.getDefault())

// Same format the Add Expense date picker writes with. If that format ever
// changes, this has to change with it or every row silently falls into "Unknown
// date" instead of crashing — parse failures are caught, not thrown.
private fun parseTransactionDate(date: String): Date? =
    try { transactionDateFormat.parse(date) } catch (e: Exception) { null }

// ---------- SCREEN ----------
@Composable
fun PersonDetailsScreen(
    person: PersonDetails = PersonDetails(
        name = samplePeople.first().name,
        amount = samplePeople.first().amount
    ),
    onAddExpenseClick: () -> Unit = {}
) {
    // Load real transactions from Firebase
    var realTransactions by remember { mutableStateOf<List<PersonExpense>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(person.name) {
        getPersonTransactions(person.name) { transactions ->
            realTransactions = transactions
            isLoading = false
            println("📱 Loaded ${transactions.size} transactions for ${person.name}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGray)
            .verticalScroll(rememberScrollState())
    ) {
        HeaderRow(person, transactions = realTransactions )
        Spacer(modifier = Modifier.height(12.dp))

        if (realTransactions.isEmpty()) {
            if (isLoading) {
                Text(
                    "Loading transactions...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    fontSize = 14.sp,
                    color = Gray
                )
            } else {
                Text(
                    "No transactions yet. Add one!",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    fontSize = 14.sp,
                    color = Gray
                )
            }
        } else {
            TransactionHistorySection(realTransactions)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ---------- HEADER ----------
@Composable
private fun HeaderRow(person: PersonDetails, transactions: List<PersonExpense>) {
    // One balance per currency. Recomputed only when the list changes.
    val balances = remember(transactions) {
        transactions
            .groupBy { it.currency }
            .mapValues { (_, rows) -> rows.sumOf { it.anotherPerson }.toMoney() }
            .filterValues { it != 0.0 }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Purple, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                person.name.firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(person.name, fontSize = 17.sp, fontWeight = FontWeight.Bold)

            if (balances.isEmpty()) {
                Text("Settled up", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
            } else {
                balances.forEach { (currency, amount) ->
                    // anotherPerson is THEIR position: positive = they are owed = you owe.
                    Text(
                        text = "${if (amount >= 0) "+" else "-"}${symbolFor(currency)} " +
                                "%.2f".format(kotlin.math.abs(amount)),
                        fontSize = 13.sp,
                        color = if (amount >= 0) Green else Red,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ---------- TRANSACTION HISTORY ----------
@Composable
private fun TransactionHistorySection(transactions: List<PersonExpense>) {
    // Newest first, like the reference. Unparseable dates sort last instead of
    // crashing or silently vanishing.
    val sorted = transactions.sortedByDescending {
        parseTransactionDate(it.date)?.time ?: Long.MIN_VALUE
    }

    // groupBy keeps first-seen key order, so grouping an already-sorted list
    // gives newest-to-oldest month sections for free — no separate sort of the
    // groups needed.
    val byMonth = sorted.groupBy { transaction ->
        parseTransactionDate(transaction.date)?.let { monthYearFormat.format(it) }
            ?: "Unknown date"
    }

    Column(modifier = Modifier.padding(top = 20.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Transactions (${transactions.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        byMonth.forEach { (month, monthTransactions) ->
            Text(
                month,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            monthTransactions.forEach { transaction ->
                TransactionRowFromFirebase(transaction)
            }
        }
    }
}

@Composable
private fun TransactionRowFromFirebase(expense: PersonExpense) {
    val (direction, amount) = directionAndAmount(expense)
    val payer = payerLabel(expense)
    val parsedDate = parseTransactionDate(expense.date)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(Color.White, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day-of-month gutter, like the reference screenshot — this stays even
        // though rows are grouped under a month header, since the header alone
        // doesn't tell you which day within the month this happened. Falls back
        // to the raw stored string if it doesn't parse, so a bad date shows up
        // as visibly odd text instead of silently disappearing.
        Column(
            modifier = Modifier.width(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (parsedDate != null) {
                Text(monthShortFormat.format(parsedDate), fontSize = 10.sp, color = Gray)
                Text(
                    dayNumberFormat.format(parsedDate),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(expense.date, fontSize = 9.sp, color = Gray)
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Icon placeholder — one icon for every row until PersonExpense has a real
        // category field to key off. That's a schema change (plus a fallback icon
        // for every row written before it existed) — separate task, held out of
        // this bug-fix pass on purpose.
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFD9E5FB), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("💰", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(expense.expenseName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = payer?.let {
                    "$it paid ${symbolFor(expense.currency)} ${"%.2f".format(expense.amount)}"
                } ?: expense.date,
                fontSize = 11.sp,
                color = Gray
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            val (label, color) = when (direction) {
                TransactionDirection.LENT -> "you lent" to Green
                TransactionDirection.BORROWED -> "you borrowed" to Red
                TransactionDirection.SETTLED -> "settled" to Gray
            }
            Text(label, fontSize = 11.sp, color = color)
            Text(
                "${symbolFor(expense.currency)} ${"%.2f".format(amount)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun PersonDetailsScreenPreview() {
    PersonDetailsScreen()
}