package com.example.myapplication

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

/**
 * FILE PURPOSE: Read and write PersonExpense data to Firebase.
 * - Write: Save new expenses to global list AND to person's transaction history
 * - Read: Fetch transactions for a specific person
 */

data class PersonExpense(
    val name: String = "",
    val date: String = "",
    val expenseName: String = "",
    val Amount: String = "",
    val credit : String = "",
    val Debit :String = "",
)

val listPersonExpenses = listOf(
    PersonExpense(
    ))

private val database = FirebaseDatabase.getInstance()
private val expensesRef = database.reference.child("expenses")

// ============ WRITE: UPLOAD EXPENSES ============
fun uploadPersonExpenses(
    expenses: List<PersonExpense> = listPersonExpenses,
    onComplete: (success: Boolean, message: String) -> Unit = { _, _ -> }
) {
    expenses.forEach { expense ->
        // Push to global expenses list
        val key = expensesRef.push().key ?: return@forEach

        println("🔵 Uploading: ${expense.name} - ${expense.expenseName}")

        expensesRef.child(key).setValue(expense)
            .addOnSuccessListener {
                println("✅ Global: ${expense.name} - ${expense.expenseName}")

                // Also save to person's transaction history
                val personRef = database.reference
                    .child("people")
                    .child(expense.name)
                    .child("transactions")
                    .push()

                personRef.setValue(expense)
                    .addOnSuccessListener {
                        println("✅ Person History: ${expense.name}")
                        onComplete(true, "Saved ${expense.expenseName}")
                    }
                    .addOnFailureListener { error ->
                        println("❌ Person History Failed: ${error.message}")
                        onComplete(false, error.message ?: "Failed to save transaction")
                    }
            }
            .addOnFailureListener { error ->
                println("❌ Global Upload Failed: ${error.message}")
                onComplete(false, error.message ?: "Upload failed")
            }
    }
}

// ============ READ: GET PERSON'S TRANSACTIONS ============
fun getPersonTransactions(
    personName: String,
    onComplete: (transactions: List<PersonExpense>) -> Unit
) {
    val personTransactionsRef = database.reference
        .child("people")
        .child(personName)
        .child("transactions")

    personTransactionsRef.addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val transactions = mutableListOf<PersonExpense>()

            for (transactionSnapshot in snapshot.children) {
                val transaction = transactionSnapshot.getValue(PersonExpense::class.java)
                if (transaction != null) {
                    transactions.add(transaction)
                }
            }

            println("📖 Loaded ${transactions.size} transactions for $personName")
            onComplete(transactions)
        }

        override fun onCancelled(error: DatabaseError) {
            println("❌ Failed to read transactions: ${error.message}")
            onComplete(emptyList())
        }
    })
}

// ============ READ: GET ALL EXPENSES ============
fun getAllExpenses(
    onComplete: (expenses: List<PersonExpense>) -> Unit
) {
    expensesRef.addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val expenses = mutableListOf<PersonExpense>()

            for (expenseSnapshot in snapshot.children) {
                val expense = expenseSnapshot.getValue(PersonExpense::class.java)
                if (expense != null) {
                    expenses.add(expense)
                }
            }

            println("📖 Loaded ${expenses.size} total expenses")
            onComplete(expenses)
        }

        override fun onCancelled(error: DatabaseError) {
            println("❌ Failed to read expenses: ${error.message}")
            onComplete(emptyList())
        }
    })
}