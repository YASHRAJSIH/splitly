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
 * FILE PURPOSE: Home screen with REAL Firebase data.
 * - Loads all expenses from Firebase
 * - Calculates balances per person (with their currency)
 * - Shows proper debit/credit amounts
 * - Auto-refreshes when expenses are added
 */

@Composable
fun HomeScreen(navController: NavHostController) {
    // State for expenses and people balances
    var allExpenses by remember { mutableStateOf<List<PersonExpense>>(emptyList()) }
    var peopleWithBalances by remember { mutableStateOf<List<PersonItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var totalOwedAmount by remember { mutableStateOf(0.0) }
    var totalOwesAmount by remember { mutableStateOf(0.0) }

    // Load expenses from Firebase when screen appears
    LaunchedEffect(Unit) {
        println("🏠 HomeScreen: Loading expenses from Firebase...")
        getAllExpenses { expenses ->
            println("📖 Loaded ${expenses.size} expenses from Firebase")
            allExpenses = expenses

            // Calculate balances from expenses
            val result = calculateBalancesFromExpenses(expenses)
            peopleWithBalances = result.first
            totalOwedAmount = result.second
            totalOwesAmount = result.third

            isLoading = false

            println("✅ Calculated balances:")
            println("   Total You Are Owed: $totalOwedAmount")
            println("   Total You Owe: $totalOwesAmount")
            peopleWithBalances.forEach { person ->
                println("   - ${person.name}: ${person.amount}")
            }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Show loading indicator
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            // Account Balance Card with proper calculation
            item {
                val netBalance = totalOwedAmount
                AccountBalanceCard(
                    totalBalance = netBalance,
                    YouGet = totalOwedAmount,
                    Due = totalOwesAmount
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // People List with Real Data
            item {
                if (peopleWithBalances.isEmpty()) {
                    Text(
                        "No transactions yet. Add one!",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                } else {
                    PeopleSection(
                        people = peopleWithBalances,
                        onPersonClick = { person ->
                            val index = samplePeople.indexOfFirst { it.name == person.name }
                            if (index >= 0) {
                                navController.navigate("personDetails/$index")
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

/**
 * Calculate total balances and per-person balances from all expenses
 *
 * Logic:
 * - For each expense, we have credit (they owe you) and debit (you owe them)
 * - Credit is positive (they owe you)
 * - Debit is negative (you owe them)
 *
 * Returns: Pair of (peopleList, totalYouAreOwed, totalYouOwe)
 */
fun calculateBalancesFromExpenses(expenses: List<PersonExpense>): Triple<List<PersonItem>, Double, Double> {
    val personBalances = mutableMapOf<String, Double>()
    var totalYouAreOwed = 0.0  // Sum of all CREDITS (positive amounts they owe you)
    var totalYouOwe = 0.0      // Sum of all DEBITS (negative amounts you owe them, stored as positive)

    // Go through each expense
    expenses.forEach { expense ->
        val name = expense.name
        val Accountholder = expense.accountHolder ?: 0.0
        val AnotherPerson = expense.anotherPerson ?: 0.0

        // Initialize if not exists
        if (!personBalances.containsKey(name)) {
            personBalances[name] = 0.0
        }

        // Update balance for this person
        // credit = positive (they owe you)
        // debit = negative (you owe them)
//        val netAmount = credit + debit  // debit is already negative
//        personBalances[name] = personBalances[name]!! + netAmount

        // Update TOTALS (from YOU perspective)
        // If credit > 0: They owe you (positive for "you are owed")

                totalYouOwe += Accountholder
             totalYouAreOwed += AnotherPerson

//        // If debit < 0: You owe them (convert to positive for "you owe")
//        if (debit < 0) {
//            totalYouOwe += kotlin.math.abs(debit)
//        }
    }

    // Convert to PersonItem list
    val peopleList = personBalances.map { (name, balance) ->
        PersonItem(
            name = name,
            amount = balance,
            avatarColor = getAvatarColorForName(name)
        )
    }.sortedByDescending { kotlin.math.abs(it.amount) }  // Sort by amount

    println("📊 Balance Breakdown:")
    println("   Total You Are Owed: $totalYouAreOwed")
    println("   Total You Owe: $totalYouOwe")

    return Triple(peopleList, totalYouAreOwed, totalYouOwe)
}

/**
 * Get consistent avatar color for a person's name
 */
fun getAvatarColorForName(name: String): Color {
    val colors = listOf(
        Color(0xFFE8B98C),  // Warm orange
        Color(0xFFD98C8C),  // Warm red
        Color(0xFFB0B0B0),  // Gray
        Color(0xFF8CBDE8),  // Blue
        Color(0xFFC78CEB),  // Purple
    )
    return colors[name.hashCode() % colors.size]
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