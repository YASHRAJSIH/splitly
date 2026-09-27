package com.example.myapplication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private fun money(v: Double): String = String.format(Locale.US, "%.2f", v)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseScreen(
    personName: String,
    expenseKey: String,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    var original by remember { mutableStateOf<PersonExpense?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var date by remember { mutableStateOf("") }
    var expenseName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("USD") }
    var selectedSplitMethod by remember { mutableStateOf("You Paid - Split Equally") }
    var AccountHolder by remember { mutableStateOf("") }
    var OtherPerson by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    var currencyDropdownExpanded by remember { mutableStateOf(false) }
    var splitMethodDropdownExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val currencyOptions = currencySymbols.keys.toList()
    val splitMethodOptions = listOf(
        "You Paid - Split Equally",
        "You Owed - Full Amount",
        "Another Person Paid - Split Equally",
        "Another Person Owed - Full Amount",
        "LumSum"
    )

    val amountValue = amountText.trim().toDoubleOrNull()
    DisposableEffect(personName) {
        val cancel = getPersonTransactions(personName) { transactions ->
            val found = transactions.firstOrNull { it.key == expenseKey }
            if (found != null && original == null) {
                original = found
                date = found.date
                expenseName = found.expenseName
                amountText = money(found.amount)
                selectedCurrency = found.currency
                selectedSplitMethod = found.splitMethod.ifBlank { "You Paid - Split Equally" }
                if (found.splitMethod == "LumSum") {
                    AccountHolder = money(-found.accountHolder)
                    OtherPerson = money(found.anotherPerson)
                }
            }
            isLoading = false
        }
        onDispose { cancel() }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val current = original
    if (current == null) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) { Text("This expense no longer exists.") }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Edit Expense", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("For $personName", fontSize = 13.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        // ---- DATE ----
        OutlinedTextField(
            value = date,
            onValueChange = {},
            label = { Text("Date") },
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
                            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            date = formatter.format(Date(millis))
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                }
            ) {
                DatePicker(state = datePickerState, modifier = Modifier.padding(16.dp))
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

        // ---- AMOUNT + CURRENCY ----
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
                    val total = input.trim().toDoubleOrNull()
                    val mine = AccountHolder.toDoubleOrNull()
                    if (total != null && mine != null) {
                        OtherPerson = money(total - mine)
                    }
                },
                label = { Text("Amount") },
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

        // ---- LUMSUM SPLIT ----
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
                    modifier = Modifier.weight(0.5f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            val enteredTotal = (AccountHolder.toDoubleOrNull() ?: 0.0) +
                    (OtherPerson.toDoubleOrNull() ?: 0.0)
            val splitIsValid = amountValue != null &&
                    abs(enteredTotal - amountValue) < 0.005

            Text(
                text = "Total: ${symbolFor(selectedCurrency)} ${money(enteredTotal)}" +
                        " of ${symbolFor(selectedCurrency)} ${money(amountValue ?: 0.0)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (splitIsValid) Color(0xFF2ECC71) else Color(0xFFE74C3C)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ---- SAVE / CANCEL ----
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                enabled = !isSaving,
                onClick = {
                    if (expenseName.isBlank() || date.isBlank() || amountValue == null || amountValue <= 0) {
                        statusMessage = "❌ Please fill all fields with a valid amount"
                        return@Button
                    }

                    if (selectedSplitMethod == "LumSum") {
                        val mine = AccountHolder.toDoubleOrNull()
                        val theirs = OtherPerson.toDoubleOrNull()
                        if (mine == null || theirs == null || mine < 0 || theirs < 0) {
                            statusMessage = "❌ Enter a valid amount in both split boxes"
                            return@Button
                        }
                        if (abs((mine + theirs) - amountValue) >= 0.005) {
                            statusMessage = "❌ The two amounts must add up to ${money(amountValue)}"
                            return@Button
                        }
                    }

                    val (holderAmount, otherAmount) = computeSplitAmounts(
                        splitMethod = selectedSplitMethod,
                        amountValue = amountValue,
                        lumSumAccountHolder = AccountHolder.toDoubleOrNull(),
                        lumSumOtherPerson = OtherPerson.toDoubleOrNull()
                    )
                    val updated = current.copy(
                        date = date,
                        expenseName = expenseName,
                        amount = amountValue.toMoney(),
                        currency = selectedCurrency,
                        splitMethod = selectedSplitMethod,
                        accountHolder = holderAmount.toMoney(),
                        anotherPerson = otherAmount.toMoney()
                    )

                    isSaving = true
                    updatePersonExpense(updated) { success, message ->
                        isSaving = false
                        if (success) onSaved() else statusMessage = "❌ Failed: $message"
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isSaving) "Saving..." else "Save Changes")
            }

            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
        }

        statusMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = Color(0xFFE74C3C), fontSize = 13.sp)
        }
    }
}