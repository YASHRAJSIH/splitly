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
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Transactions (${transactions.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        transactions.forEach { transaction ->
            TransactionRowFromFirebase(transaction)
        }
    }
}

@Composable
private fun TransactionRowFromFirebase(expense: PersonExpense) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(Color.White, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon placeholder
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
            Text(expense.date, fontSize = 11.sp, color = Gray)
           // Text(expense.amount, fontSize = 11.sp, color = Gray)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                expense.amount.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun PersonDetailsScreenPreview() {
    PersonDetailsScreen()
}