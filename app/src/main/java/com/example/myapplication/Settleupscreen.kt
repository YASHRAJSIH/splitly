package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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


private val Purple = Color(0xFF5B6EF5)
private val Green = Color(0xFF2ECC71)
private val Red = Color(0xFFE67E22)
private val Gray = Color(0xFF9AA0A6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleUpScreen(
    personName: String,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    var allTransactions by remember { mutableStateOf<List<PersonExpense>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(personName) {
        val cancel = getPersonTransactions(personName) { transactions ->
            allTransactions = transactions
            isLoading = false
        }
        onDispose { cancel() }
    }

    val balances = remember(allTransactions) {
        allTransactions
            .groupBy { it.currency }
            .mapValues { (_, rows) -> rows.sumOf { it.anotherPerson }.toMoney() }
            .filterValues { kotlin.math.abs(it) > 0.005 }
    }

    val primary = remember(balances) { balances.maxByOrNull { kotlin.math.abs(it.value) } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Settle up", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            primary == null -> {
                Text(
                    "You're already settled up with $personName",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }

            else -> {
                val (currency, balance) = primary

                var youPaid by remember(balance) { mutableStateOf(balance < 0) }
                var amountText by remember(balance) {
                    mutableStateOf("%.2f".format(kotlin.math.abs(balance)))
                }
                var date by remember {
                    mutableStateOf(SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date()))
                }
                var showDatePicker by remember { mutableStateOf(false) }
                val datePickerState = rememberDatePickerState()


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarCircle(label = "You", color = Purple)

                    IconButton(onClick = { youPaid = !youPaid }) {
                        Text("⇄", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Gray)
                    }

                    AvatarCircle(label = personName, color = Green)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (youPaid) "You paid $personName" else "$personName paid you",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(28.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    leadingIcon = { Text(symbolFor(currency), fontSize = 16.sp, color = Gray) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))


                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick date")
                        }
                    }
                )
                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            Button(onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                        .format(Date(millis))
                                }
                                showDatePicker = false
                            }) { Text("OK") }
                        }
                    ) {
                        DatePicker(state = datePickerState, modifier = Modifier.padding(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    enabled = !isSaving,
                    onClick = {
                        val amountValue = amountText.trim().toDoubleOrNull()
                        if (amountValue == null || amountValue <= 0) {
                            statusMessage = "❌ Enter a valid amount"
                            return@Button
                        }

                        val splitMethod = if (youPaid) SETTLEMENT_YOU_PAID else SETTLEMENT_THEY_PAID
                        val (holderAmount, otherAmount) = computeSplitAmounts(splitMethod, amountValue)

                        val settlement = PersonExpense(
                            name = personName,
                            date = date,
                            expenseName = "Settle Up",
                            amount = amountValue.toMoney(),
                            currency = currency,
                            splitMethod = splitMethod,
                            accountHolder = holderAmount.toMoney(),
                            anotherPerson = otherAmount.toMoney()
                        )

                        isSaving = true
                        uploadPersonExpenses(listOf(settlement)) { success, message ->
                            isSaving = false
                            if (success) onDone() else statusMessage = "❌ Failed: $message"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isSaving) "Saving..." else "Save Payment")
                }

                statusMessage?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(it, color = Red, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun AvatarCircle(label: String, color: Color) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .padding(4.dp)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label.firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
    }
}