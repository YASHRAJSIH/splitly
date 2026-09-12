package com.example.myapplication

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.LaunchedEffect

/**
 * FILE PURPOSE: "Add Expense" form that saves to Firebase and returns home.
 * - Simple form
 * - Saves to Firebase (both global and person's history)
 * - Navigates back to HOME (not just popBackStack)
 * - Shows success/error messages
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(navController: NavHostController) {
    val personNames = samplePeople.map { it.name }
    val expenses = remember { mutableStateListOf<PersonExpense>() }

    // ---- FORM STATE ----
    var name by remember { mutableStateOf<String?>(null) }
    var nameDropdownExpanded by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf("") }
    var expenseName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    // ---- CURRENCY ----
    var selectedCurrency by remember { mutableStateOf("USD") }
    var currencyDropdownExpanded by remember { mutableStateOf(false) }
    val currencyOptions = listOf("USD", "EUR", "GBP", "INR", "JPY", "AUD", "CAD", "CHF", "CNY", "MXN")
    val currencySymbols = mapOf(
        "USD" to "$", "EUR" to "€", "GBP" to "£", "INR" to "₹",
        "JPY" to "¥", "AUD" to "A$", "CAD" to "C$", "CHF" to "CHF",
        "CNY" to "¥", "MXN" to "$"
    )

    // ---- DATE PICKER ----
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // ---- SPLIT METHOD ----
    var selectedSplitMethod by remember { mutableStateOf("You Paid - Split Equally") }
    var splitMethodDropdownExpanded by remember { mutableStateOf(false) }
    val splitMethodOptions = listOf(
        "You Paid - Split Equally",
        "You Owed - Full Amount",
        "Another Person Paid - Split Equally",
        "Another Person Owed - Full Amount",
        "Percentage Split"
    )
    var yourPercentage by remember { mutableStateOf("50") }
    var otherPersonPercentage by remember { mutableStateOf("50") }

    // ---- STATUS ----
    var uploadStatus by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Add Expense", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // ---- NAME DROPDOWN ----
        ExposedDropdownMenuBox(
            expanded = nameDropdownExpanded,
            onExpandedChange = { nameDropdownExpanded = it }
        ) {
            OutlinedTextField(
                value = name ?: "",
                onValueChange = {},
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

        // ---- DATE PICKER ----
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

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    Button(onClick = {
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

        // ---- EXPENSE NAME ----
        OutlinedTextField(
            value = expenseName,
            onValueChange = { expenseName = it },
            label = { Text("Expense Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // ---- AMOUNT WITH CURRENCY ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount") },
                placeholder = { Text("${currencySymbols[selectedCurrency]} 0.00") },
                modifier = Modifier.weight(0.7f)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        // ---- SPLIT METHOD ----
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

        // ---- PERCENTAGE SPLIT ----
        if (selectedSplitMethod == "Percentage Split") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = yourPercentage,
                    onValueChange = {
                        yourPercentage = it
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
                        val otherPct = it.toIntOrNull() ?: 0
                        yourPercentage = (100 - otherPct).toString()
                    },
                    label = { Text("Other %") },
                    placeholder = { Text("50") },
                    modifier = Modifier.weight(0.5f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            val totalPercentage = (yourPercentage.toIntOrNull() ?: 0) + (otherPersonPercentage.toIntOrNull() ?: 0)
            Text(
                text = "Total: $totalPercentage%",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (totalPercentage == 100) Color.Green else Color.Red
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // ---- SPLIT DESCRIPTION ----
        Text(
            text = when(selectedSplitMethod) {
                "You Paid - Split Equally" -> "You paid full amount, split equally"
                "You Owed - Full Amount" -> "You owe the entire amount"
                "Another Person Paid - Split Equally" -> "Another person paid, you split equally"
                "Another Person Owed - Full Amount" -> "Another person owes you the full amount"
                "Percentage Split" -> "You: $yourPercentage% | Other: $otherPersonPercentage%"
                else -> ""
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))

        // ---- ADD BUTTON ----
        Button(
            onClick = {
                // Simple validation
                if (name == null || date.isBlank() || expenseName.isBlank() || amountText.isBlank()) {
                    uploadStatus = "❌ Please fill all fields"
                    return@Button
                }

                val amountValue = amountText.toDoubleOrNull()
                if (amountValue == null || amountValue <= 0) {
                    uploadStatus = "❌ Please enter a valid amount"
                    return@Button
                }

                if (selectedSplitMethod == "Percentage Split") {
                    val totalPct = (yourPercentage.toIntOrNull() ?: 0) + (otherPersonPercentage.toIntOrNull() ?: 0)
                    if (totalPct != 100) {
                        uploadStatus = "❌ Percentages must add up to 100%"
                        return@Button
                    }
                }

                // Calculate split
                val (debitAmount, creditAmount) = when (selectedSplitMethod) {
                    "You Paid - Split Equally" -> {
                        Pair((-(amountValue / 2)).toString(), (amountValue / 2).toString())
                    }
                    "You Owed - Full Amount" -> {
                        Pair((-amountValue).toString(), "0")
                    }
                    "Another Person Paid - Split Equally" -> {
                        Pair((-(amountValue / 2)).toString(), "0")
                    }
                    "Another Person Owed - Full Amount" -> {
                        Pair("0", amountValue.toString())
                    }
                    "Percentage Split" -> {
                        val yourPct = yourPercentage.toDoubleOrNull() ?: 50.0
                        val otherPct = otherPersonPercentage.toDoubleOrNull() ?: 50.0
                        val yourAmount = (amountValue * yourPct) / 100
                        val otherAmount = (amountValue * otherPct) / 100
                        Pair((-yourAmount).toString(), otherAmount.toString())
                    }
                    else -> Pair("0", "0")
                }

                val newExpense = PersonExpense(
                    name = name!!,
                    date = date,
                    expenseName = expenseName,
                    Amount = "${currencySymbols[selectedCurrency]} $amountText ($selectedCurrency)",
                    Debit = debitAmount,
                    credit = creditAmount
                )

                expenses.add(newExpense)

                // Upload to Firebase
                uploadPersonExpenses(listOf(newExpense)) { success, message ->
                    if (success) {
                        println("✅ Expense saved! Navigating to home...")
                        uploadStatus = "✅ Expense added! Returning home..."

                        // Navigate to home after showing success message
                        navController.navigate("home") {
                            // Clear the back stack up to home
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    } else {
                        uploadStatus = "❌ Failed: $message"
                        println("❌ Upload failed: $message")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5B6EF5)
            )
        ) {
            Text("Add Expense", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        // ---- STATUS MESSAGE ----
        uploadStatus?.let { status ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                status,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (status.contains("✅")) Color.Green else Color.Red
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ---- PREVIEW ----
        if (expenses.isNotEmpty()) {
            Text("Preview (${expenses.size})", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                items(expenses) { expense ->
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Text(
                            "${expense.name} — ${expense.expenseName}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            "${expense.date} · ${expense.Amount}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}