package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
 * FILE PURPOSE: only the "Monthly Expenses" card — the bar chart plus the
 * three month rows underneath it. Nothing else from that Accounts screen.
 *
 * Note: package matches your real project package (com.example.myapplication),
 * same fix as MainActivity — check yours matches before dropping this in.
 */

private val Purple = Color(0xFF5B6EF5)
private val PurpleLight = Color(0xFFD8DCFB)
private val Green = Color(0xFF2ECC71)
private val Red = Color(0xFFE74C3C)

// monthShort = x-axis label under each bar ("Sep")
// monthFull  = label used in the list rows ("September")
// changePercent = null for months not shown in the list below the chart
data class MonthExpense(
    val monthShort: String,
    val monthFull: String,
    val amount: Int,
    val changePercent: Int? = null
)

private val sampleMonths = listOf(
    MonthExpense("Apr", "April", 340),
    MonthExpense("May", "May", 380),
    MonthExpense("Jun", "June", 473),
    MonthExpense("Jul", "July", 530, changePercent = 12),
    MonthExpense("Aug", "August", 480, changePercent = -9),
    MonthExpense("Sep", "September", 610, changePercent = 27)
)

@Composable
fun MonthlyExpensesCard(
    months: List<MonthExpense> = sampleMonths,
    averagePerMonth: Int = 440
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        // ---- header row: title + "Last 6 Months" pill ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Monthly Expenses", fontSize = 16.sp, fontWeight = FontWeight.Bold)

            Box(
                modifier = Modifier
                    .background(Color(0xFFF2F2F7), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Last 6 Months", fontSize = 12.sp, color = Color.Gray)
            }
        }

        Text(
            "Average: $${averagePerMonth}/mo",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
        )

        // ---- the bars ----
        BarChart(months)

        Spacer(modifier = Modifier.height(16.dp))

        // ---- month rows below the chart (most recent first, like the screenshot) ----
        months.filter { it.changePercent != null }
            .reversed()
            .forEach { month -> MonthRow(month) }
    }
}

@Composable
private fun BarChart(months: List<MonthExpense>) {
    val maxAmount = months.maxOf { it.amount }
    val maxBarHeight = 110.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        months.forEachIndexed { index, month ->
            val isLast = index == months.lastIndex
            // bar height is just a straight ratio against the biggest value
            val barHeight = maxBarHeight * (month.amount.toFloat() / maxAmount)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // callout label only above the highlighted (latest) bar
                if (isLast) {
                    Text(
                        "$${month.amount}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Purple,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                } else {
                    // empty spacer so every bar still lines up at the same baseline
                    Spacer(modifier = Modifier.height(19.dp))
                }

                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(barHeight)
                        .background(
                            if (isLast) Purple else PurpleLight,
                            RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                        )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(month.monthShort, fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun MonthRow(month: MonthExpense) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(Purple, CircleShape)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(month.monthFull, fontSize = 14.sp, modifier = Modifier.weight(1f))

        Text("$${month.amount}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

        Spacer(modifier = Modifier.width(8.dp))

        val change = month.changePercent ?: 0
        val isPositive = change >= 0
        Box(
            modifier = Modifier
                .background(
                    if (isPositive) Color(0xFFE3F9EC) else Color(0xFFFCE8E8),
                    RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                "${if (isPositive) "+" else ""}$change%",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isPositive) Green else Red
            )
        }
    }
}

// preview only — not part of the real screen
@Preview(showBackground = true)
@Composable
private fun MonthlyExpensesCardPreview() {
    MonthlyExpensesCard()
}
