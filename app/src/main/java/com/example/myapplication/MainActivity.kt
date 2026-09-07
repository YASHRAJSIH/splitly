package com.example.myapplication
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

/**
 * MainActivity's ONLY job: host the Scaffold (top bar / bottom bar / FAB) and
 * decide which page is shown ("home" vs "accounts"). It does not draw any of
 * the actual content — that comes from AccountBalance.kt and GroupsAndPeople.kt.
 *
 * Needed dependencies (add to app/build.gradle if not already there):
 *   implementation("androidx.navigation:navigation-compose:2.8.0")
 *   implementation(platform("androidx.compose:compose-bom:2024.09.00"))
 *   implementation("androidx.compose.material3:material3")
 *   implementation("androidx.activity:activity-compose:1.9.2")
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

    Scaffold(
        topBar = { SplitlyTopBar() },
        bottomBar = { SplitlyBottomBar(navController) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: navigate to "add expense" once that screen exists */ },
                containerColor = Color(0xFF5B6EF5)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add expense", tint = Color.White)
            }
        }
    ) { innerPadding ->
        // ---- ROUTING TABLE: this is the "pages path and redirection" part ----
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen() }
            composable("accounts") { AccountsScreen() }
        }
    }
}

// The Home page = balance card + groups + people, stacked and scrollable.
@Composable
private fun HomeScreen() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { AccountBalanceCard() }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item { GroupsSection() }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item { PeopleSection() }
        item { Spacer(modifier = Modifier.height(80.dp)) } // clears the FAB
    }
}

// Placeholder second page — build this out next, same pattern as HomeScreen.
@Composable
private fun AccountsScreen() {
//    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//        Text("Accounts screen — build this next")
//    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { AccountBalanceCard() }
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
