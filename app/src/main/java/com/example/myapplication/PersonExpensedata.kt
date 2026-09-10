package com.example.myapplication

import android.R

/**
 * Plain data holder — no UI here. One person's expense entry.
 *
 */
data class PersonExpense(
    val name: String = "",
    val date: String = "",
    val expenseName: String = "",
    val Amount: String = "",
    val credit : String = "",
    val Debit :String = "",
)

val samplePersonExpenses = listOf(
    PersonExpense(
        name = "Alex",
        date = "Sep 5, 2026",
        expenseName = "Grocery Run",
        Amount = "90.0",
    ),
    PersonExpense(
        name = "Raj",
        date = "Sep 3, 2026",
        expenseName = "Electricity & Wifi",
        Amount = "60.0",
    ),
    PersonExpense(
        name = "Alex",
        date = "Aug 28, 2026",
        expenseName = "tk",
        Amount = "60.0",
    ),
    PersonExpense(
        name = "Raj",
        date = "Aug 28, 2026",
        expenseName = "kaufland",
        Amount = "60.0",
    ),
    PersonExpense(
        name = "Raj",
        date = "Aug 28, 2026",
        expenseName = "party",
        Amount = "60.0",
    ),
    PersonExpense(
        name = "Raj",
        date = "Aug 28, 2026",
        expenseName = "university day",
        Amount = "60.0",
    )

)