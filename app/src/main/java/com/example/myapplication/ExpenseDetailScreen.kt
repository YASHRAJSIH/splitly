package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Purple = Color(0xFF5B6EF5)
private val Green = Color(0xFF2ECC71)
private val Red = Color(0xFFE67E22)
private val Gray = Color(0xFF9AA0A6)
private val BgGray = Color(0xFFF5F6FA)

@Composable
fun ExpenseDetailScreen(
    personName: String,
    expenseKey: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit
) {
    var expense by remember { mutableStateOf<PersonExpense?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(personName) {
        val cancel = getPersonTransactions(personName) { transactions ->
            expense = transactions.firstOrNull { it.key == expenseKey }
            isLoading = false
        }
        onDispose { cancel() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGray)
    ) {
        // ---- TOP BAR ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { /* not built yet — see file header */ }) {
                Text("📷", fontSize = 20.sp)
            }
            IconButton(
                onClick = { showDeleteConfirm = true },
                enabled = expense != null && !isDeleting
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
            IconButton(onClick = onEdit, enabled = expense != null) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit")
            }
        }

        when {
            isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            expense == null -> Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) { Text("This expense no longer exists.", color = Gray) }

            else -> ExpenseDetailBody(expense!!, personName)
        }

        deleteError?.let {
            Text(it, color = Red, modifier = Modifier.padding(16.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this expense?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val current = expense ?: return@TextButton
                        isDeleting = true
                        deletePersonExpense(current) { success, message ->
                            isDeleting = false
                            showDeleteConfirm = false
                            if (success) onDeleted() else deleteError = message
                        }
                    }
                ) { Text("Delete", color = Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ExpenseDetailBody(expense: PersonExpense, personName: String) {
    val isSettlement = isSettlement(expense)

    Column(modifier = Modifier.padding(20.dp)) {
        Text(expense.expenseName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "${symbolFor(expense.currency)} ${"%.2f".format(expense.amount)}",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text("Added on ${expense.date}", fontSize = 13.sp, color = Gray)

        Spacer(modifier = Modifier.height(24.dp))

        if (isSettlement) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Gray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💵", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(settlementDisplayText(expense), fontSize = 14.sp, color = Gray)
            }
        } else {
            val (direction, amount) = directionAndAmount(expense)
            val payer = payerLabel(expense) ?: "Someone"

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Purple, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        payer.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "$payer paid ${symbolFor(expense.currency)} ${"%.2f".format(expense.amount)}",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (direction) {
                            TransactionDirection.LENT ->
                                "$personName owes ${symbolFor(expense.currency)} ${"%.2f".format(amount)}"
                            TransactionDirection.BORROWED ->
                                "You owe $personName ${symbolFor(expense.currency)} ${"%.2f".format(amount)}"
                            TransactionDirection.SETTLED -> "Settled up"
                        },
                        fontSize = 13.sp,
                        color = when (direction) {
                            TransactionDirection.LENT -> Green
                            TransactionDirection.BORROWED -> Red
                            TransactionDirection.SETTLED -> Gray
                        }
                    )
                }
            }
        }
    }
}