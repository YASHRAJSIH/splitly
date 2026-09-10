package com.example.myapplication
/**
 * Plain data holder — no UI here. One person's expense entry.
 *
 */
data class PersonExpense(
    val name: String = "",
    val date: String = "",
    val expenseName: String = "",
    val money: Double = 0.0,
)

val samplePersonExpenses = listOf(
    PersonExpense(
        name = "Alex",
        date = "Sep 5, 2026",
        expenseName = "Grocery Run",
        money = 10.0,
    ),
    PersonExpense(
        name = "Raj",
        date = "Sep 3, 2026",
        expenseName = "Electricity & Wifi",
        money = 60.0,
    ),
    PersonExpense(
        name = "Alex",
        date = "Aug 28, 2026",
        expenseName = "tk",
        money = 180.0,
    ),
    PersonExpense(
        name = "Raj",
        date = "Aug 28, 2026",
        expenseName = "kaufland",
        money = 120.0,
    ),
    PersonExpense(
        name = "Raj",
        date = "Aug 28, 2026",
        expenseName = "party",
        money = 60.0,
    ),
    PersonExpense(
        name = "Raj",
        date = "Aug 28, 2026",
        expenseName = "university day",
        money = 90.0,
    )

)