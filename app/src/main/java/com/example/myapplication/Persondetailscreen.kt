package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable

/**
 * FILE PURPOSE: "Person Details" screen — UI only, no Firebase, no navigation
 * wiring. Just what was asked for: back button, name, net balance, the full
 * transaction list (no filter tabs), and the Add Expense button.
 */

private val Purple = Color(0xFF5B6EF5)
private val Green = Color(0xFF2ECC71)
private val Red = Color(0xFFE67E22)
private val Gray = Color(0xFF9AA0A6)
private val BgGray = Color(0xFFF5F6FA)

// ---------- DATA MODELS ----------
data class PersonDetails(
    val name: String,
    val amount: Double,
)


//{
//
//    val netBalance: Double get() = theyOweYou - youOweThem
//}

data class Transaction(
    val icon: String,              // emoji shown in the colored circle
    val iconBackground: Color,
    val title: String,
    val date: String,
    val paidBy: String,            // "Alex" or "You"
    val amount: Double,
    val note: String,              // small gray line under the date
    val statusLabel: String,       // "You owe €30.00" / "Settled" / etc
    val statusColor: Color,
    val statusIsPill: Boolean      // true = rounded pill background, false = plain text
)

//// ---------- SAMPLE DATA ----------
//private val samplePerson = PersonDetails(
//    name = "Alex",
//    theyOweYou = 60.50,
//    theyOweExpenseCount = 2,
//    youOweThem = 18.00,
//    youOweExpenseCount = 1,
//    since = "Mar 2026",
//    lastSettledDate = "Aug 28, 2026",
//    lastSettledAmount = 85.00
//)

private val sampleTransactions = listOf(
    Transaction(
        icon = "🍽️",
        iconBackground = Color(0xFFFDEBD9),
        title = "Dinner at Osteria",
        date = "12 Sep 2026 · Paid by Alex",
        paidBy = "Alex",
        amount = 60.00,
        note = "50/50 Split (Your cut: €30.00)",
        statusLabel = "You owe €30.00",
        statusColor = Red,
        statusIsPill = false
    ),
    Transaction(
        icon = "🚕",
        iconBackground = Color(0xFFD9F2EC),
        title = "Airport Taxi",
        date = "10 Sep 2026 · Paid by You",
        paidBy = "You",
        amount = 24.00,
        note = "Alex's share: €12.00",
        statusLabel = "Unsettled",
        statusColor = Gray,
        statusIsPill = true
    ),
    Transaction(
        icon = "🛒",
        iconBackground = Color(0xFFFDF3D9),
        title = "Weekend Groceries",
        date = "07 Sep 2026 · Paid by Alex",
        paidBy = "Alex",
        amount = 45.50,
        note = "Split 2 ways (€22.75 each)",
        statusLabel = "You owe €22.75",
        statusColor = Red,
        statusIsPill = false
    ),
    Transaction(
        icon = "✅",
        iconBackground = Color(0xFFD9F9E5),
        title = "Settle Up Transfer",
        date = "28 Aug 2026 · Paid by Alex",
        paidBy = "Alex",
        amount = 85.00,
        note = "Via SEPA Instant Transfer",
        statusLabel = "Settled",
        statusColor = Green,
        statusIsPill = true
    ),
    Transaction(
        icon = "🎫",
        iconBackground = Color(0xFFE3E0FB),
        title = "Museum Tickets",
        date = "24 Aug 2026 · Paid by You",
        paidBy = "You",
        amount = 32.00,
        note = "Resolved in August balance",
        statusLabel = "Settled",
        statusColor = Gray,
        statusIsPill = true
    )
)

// ---------- SCREEN ----------
@Composable
fun PersonDetailsScreen(
    person: PersonDetails = PersonDetails(
        name = samplePeople.first().name,
        amount = samplePeople.first().amount
    ),
    transactions: List<Transaction> = sampleTransactions,
    onAddExpenseClick: () -> Unit = {}   // ← new
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGray)
            .verticalScroll(rememberScrollState())
    ) {
        HeaderRow(person)
        //NetBalanceCard(person)
        Spacer(modifier = Modifier.height(12.dp))
        TransactionHistorySection(transactions)
        Spacer(modifier = Modifier.height(16.dp))
        AddExpenseButton(person.name, onClick = onAddExpenseClick)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ---------- BACK BUTTON + NAME ----------
@Composable
private fun HeaderRow(person: PersonDetails) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Purple, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(person.name, color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(person.name, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

//// ---------- NET BALANCE CARD ----------
//@Composable
//private fun NetBalanceCard(person: PersonDetails) {
//    val isInYourFavor = person.netBalance >= 0
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp)
//            .background(Color.White, RoundedCornerShape(16.dp))
//            .padding(20.dp)
//    ) {
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Box(
//                modifier = Modifier
//                    .background(Color(0xFFE3F9EC), RoundedCornerShape(20.dp))
//                    .padding(horizontal = 10.dp, vertical = 4.dp)
//            )
//        }
//
//        Spacer(modifier = Modifier.height(14.dp))
//
//        Text("TOTAL NET BALANCE", fontSize = 11.sp, color = Gray, fontWeight = FontWeight.Medium)
//
//        Text(
//            text = "${if (isInYourFavor) "+" else "-"}€${"%.2f".format(kotlin.math.abs(person.netBalance))}",
//            fontSize = 30.sp,
//            fontWeight = FontWeight.Bold,
//            color = if (isInYourFavor) Green else Red
//        )
//
//        Text(
//            text = if (isInYourFavor) "in your favor" else "you're behind",
//            fontSize = 12.sp,
//            color = Gray
//        )
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .background(Purple, RoundedCornerShape(14.dp))
//                .padding(vertical = 14.dp),
//            horizontalArrangement = Arrangement.Center
//        ) {
//            Text(
//                "Settle Up €${"%.2f".format(kotlin.math.abs(person.netBalance))}",
//                color = Color.White,
//                fontWeight = FontWeight.SemiBold,
//                fontSize = 15.sp
//            )
//        }
//
//        //Spacer(modifier = Modifier.height(10.dp))
//
////        Text(
////            "Last settled: ${person.lastSettledDate} (€${"%.2f".format(person.lastSettledAmount)})",
////            fontSize = 11.sp,
////            color = Gray,
////            modifier = Modifier.fillMaxWidth(),
////            textAlign = androidx.compose.ui.text.style.TextAlign.Center
////        )
//    }
//}
//
//  ---------- TRANSACTION HISTORY (all items, no filter tabs) ----------
@Composable
private fun TransactionHistorySection(transactions: List<Transaction>) {
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Transaction History", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(6.dp))
        }

        transactions.forEach { transaction -> TransactionRow(transaction) }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(Color.White, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(transaction.iconBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(transaction.icon, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(transaction.date, fontSize = 11.sp, color = Gray)
            Text(transaction.note, fontSize = 11.sp, color = Gray)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                "€${"%.2f".format(transaction.amount)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (transaction.statusIsPill) {
                Box(
                    modifier = Modifier
                        .background(
                            transaction.statusColor.copy(alpha = 0.15f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(transaction.statusLabel, fontSize = 10.sp, color = transaction.statusColor)
                }
            } else {
                Text(
                    transaction.statusLabel,
                    fontSize = 11.sp,
                    color = transaction.statusColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ---------- ADD EXPENSE BUTTON ----------
@Composable
private fun AddExpenseButton(personFirstName: String,onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() }
            .background(Purple, RoundedCornerShape(14.dp))
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "Add Expense with ${personFirstName.substringBefore(" ")}",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun PersonDetailsScreenPreview() {
    PersonDetailsScreen()
}