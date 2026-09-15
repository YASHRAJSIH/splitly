package com.example.myapplication

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

/**
 * FILE PURPOSE: Account balance card showing total balance, you are owed, and you owe.
 * - Shows net balance at top
 * - Shows breakdown of how much you are owed vs how much you owe
 * - No hardcoded currency (shows as numbers)
 */

private val Green = Color(0xFF2ECC71)
private val Red = Color(0xFFE74C3C)
@Composable
fun HomeScreen(navController: NavHostController) {
    var allExpenses by remember { mutableStateOf<List<PersonExpense>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        getAllExpenses { expenses ->
            allExpenses = expenses
            isLoading = false
        }
    }

    // Everything below recomputes automatically when allExpenses changes.
    val summary = remember(allExpenses) { balanceSummary(allExpenses) }
    val people = remember(allExpenses) {
        totalsPerPersonSorted(allExpenses).map { (name, amount) ->
            PersonItem(
                name = name,
                amount = amount,
                avatarColor = getAvatarColorForName(name)
            )
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
        } else {
            item {
                AccountBalanceCard(
                    totalBalance = summary.net,
                    YouGet = summary.getBack,
                    Due = summary.due
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                if (people.isEmpty()) {
                    Text(
                        "No transactions yet. Add one!",
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                } else {
                    PeopleSection(
                        people = people,
                        onPersonClick = { person ->
                            val index = people.indexOfFirst { it.name == person.name }
                            if (index >= 0) navController.navigate("personDetails/$index")
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

//@Composable
//private fun BalanceStat(
//    label: String,
//    amount: String,
//    color: Color,
//    alignEnd: Boolean
//) {
//    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
//        Text(label, fontSize = 12.sp, color = Color.Gray)
//        Text(amount, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = color)
//    }
//}

/**
 * Calculate total balances and per-person balances from all expenses
 *
 * Logic:
 * - For each expense, we have credit (they owe you) and debit (you owe them)
 * - Credit is positive (they owe you)
 * - Debit is negative (you owe them)
 *
 * Returns: Pair of (peopleList, totalYouAreOwed, totalYouOwe)
// */
//fun calculateBalancesFromExpenses(expenses: List<PersonExpense>): Triple<List<PersonItem>, Double, Double> {
//    val personBalances = mutableMapOf<String, Double>()
//    var totalYouAreOwed = 0.0  // Sum of all CREDITS (positive amounts they owe you)
//    var totalYouOwe = 0.0      // Sum of all DEBITS (negative amounts you owe them, stored as positive)
//
//    // Go through each expense
//    expenses.forEach { expense ->
//        val name = expense.name
//        val Accountholder = expense.accountHolder ?: 0.0
//        val AnotherPerson = expense.anotherPerson ?: 0.0
//
//        // Initialize if not exists
//        if (!personBalances.containsKey(name)) {
//            personBalances[name] = 0.0
//        }
//
//        // Update balance for this person
//        // credit = positive (they owe you)
//        // debit = negative (you owe them)
////        val netAmount = credit + debit  // debit is already negative
////        personBalances[name] = personBalances[name]!! + netAmount
//
//        // Update TOTALS (from YOU perspective)
//        // If credit > 0: They owe you (positive for "you are owed")
//
//                totalYouOwe += Accountholder
//             totalYouAreOwed += AnotherPerson
//
////        // If debit < 0: You owe them (convert to positive for "you owe")
////        if (debit < 0) {
////            totalYouOwe += kotlin.math.abs(debit)
////        }
//    }
//
//    // Convert to PersonItem list
//    val peopleList = personBalances.map { (name, balance) ->
//        PersonItem(
//            name = name,
//            amount = balance,
//            avatarColor = getAvatarColorForName(name)
//        )
//    }.sortedByDescending { kotlin.math.abs(it.amount) }  // Sort by amount
//
//    println("📊 Balance Breakdown:")
//    println("   Total You Are Owed: $totalYouAreOwed")
//    println("   Total You Owe: $totalYouOwe")
//
//    return Triple(peopleList, totalYouAreOwed, totalYouOwe)
//}

/**
 * Get consistent avatar color for a person's name
 */
fun getAvatarColorForName(name: String): Color {
    val colors = listOf(
        Color(0xFFE8B98C), Color(0xFFD98C8C), Color(0xFFB0B0B0),
        Color(0xFF8CBDE8), Color(0xFFC78CEB)
    )
    return colors[Math.floorMod(name.hashCode(), colors.size)]
}

/**
 * Data class for returning 3 values
 */
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)