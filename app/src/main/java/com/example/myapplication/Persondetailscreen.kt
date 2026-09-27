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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


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


private val transactionDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
private val monthShortFormat = SimpleDateFormat("MMM", Locale.getDefault())
private val dayNumberFormat = SimpleDateFormat("dd", Locale.getDefault())


private fun parseTransactionDate(date: String): Date? =
    try { transactionDateFormat.parse(date) } catch (e: Exception) { null }

// ---------- SCREEN ----------
@Composable
fun PersonDetailsScreen(
    person: PersonDetails = PersonDetails(
        name = samplePeople.first().name,
        amount = samplePeople.first().amount
    ),
    onAddExpenseClick: () -> Unit = {},
    onTransactionClick: (PersonExpense) -> Unit = {},
    onSettleUpClick: () -> Unit = {}
) {
    var realTransactions by remember { mutableStateOf<List<PersonExpense>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(person.name) {
        getPersonTransactions(person.name) { transactions ->
            realTransactions = transactions
            isLoading = false
            println("📱 Loaded ${transactions.size} transactions for ${person.name}")
        }
    }


    val balances = remember(realTransactions) {
        realTransactions
            .groupBy { it.currency }
            .mapValues { (_, rows) -> rows.sumOf { it.anotherPerson }.toMoney() }
            .filterValues { kotlin.math.abs(it) > 0.005 }
    }
    val isFullySettled = balances.isEmpty()


    var showSettledExpenses by remember(person.name) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGray)
            .verticalScroll(rememberScrollState())
    ) {
        HeaderRow(person, balances = balances)

        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            Button(
                onClick = onSettleUpClick,
                colors = ButtonDefaults.buttonColors(containerColor = Red)
            ) {
                Text("Settle Up")
            }
        }

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
        } else if (isFullySettled) {
            SettledUpBanner(
                personName = person.name,
                showingSettled = showSettledExpenses,
                onToggle = { showSettledExpenses = !showSettledExpenses }
            )
            if (showSettledExpenses) {
                TransactionHistorySection(realTransactions, onTransactionClick)
            }
        } else {
            TransactionHistorySection(realTransactions, onTransactionClick)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}


@Composable
private fun SettledUpBanner(
    personName: String,
    showingSettled: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "🎉 You are all settled up with $personName",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("✅", fontSize = 56.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (showingSettled) "Hide settled expenses" else "Tap to show settled expenses",
            fontSize = 13.sp,
            color = Purple,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onToggle() }
        )
    }
}

@Composable
private fun HeaderRow(person: PersonDetails, balances: Map<String, Double>) {
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
                    // Positive = you are owed (green, "+"). Negative = you owe (red, "-").
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

@Composable
private fun TransactionHistorySection(
    transactions: List<PersonExpense>,
    onTransactionClick: (PersonExpense) -> Unit
) {

    val sorted = transactions.sortedByDescending {
        parseTransactionDate(it.date)?.time ?: Long.MIN_VALUE
    }


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
                TransactionRowFromFirebase(transaction, onClick = { onTransactionClick(transaction) })
            }
        }
    }
}

@Composable
private fun TransactionRowFromFirebase(expense: PersonExpense, onClick: () -> Unit) {

    val isSettlement = isSettlement(expense)
    val parsedDate = parseTransactionDate(expense.date)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(Color.White, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

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

        if (isSettlement) {
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.expenseName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(settlementDisplayText(expense), fontSize = 11.sp, color = Gray)
            }
        } else {
            val payer = payerLabel(expense)
            val (direction, amount) = directionAndAmount(expense)

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
}
