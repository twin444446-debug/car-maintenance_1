package com.example.carmaintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatusBlock(title: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = .16f)), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(value.toString(), color = color, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(title, color = TextPrimary, fontSize = 12.sp)
        }
    }
}

@Composable
fun QuickAction(text: String, icon: @Composable () -> Unit, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = CardColor), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(38.dp).background(color.copy(alpha = .18f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.height(8.dp))
            Text(text, color = TextPrimary, fontSize = 12.sp)
        }
    }
}

@Composable
fun WorkCard(work: MaintenanceWork, car: Car, records: List<ServiceRecord>, onClick: () -> Unit) {
    val status = getWorkStatus(work, car, records)
    val color = statusColor(status)
    val dueKm = nextDueMileage(work, records)
    val dueDate = nextDueDate(work, records)
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = CardColor), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(color, RoundedCornerShape(50)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(work.name, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(statusLabel(status), color = color, fontSize = 12.sp)
                val due = buildList {
                    dueKm?.let { add("следующее: $it км") }
                    dueDate?.let { add(formatDate(it)) }
                }.joinToString(" • ")
                if (due.isNotBlank()) Text(due, color = TextSecondary, fontSize = 12.sp)
            }
            Text("›", color = TextSecondary, fontSize = 28.sp)
        }
    }
}

@Composable
fun SectionTitle(text: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action, color = ColorBlue) }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary)
        Text(value.ifBlank { "—" }, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}
