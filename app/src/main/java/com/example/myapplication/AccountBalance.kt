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
 * FILE PURPOSE: Account balance card showing total balance, you are owed, and you owe.
 * - Shows net balance at top
 * - Shows breakdown of how much you are owed vs how much you owe
 * - No hardcoded currency (shows as numbers)
 */

private val Green = Color(0xFF2ECC71)
private val Red = Color(0xFFE74C3C)

@Composable
fun AccountBalanceCard(
    totalBalance: Double = 0.0,
    YouGet: Double = 0.0,
    Due: Double = 0.0
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Text(
            text = "NET BALANCE",
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Net balance (total)
        Text(
            text = if (totalBalance >= 0)
                "+${"%.2f".format(totalBalance)}"
            else
                "-${"%.2f".format(-totalBalance)}",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = if (totalBalance >= 0) Green else Red
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Breakdown row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // You Are Owed (CREDIT)
            BalanceStat(
                label = "Get back",
                amount = "+${"%.2f".format(YouGet)}",
                color = Green,
                alignEnd = false
            )

            // You Owe (DEBIT)
            BalanceStat(
                label = "Due - To give",
                amount = "-${"%.2f".format(Due)}",
                color = Red,
                alignEnd = true
            )
        }

        // Explanation
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Net = You Are Owed - You Owe",
            fontSize = 10.sp,
            color = Color.Gray,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
        )
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

@Preview(showBackground = true)
@Composable
private fun AccountBalanceCardPreview() {
    AccountBalanceCard(
        totalBalance = 105.0,
        YouGet = 150.0,
        Due = 45.0
    )
}