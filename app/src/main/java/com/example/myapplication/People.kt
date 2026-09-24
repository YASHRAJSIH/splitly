package com.example.myapplication
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable

private val Green = Color(0xFF2ECC71)
private val Red = Color(0xFFE74C3C)
private val Purple = Color(0xFF5B6EF5)


data class PersonItem(
    val name: String,
    val amount: Double,
    val avatarColor: Color
)

public val samplePeople = listOf(
    PersonItem("Kishan", 50.0, Color(0xFFE8B98C)),
    PersonItem("Ankita", -20.0, Color(0xFFD98C8C)),
    PersonItem("Devanshi", 0.0, Color(0xFFB0B0B0))
)



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

