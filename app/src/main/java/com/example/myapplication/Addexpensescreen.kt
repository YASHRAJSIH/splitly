package com.example.myapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * FILE PURPOSE: "Add Expense" form.
 *
 * Right now: submitting appends a PersonExpense to an in-memory list
 * (mutableStateListOf) so you can see it actually working — nothing
 * leaves the phone.
 *
 * Next step (not done here): once this feels right, swap the local
 * `expenses.add(...)` call for uploadPersonExpenses(listOf(newExpense))
 * from FirebaseRepository.kt to push it to the real database instead.
 *
 * Reuses PersonExpense from PersonExpenseData.kt — same shape Firebase
 * is already set up to accept, so no data model needs to change later.
 */
@Composable
fun AddExpenseScreen(onBackClick: () -> Unit = {}) {
    // Lives only while this screen is in memory — lost on screen rotation.
    // Fine for testing; move to a ViewModel later if that becomes a problem.
    val expenses = remember { mutableStateListOf<PersonExpense>() }

    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var expenseName by remember { mutableStateOf("") }
    var creditText by remember { mutableStateOf("") }
    var debitText by remember { mutableStateOf("") }
    var uploadStatus by remember { mutableStateOf<String?>(null) }   // shows result of the last save

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = "Back",
            modifier = Modifier.clickable { onBackClick() }
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text("Add Expense", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Date (e.g. Sep 10, 2026)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = expenseName,
            onValueChange = { expenseName = it },
            label = { Text("Expense Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = creditText,
                onValueChange = { creditText = it },
                label = { Text("Credit") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = debitText,
                onValueChange = { debitText = it },
                label = { Text("Debit") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // don't add junk rows if the required fields are empty
                if (name.isBlank() || expenseName.isBlank()) return@Button

                val newExpense = PersonExpense(
                    name = name,
                    date = date,
                    expenseName = expenseName,
                   // credit = creditText.toDoubleOrNull() ?: 0.0,
                    //debit = debitText.toDoubleOrNull() ?: 0.0
                )

                expenses.add(newExpense) // keeps the on-screen list below in sync

                // only this one new entry gets sent — not the whole list —
                // otherwise every click would re-upload everything added before it
                uploadPersonExpenses(listOf(newExpense)) { success, message ->
                    uploadStatus = if (success) "✅ $message" else "❌ $message"
                }

                // clear the form so it's ready for the next entry
                name = ""
                date = ""
                expenseName = ""
                creditText = ""
                debitText = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Expense")
        }

        uploadStatus?.let { status ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(status, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Added so far (${expenses.size})", fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        // proves the list is actually filling up — remove once you trust it
        LazyColumn {
            items(expenses) { expense ->
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Text("${expense.name} — ${expense.expenseName}", fontWeight = FontWeight.SemiBold)
                    Text(
                        "${expense.date} · Credit: will be · Debit: will be ",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}