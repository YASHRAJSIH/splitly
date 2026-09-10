package com.example.myapplication
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable

/**
 * FILE PURPOSE: "Your Groups" section + "People" section.
 * This file does NOT know or care about total balances — AccountBalance.kt owns that.
 * It just renders a list of groups and a list of people, each with an amount.
 */

private val Green = Color(0xFF2ECC71)
private val Red = Color(0xFFE74C3C)
private val Purple = Color(0xFF5B6EF5)

// ---------- DATA MODELS ----------
// amount > 0  -> you are owed / they owe you
// amount < 0  -> you owe / you owe them
// amount == 0 -> settled
data class GroupItem(
    val name: String,
    val memberCount: Int,
    val amount: Double,
    val iconColor: Color
)

data class PersonItem(
    val name: String,
    val amount: Double,
    val avatarColor: Color
)

// ---------- SAMPLE DATA (swap for real data later) ----------
private val sampleGroups = listOf(
    GroupItem("Berlin Trip", 4, 80.0, Purple),
    GroupItem("Flat Expenses", 2, -25.0, Color(0xFFF5A15B)),
    GroupItem("Weekend with Friends", 5, 0.0, Color(0xFFB0B0B0))
)

private val samplePeople = listOf(
    PersonItem("Alex", 50.0, Color(0xFFE8B98C)),
    PersonItem("Sarah", -20.0, Color(0xFFD98C8C)),
    PersonItem("Mike", 0.0, Color(0xFFB0B0B0))
)

// ---------- GROUPS ----------
@Composable
fun GroupsSection(groups: List<GroupItem> = sampleGroups) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Your Groups", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("View All", fontSize = 14.sp, color = Purple)
        }

        groups.forEach { group -> GroupRow(group) }
    }
}

@Composable
private fun GroupRow(group: GroupItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // colored square "icon" for now — swap for a real icon/image later
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(group.iconColor, RoundedCornerShape(12.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(group.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("${group.memberCount} members", fontSize = 13.sp, color = Color.Gray)
        }

        when {
            group.amount > 0 -> TwoLineAmount(
                amount = "+$${"%.2f".format(group.amount)}",
                label = "You are owed",
                color = Green
            )
            group.amount < 0 -> TwoLineAmount(
                amount = "-$${"%.2f".format(-group.amount)}",
                label = "You owe",
                color = Red
            )
            else -> TwoLineAmount(amount = "Settled", label = "All good", color = Color.Gray)
        }
    }
}

@Composable
private fun TwoLineAmount(amount: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.End) {
        Text(amount, color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(label, color = color, fontSize = 11.sp)
    }
}

// ---------- PEOPLE ----------
@Composable
fun PeopleSection(people: List<PersonItem> = samplePeople,
                  onPersonClick: (PersonItem) -> Unit = {} ) {
    Column {
        Text(
            "People",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        people.forEach { person -> PersonRow(person, onClick = { onPersonClick(person)}) }
    }
}

@Composable
private fun PersonRow(person: PersonItem,onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // circular avatar with initial — swap for a real photo (e.g. via Coil) later
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(person.avatarColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                person.name.first().uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(person.name, fontSize = 15.sp, modifier = Modifier.weight(1f))

        when {
            person.amount > 0 -> Text(
                "+$${"%.2f".format(person.amount)}",
                color = Green,
                fontWeight = FontWeight.SemiBold
            )
            person.amount < 0 -> Text(
                "-$${"%.2f".format(-person.amount)}",
                color = Red,
                fontWeight = FontWeight.SemiBold
            )
            else -> Text("Settled", color = Color.Gray)
        }
    }
}

// Lets you preview both sections stacked, without running the app.
@Preview(showBackground = true)
@Composable
private fun GroupsAndPeoplePreview() {
    Column {
        GroupsSection()
        PeopleSection()
    }
}
