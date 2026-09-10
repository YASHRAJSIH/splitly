package com.example.myapplication

import com.google.firebase.database.FirebaseDatabase

/**
 * FILE PURPOSE: send PersonExpense data to Firebase Realtime Database.
 * Nothing else — reading it back is a separate job for later.
 */

// "expenses" is just the top-level node name in your database tree —
// call it whatever you want, it'll show up under that name in the
// Firebase console's Realtime Database tab.
private val expensesRef = FirebaseDatabase.getInstance().reference.child("expenses")

fun uploadPersonExpenses(
    expenses: List<PersonExpense> = samplePersonExpenses,
    onComplete: (success: Boolean, message: String) -> Unit = { _, _ -> }
) {
    expenses.forEach { expense ->
        // push() generates a unique key per entry so multiple expenses
        // don't overwrite each other — without it, every call would
        // stomp on the same node.
        val key = expensesRef.push().key ?: return@forEach

        expensesRef.child(key).setValue(expense)
            .addOnSuccessListener {
                println("Uploaded: ${expense.name} — ${expense.expenseName}")
                onComplete(true, "Saved ${expense.expenseName}")
            }
            .addOnFailureListener { error ->
                // If this fires, check Logcat — 9 times out of 10 it's a
                // "Permission denied" from your database Rules, not your code.
                println("Upload failed: ${error.message}")
                onComplete(false, error.message ?: "Upload failed")
            }
    }
}

