package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


private val Purple = Color(0xFF5B6EF5)
private val Gray = Color(0xFF9AA0A6)

private val chartDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
private val dayLabelFormat = SimpleDateFormat("EEE\nd", Locale.getDefault())

private fun parseExpenseDate(date: String): Date? =
    try { chartDateFormat.parse(date) } catch (e: Exception) { null }

data class DayBucket(
    val label: String,
    val total: Double,
    val currency: String,
    val isToday: Boolean
)


fun dailyTotalsThisWeek(expenses: List<PersonExpense>): List<DayBucket> {
    val monday = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        val diff = (get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        add(Calendar.DAY_OF_MONTH, -diff)
    }
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val realExpenses = expenses.filterNot { isSettlement(it) }

    return (0..6).map { dayOffset ->
        val dayStart = (monday.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, dayOffset) }
        val dayEnd = (dayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }

        val expensesThatDay = realExpenses.filter { expense ->
            val d = parseExpenseDate(expense.date)
            d != null && !d.before(dayStart.time) && d.before(dayEnd.time)
        }
        val total = expensesThatDay.sumOf { it.amount }

        val dominantCurrency = expensesThatDay
            .groupBy { it.currency }
            .mapValues { (_, rows) -> rows.sumOf { it.amount } }
            .maxByOrNull { it.value }
            ?.key
            ?: "USD"

        DayBucket(
            label = dayLabelFormat.format(dayStart.time),
            total = total.toMoney(),
            currency = dominantCurrency,
            isToday = dayStart.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    dayStart.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        )
    }
}

@Composable
fun WeeklyExpenseChartCard(expenses: List<PersonExpense>) {
    val days = remember(expenses) { dailyTotalsThisWeek(expenses) }
    val maxTotal = days.maxOfOrNull { it.total } ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Text(
            "WEEKLY EXPENSES",
            fontSize = 12.sp,
            color = Gray,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text("This week, by day", fontSize = 12.sp, color = Gray)
        Spacer(modifier = Modifier.height(20.dp))

        if (maxTotal <= 0.005) {
            Text(
                "No expenses yet",
                fontSize = 13.sp,
                color = Gray,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                days.forEach { day ->
                    val fraction = (day.total / maxTotal).toFloat().coerceIn(0.03f, 1f)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .height(120.dp)
                                .width(24.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fraction)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(if (day.isToday) Purple else Purple.copy(alpha = 0.5f))
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            day.label,
                            fontSize = 10.sp,
                            color = Gray,
                            lineHeight = 12.sp,
                            maxLines = 2,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "${symbolFor(day.currency)}${"%.0f".format(day.total)}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}