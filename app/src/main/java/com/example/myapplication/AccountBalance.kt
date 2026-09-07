package com.example.myapplication
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * FILE PURPOSE: everything about money / debts / profits lives here.
 * (Total balance, "you are owed", "you owe" — the white card at the top of Home.)
 *
 * Nothing in this file knows about navigation or the rest of the screen.
 * It just takes numbers in and draws them.
 */

private val Green = Color(0xFF2ECC71)
private val Red = Color(0xFFE74C3C)

@Composable
fun AccountBalanceCard(
    totalBalance: Double = 105.00,
    youAreOwed: Double = 150.00,
    youOwe: Double = 45.00
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Text(
            text = "TOTAL BALANCE",
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(4.dp))

        // sign handled here so the card works whether the user is net positive or negative
        Text(
            text = if (totalBalance >= 0)
                "+$${"%.2f".format(totalBalance)}"
            else
                "-$${"%.2f".format(-totalBalance)}",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = if (totalBalance >= 0) Green else Red
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BalanceStat(
                label = "You are owed",
                amount = "+$${"%.2f".format(youAreOwed)}",
                color = Green,
                alignEnd = false
            )
            BalanceStat(
                label = "You owe",
                amount = "-$${"%.2f".format(youOwe)}",
                color = Red,
                alignEnd = true
            )
        }
    }
}

@Composable
private fun BalanceStat(
    label: String,
    amount: String,
    color: Color,
    alignEnd: Boolean
) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(amount, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

// Lets you see this card in Android Studio's Split/Design view without running the app.
@Preview(showBackground = true)
@Composable
private fun AccountBalanceCardPreview() {
    AccountBalanceCard()
}
