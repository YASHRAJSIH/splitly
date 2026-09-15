// AccountBalanceViewModel.kt
package com.example.myapplication

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

data class Expense(
    val amount: String = "",
    val credit: String = "",
    val debit: String = "",
    val date: String = "",
    val expenseName: String = "",
    val name: String = ""
)

class AccountBalanceViewModel : ViewModel() {

    private val _youAreOwed = mutableStateOf(0.0)
    val youAreOwed: State<Double> = _youAreOwed

    private val _youOwe = mutableStateOf(0.0)
    val youOwe: State<Double> = _youOwe

    private val _totalBalance = mutableStateOf(0.0)
    val totalBalance: State<Double> = _totalBalance

    // CHECK THIS PATH — "expenses" assumes a global node, not per-user
    private val ref = FirebaseDatabase.getInstance().getReference("expenses")

    private val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            var owed = 0.0
            var owe = 0.0

            for (child in snapshot.children) {
                val expense = child.getValue(Expense::class.java) ?: continue
                val credit = expense.credit.toDoubleOrNull() ?: 0.0
                val debit = expense.debit.toDoubleOrNull() ?: 0.0

                if (credit > 0) owed += credit
                if (debit < 0) owe += -debit
            }

            _youAreOwed.value = owed
            _youOwe.value = owe
            _totalBalance.value = owed - owe
        }

        override fun onCancelled(error: DatabaseError) {
            // TODO: this currently fails silently — at least log it
        }
    }

    init {
        ref.addValueEventListener(listener)
    }

    override fun onCleared() {
        super.onCleared()
        ref.removeEventListener(listener)
    }
}