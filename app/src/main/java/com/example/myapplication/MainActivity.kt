package com.example.myapplication
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType

/**
 * FILE PURPOSE: Main navigation and app structure.
 * - Scaffold with top bar, bottom bar, FAB
 * - Navigation between Home, Accounts, Person Details, Add Expense
 * - Pass navController to HomeScreen so it can refresh data
 */

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SplitlyApp()
            }
        }
    }
}

@Composable
fun SplitlyApp() {
    val navController = rememberNavController()

    // The "Add expense" FAB only makes sense on the screens that list
    // expenses in general (home/accounts). On a person's own screen — where
    // "Settle Up" is already the primary action, and especially on the
    // "all settled up" state — it has nothing useful to do there, so it's
    // hidden rather than floating over content on every screen regardless
    // of context.
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showAddExpenseFab = currentRoute == "home" || currentRoute == "accounts"

    Scaffold(
        topBar = { SplitlyTopBar() },
        bottomBar = { SplitlyBottomBar(navController) },
        floatingActionButton = {
            if (showAddExpenseFab) {
                FloatingActionButton(
                    onClick = { navController.navigate("addExpense") },
                    containerColor = Color(0xFF5B6EF5)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add expense", tint = Color.White)
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            // Home screen - loads real Firebase data
            composable("home") {
                HomeScreen(navController)
            }

            // Accounts screen
            composable("accounts") {
                AccountsScreen()
            }

            // Person Details - shows their transactions.
            // Routed by NAME, not by position in a list. PersonDetailsScreen only
            // ever needs the name (it fetches its own live Firebase transactions
            // by name, and the balance shown there is computed from those
            // transactions, not from any amount passed in here) — so there is no
            // second list to keep in sync, and no index that can point at the
            // wrong person when the underlying data changes.
            composable(
                route = "personDetails/{personName}",
                arguments = listOf(navArgument("personName") { type = NavType.StringType })
            ) { backStackEntry ->
                val personName = backStackEntry.arguments?.getString("personName")
                    ?.let { Uri.decode(it) } ?: ""

                PersonDetailsScreen(
                    person = PersonDetails(name = personName, amount = 0.0),
                    onAddExpenseClick = { navController.navigate("addExpense") },
                    onTransactionClick = { expense ->
                        navController.navigate(
                            "expenseDetails/${Uri.encode(personName)}/${Uri.encode(expense.key)}"
                        )
                    },
                    onSettleUpClick = {
                        navController.navigate("settleUp/${Uri.encode(personName)}")
                    }
                )
            }

            // Settle Up - dedicated flow, reached from the button next to a
            // person's balance. Not routed through Add Expense.
            composable(
                route = "settleUp/{personName}",
                arguments = listOf(navArgument("personName") { type = NavType.StringType })
            ) { backStackEntry ->
                val personName = backStackEntry.arguments?.getString("personName")
                    ?.let { Uri.decode(it) } ?: ""

                SettleUpScreen(
                    personName = personName,
                    onDone = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }

            // Expense Details - single transaction, opened by tapping a row.
            // Carries personName + the Firebase key rather than the whole
            // PersonExpense (Compose Nav routes only carry primitives) and
            // re-fetches to find it, same pattern personDetails already uses.
            composable(
                route = "expenseDetails/{personName}/{expenseKey}",
                arguments = listOf(
                    navArgument("personName") { type = NavType.StringType },
                    navArgument("expenseKey") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val personName = backStackEntry.arguments?.getString("personName")
                    ?.let { Uri.decode(it) } ?: ""
                val expenseKey = backStackEntry.arguments?.getString("expenseKey")
                    ?.let { Uri.decode(it) } ?: ""

                ExpenseDetailScreen(
                    personName = personName,
                    expenseKey = expenseKey,
                    onBack = { navController.popBackStack() },
                    onEdit = {
                        navController.navigate(
                            "editExpense/${Uri.encode(personName)}/${Uri.encode(expenseKey)}"
                        )
                    },
                    onDeleted = { navController.popBackStack() }
                )
            }

            // Edit Expense - same key, same person, overwritten in place.
            composable(
                route = "editExpense/{personName}/{expenseKey}",
                arguments = listOf(
                    navArgument("personName") { type = NavType.StringType },
                    navArgument("expenseKey") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val personName = backStackEntry.arguments?.getString("personName")
                    ?.let { Uri.decode(it) } ?: ""
                val expenseKey = backStackEntry.arguments?.getString("expenseKey")
                    ?.let { Uri.decode(it) } ?: ""

                EditExpenseScreen(
                    personName = personName,
                    expenseKey = expenseKey,
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }

            // Add Expense screen
            composable("addExpense") {
                AddExpenseScreen(navController = navController)
            }
        }
    }
}

@Composable
private fun AccountsScreen() {
    var allExpenses by remember { mutableStateOf<List<PersonExpense>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        getAllExpenses { expenses ->
            allExpenses = expenses
            isLoading = false
        }
    }
    val summary = remember(allExpenses) { balanceSummary(allExpenses) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { AccountBalanceCard(
            totalBalance = summary.net,
            YouGet = summary.getBack,
            Due = summary.due
        )}
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item { WeeklyExpenseChartCard(allExpenses) }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item { MonthlyExpensesCard() }
        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitlyTopBar() {
    TopAppBar(title = { Text("Splitly") })
}

@Composable
private fun SplitlyBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = { navController.navigate("home") },
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = currentRoute == "accounts",
            onClick = { navController.navigate("accounts") },
            icon = { Icon(Icons.Filled.Person, contentDescription = "Accounts") },
            label = { Text("Accounts") }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SplitlyAppPreview() {
    MaterialTheme {
        SplitlyApp()
    }
}