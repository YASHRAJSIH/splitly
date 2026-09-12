package com.example.myapplication

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var showDatePicker by remember { mutableStateOf(false) } // ← Date picker state
    val datePickerState = rememberDatePickerState() // ← Capture date picker state

    // ---- CURRENCY DROPDOWN ----
    var selectedCurrency by remember { mutableStateOf("USD") }
    var currencyDropdownExpanded by remember { mutableStateOf(false) }
    val currencyOptions = listOf("USD", "EUR", "GBP", "INR", "JPY", "AUD", "CAD", "CHF", "CNY", "MXN")

    // Currency symbols mapping
    val currencySymbols = mapOf(
        "USD" to "$",
        "EUR" to "€",
        "GBP" to "£",
        "INR" to "₹",
        "JPY" to "¥",
        "AUD" to "A$",
        "CAD" to "C$",
        "CHF" to "CHF",
        "CNY" to "¥",
        "MXN" to "$"
    )

    // ---- SPLIT METHOD DROPDOWN ----
    var selectedSplitMethod by remember { mutableStateOf("You Paid - Split Equally") }
    var splitMethodDropdownExpanded by remember { mutableStateOf(false) }
    val splitMethodOptions = listOf(
        "You Paid - Split Equally",
        "You Owed - Full Amount",
        "Another Person Paid - Split Equally",
        "Another Person Owed - Full Amount",
        "Percentage Split"
    )

    // Percentage split fields
    var yourPercentage by remember { mutableStateOf("50") }
    var otherPersonPercentage by remember { mutableStateOf("50") }

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

        // ---- DATE PICKER FIELD ----
        OutlinedTextField(
            value = date,
            onValueChange = { },
            label = { Text("Date (e.g. Sep 10, 2026)") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Pick Date")
                }
            }
        )

        // Show calendar dialog when clicked
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    Button(onClick = {
                        // Format the selected date and display it
                        datePickerState.selectedDateMillis?.let { millis ->
                            val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            date = dateFormatter.format(Date(millis))
                        }
                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                }
            ) {
                DatePicker(
                    state = datePickerState,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = expenseName,
            onValueChange = { expenseName = it },
            label = { Text("Expense Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // ---- AMOUNT WITH CURRENCY DROPDOWN ----
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Currency Dropdown
            ExposedDropdownMenuBox(
                expanded = currencyDropdownExpanded,
                onExpandedChange = { currencyDropdownExpanded = it },
                modifier = Modifier.weight(0.3f)
            ) {
                OutlinedTextField(
                    value = selectedCurrency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Currency") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyDropdownExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = currencyDropdownExpanded,
                    onDismissRequest = { currencyDropdownExpanded = false }
                ) {
                    currencyOptions.forEach { currency ->
                        DropdownMenuItem(
                            text = { Text(currency) },
                            onClick = {
                                selectedCurrency = currency
                                currencyDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount") },
                placeholder = { Text("${currencySymbols[selectedCurrency]} 0.00") },
                modifier = Modifier.weight(0.7f)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        // ---- SPLIT METHOD DROPDOWN ----
        ExposedDropdownMenuBox(
            expanded = splitMethodDropdownExpanded,
            onExpandedChange = { splitMethodDropdownExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedSplitMethod,
                onValueChange = {},
                readOnly = true,
                label = { Text("Split Method") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = splitMethodDropdownExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = splitMethodDropdownExpanded,
                onDismissRequest = { splitMethodDropdownExpanded = false }
            ) {
                splitMethodOptions.forEach { method ->
                    DropdownMenuItem(
                        text = { Text(method) },
                        onClick = {
                            selectedSplitMethod = method
                            splitMethodDropdownExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Show percentage input fields only for Percentage Split
        if (selectedSplitMethod == "Percentage Split") {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = yourPercentage,
                    onValueChange = {
                        yourPercentage = it
                        // Auto-calculate other person's percentage
                        val yourPct = it.toIntOrNull() ?: 0
                        otherPersonPercentage = (100 - yourPct).toString()
                    },
                    label = { Text("You Owe %") },
                    placeholder = { Text("50") },
                    modifier = Modifier.weight(0.5f)
                )

                OutlinedTextField(
                    value = otherPersonPercentage,
                    onValueChange = {
                        otherPersonPercentage = it
                        // Auto-calculate your percentage
                        val otherPct = it.toIntOrNull() ?: 0
                        yourPercentage = (100 - otherPct).toString()
                    },
                    label = { Text("Other Person %") },
                    placeholder = { Text("50") },
                    modifier = Modifier.weight(0.5f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Show total percentage
            val totalPercentage = (yourPercentage.toIntOrNull() ?: 0) + (otherPersonPercentage.toIntOrNull() ?: 0)
            Text(
                text = "Total: $totalPercentage% ${if (totalPercentage == 100) "✓" else "⚠ Must be 100%"}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Split Method Description
        Text(
            text = when(selectedSplitMethod) {
                "You Paid - Split Equally" -> "You paid full amount, split equally with another person"
                "You Owed - Full Amount" -> "You owe the entire amount"
                "Another Person Paid - Split Equally" -> "Another person paid, you split equally"
                "Another Person Owed - Full Amount" -> "Another person owes you the full amount"
                "Percentage Split" -> "Custom percentage split (You: $yourPercentage% | Other: $otherPersonPercentage%)"
                else -> ""
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Check if percentage split total is 100
                if (selectedSplitMethod == "Percentage Split") {
                    val totalPct = (yourPercentage.toIntOrNull() ?: 0) + (otherPersonPercentage.toIntOrNull() ?: 0)
                    if (totalPct != 100) {
                        uploadStatus = "❌ Percentages must add up to 100%"
                        return@Button
                    }
                }

                // amountText.toDouble() would CRASH if this field is empty
                // or not a number — toDoubleOrNull() + this guard stops that
                val amountValue = amountText.toDoubleOrNull()
                if (name == null || expenseName.isBlank() || amountValue == null) return@Button

                // Calculate Debit/Credit based on split method
                val (debitAmount, creditAmount) = when (selectedSplitMethod) {
                    "You Paid - Split Equally" -> {
                        // You paid full amount, split equally with other person
                        // You paid 100, get back 50 from other person
                        Pair((-(amountValue / 2)).toString(), (amountValue / 2).toString())
                    }
                    "You Owed - Full Amount" -> {
                        // You owe the entire amount
                        Pair((-amountValue).toString(), "0")
                    }
                    "Another Person Paid - Split Equally" -> {
                        // Another person paid, you owe them half
                        Pair((-(amountValue / 2)).toString(), "0")
                    }
                    "Another Person Owed - Full Amount" -> {
                        // Another person owes you the full amount
                        Pair("0", amountValue.toString())
                    }
                    "Percentage Split" -> {
                        // Custom percentage split
                        val yourPct = yourPercentage.toDoubleOrNull() ?: 50.0
                        val otherPct = otherPersonPercentage.toDoubleOrNull() ?: 50.0
                        val yourAmount = (amountValue * yourPct) / 100
                        val otherAmount = (amountValue * otherPct) / 100
                        Pair((-yourAmount).toString(), otherAmount.toString())
                    }
                    else -> Pair("0", "0")
                }

                val newExpense = PersonExpense(
                    name = name!!, // safe — guarded by the check above
                    date = date,
                    expenseName = expenseName,
                    Amount = "${currencySymbols[selectedCurrency]} $amountText ($selectedCurrency)", // ← Include currency symbol
                    Debit = debitAmount,
                    credit = creditAmount
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