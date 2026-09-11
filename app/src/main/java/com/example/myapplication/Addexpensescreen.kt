package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * FILE PURPOSE: "Add Expense" form.
 *
 * Name is a dropdown now, not free text — options come from samplePeople
 * in GroupsAndPeople.kt, not a second hardcoded list here.
 *
 * Submitting appends to an in-memory list (for the on-screen preview)
 * AND sends just that one new entry to Firebase.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen() {
    // pulled from GroupsAndPeople.kt — one source of truth, not duplicated here
    val personNames = samplePeople.map { it.name }

    val expenses = remember { mutableStateListOf<PersonExpense>() }

    var name by remember { mutableStateOf<String?>(null) } // null = nothing picked yet
    var nameDropdownExpanded by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf("") }
    var expenseName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var uploadStatus by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Add Expense", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // ---- name dropdown ----
        ExposedDropdownMenuBox(
            expanded = nameDropdownExpanded,
            onExpandedChange = { nameDropdownExpanded = it }
        ) {
            OutlinedTextField(
                value = name ?: "",
                onValueChange = {}, // typing disabled — pick from the list only
                readOnly = true,
                label = { Text("Name") },
                placeholder = { Text("Choose from the list") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = nameDropdownExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = nameDropdownExpanded,
                onDismissRequest = { nameDropdownExpanded = false }
            ) {
                personNames.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            name = option
                            nameDropdownExpanded = false
                        }
                    )
                }
            }
        }
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

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        Text("Amount is Equally Divided", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // amountText.toDouble() would CRASH if this field is empty
                // or not a number — toDoubleOrNull() + this guard stops that
                val amountValue = amountText.toDoubleOrNull()
                if (name == null || expenseName.isBlank() || amountValue == null) return@Button

                val newExpense = PersonExpense(
                    name = name!!, // safe — guarded by the check above
                    date = date,
                    expenseName = expenseName,
                    Amount = amountText,
                    Debit = (-(amountValue / 2)).toString(),
                    credit = (amountValue / 2).toString()
                )

                expenses.add(newExpense) // keeps the on-screen list below in sync

                // only this one new entry gets sent — not the whole list —
                // otherwise every click would re-upload everything added before it
                uploadPersonExpenses(listOf(newExpense)) { success, message ->
                    uploadStatus = if (success) "✅ $message" else "❌ $message"
                }

                // clear the form — name stays as the last-picked person,
                // not reset, since you're likely adding another expense
                // for them right after
                date = ""
                expenseName = ""
                amountText = ""
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
                        "${expense.date} · Credit: ${expense.credit} · Debit: ${expense.Debit}",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}