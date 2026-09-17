package com.example.myapplication

import android.net.Uri
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
                            // Navigate by name, not by this list's position. This list is
                            // re-sorted by balance on every recomposition, so an index
                            // captured at click time can already be stale by the time the
                            // destination reads it back. Uri.encode because a name can
                            // contain a space or another character the route parser
                            // wouldn't otherwise handle.
                            navController.navigate("personDetails/${Uri.encode(person.name)}")
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

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