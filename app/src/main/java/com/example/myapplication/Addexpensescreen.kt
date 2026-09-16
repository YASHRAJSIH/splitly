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
import kotlin.math.abs

/**
 * FILE PURPOSE: "Add Expense" form that saves to Firebase and returns home.
 *
 * WHAT CHANGED IN THE MONEY PATH
 * The amount is now parsed once into a Double and stored as a number, with the
 * currency code in its own field. The symbol is only ever used for display.
 * Storing "€ 42.50 (EUR)" made the amount unusable for arithmetic — you could not
 * sum a balance without re-parsing a string you had formatted yourself.
 *
 * The split produces a PAIR of numbers, one per side, set explicitly by each
 * split method. See the WARNING on the split block in the button for what that
 * costs you.
 *
 * LUMSUM (replaces the old Percentage Split)
 * Two free-form money boxes that must add up to the total. Editing either one
 * auto-fills the other, and editing the total re-derives the second box from the
 * first. Like the old percentage branch, it ASSUMES THE ACCOUNT HOLDER PAID the
 * bill — so the number in "They Owe" is what the other person owes you. If they
 * paid instead, the sign is wrong; that needs a separate option to fix properly.
 */

/** Formats a Double as a 2-decimal money string. Locale.US so the decimal
 *  separator is always a dot — see the note at the bottom of this file. */
private fun money(v: Double): String = String.format(Locale.US, "%.2f", v)

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

    // Parsed ONCE here. Every consumer below (the LumSum boxes, the validity
    // indicator, the button) reads this instead of parsing the string again.
    val amountValue = amountText.trim().toDoubleOrNull()

    // ---- CURRENCY ----
    var selectedCurrency by remember { mutableStateOf("USD") }
    var currencyDropdownExpanded by remember { mutableStateOf(false) }
    val currencyOptions = currencySymbols.keys.toList()

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
        "LumSum"
    )

    // Blank, not "50". A leftover default of 50 was a percentage meaning "half";
    // as a lump sum it would mean "50 of whatever currency" and open the form in
    // an invalid state for any total that isn't 100.
    var AccountHolder by remember { mutableStateOf("") }
    var OtherPerson by remember { mutableStateOf("") }

    // ---- STATUS ----
    var uploadStatus by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

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
                onValueChange = { input ->
                    amountText = input
                    // Changing the total has to re-derive the second LumSum box,
                    // otherwise the two boxes silently go stale against the total
                    // and the user sees a red indicator with no idea which field
                    // is wrong.
                    val total = input.trim().toDoubleOrNull()
                    val mine = AccountHolder.toDoubleOrNull()
                    if (total != null && mine != null) {
                        OtherPerson = money(total - mine)
                    }
                },
                label = { Text("Amount") },
                placeholder = { Text("${symbolFor(selectedCurrency)} 0.00") },
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

        if (selectedSplitMethod == "LumSum") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = AccountHolder,
                    onValueChange = { input ->
                        AccountHolder = input
                        val mine = input.toDoubleOrNull()
                        if (amountValue != null && mine != null) {
                            OtherPerson = money(amountValue - mine)
                        }
                    },
                    label = { Text("You Owe") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier.weight(0.5f)
                )

                OutlinedTextField(
                    value = OtherPerson,
                    onValueChange = { input ->
                        OtherPerson = input
                        val theirs = input.toDoubleOrNull()
                        if (amountValue != null && theirs != null) {
                            AccountHolder = money(amountValue - theirs)
                        }
                    },
                    label = { Text("They Owe") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier.weight(0.5f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            val enteredTotal = (AccountHolder.toDoubleOrNull() ?: 0.0) +
                    (OtherPerson.toDoubleOrNull() ?: 0.0)

            // Tolerance, not ==. Doubles built from 33.33 + 16.67 will not land
            // exactly on 50.0, and an exact comparison would reject a valid split.
            val splitIsValid = amountValue != null &&
                    abs(enteredTotal - amountValue) < 0.005

            Text(
                text = "Total: ${symbolFor(selectedCurrency)} ${money(enteredTotal)}" +
                        " of ${symbolFor(selectedCurrency)} ${money(amountValue ?: 0.0)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (splitIsValid) Color.Green else Color.Red
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // ---- SPLIT DESCRIPTION ----
        Text(
            text = when (selectedSplitMethod) {
                "You Paid - Split Equally" -> "You paid the full amount, split equally"
                "You Owed - Full Amount" -> "You owe the entire amount"
                "Another Person Paid - Split Equally" -> "They paid, you split equally"
                "Another Person Owed - Full Amount" -> "They owe you the full amount"
                "LumSum" -> "You paid — you cover ${symbolFor(selectedCurrency)} " +
                        "${AccountHolder.ifBlank { "0" }}, they owe " +
                        "${symbolFor(selectedCurrency)} ${OtherPerson.ifBlank { "0" }}"
                else -> ""
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))

        // ---- ADD BUTTON ----
        Button(
            enabled = !isSaving,
            onClick = {
                if (name == null || date.isBlank() || expenseName.isBlank() || amountText.isBlank()) {
                    uploadStatus = "❌ Please fill all fields"
                    return@Button
                }

                // amountValue is the hoisted parse from the top of the composable.
                if (amountValue == null || amountValue <= 0) {
                    uploadStatus = "❌ Please enter a valid amount"
                    return@Button
                }

                if (selectedSplitMethod == "LumSum") {
                    val mine = AccountHolder.toDoubleOrNull()
                    val theirs = OtherPerson.toDoubleOrNull()
                    if (mine == null || theirs == null || mine < 0 || theirs < 0) {
                        uploadStatus = "❌ Enter a valid amount in both split boxes"
                        return@Button
                    }
                    if (abs((mine + theirs) - amountValue) >= 0.005) {
                        uploadStatus = "❌ The two amounts must add up to ${money(amountValue)}"
                        return@Button
                    }
                }
                val (holderAmount, otherAmount) = when (selectedSplitMethod) {
                    "You Paid - Split Equally" ->
                        -(amountValue / 2) to (amountValue / 2)

                    "You Owed - Full Amount" ->
                        -amountValue to 0.0

                    "Another Person Paid - Split Equally" ->
                        (amountValue / 2) to -(amountValue / 2)

                    "Another Person Owed - Full Amount" ->
                        0.0 to -amountValue

                    "LumSum" ->
                        // Account holder's own share is always stored negative;
                        // the other person's stays positive. Already validated
                        // above to sum to the total.
                        -(AccountHolder.toDoubleOrNull() ?: 0.0) to
                                (OtherPerson.toDoubleOrNull() ?: 0.0)

                    else -> 0.0 to 0.0
                }

                val newExpense = PersonExpense(
                    name = name!!,
                    date = date,
                    expenseName = expenseName,
                    amount = amountValue.toMoney(),
                    currency = selectedCurrency,
                    accountHolder = holderAmount.toMoney(),
                    anotherPerson = otherAmount.toMoney()
                )

                isSaving = true
                uploadPersonExpenses(listOf(newExpense)) { success, message ->
                    isSaving = false
                    if (success) {
                        // Only add to the local preview after the write actually
                        // landed, so the UI never shows an expense that failed.
                        expenses.add(newExpense)
                        uploadStatus = "✅ Expense added! Returning home..."
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    } else {
                        uploadStatus = "❌ Failed: $message"
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
            Text(
                if (isSaving) "Saving..." else "Add Expense",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
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
                            "${expense.date} · ${expense.displayAmount()}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}