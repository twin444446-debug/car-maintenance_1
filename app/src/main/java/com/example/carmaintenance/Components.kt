package com.example.carmaintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WorkCard(work: MaintenanceWork, car: Car, records: List<ServiceRecord>, onClick: () -> Unit) {
    val status = getWorkStatus(work, car, records)
    val currentMileage = car.mileage.toIntOrNull() ?: 0

    val lastMileage = if (work.hasLastExecution) work.lastMileage else 0
    val remainingKm = lastMileage + work.intervalKm - currentMileage

    val style = when (status) {
        WorkStatus.REQUIRED -> WorkCardStyle(Icons.Default.PriorityHigh, Color.Red, "Требуется", Color.Red.copy(alpha = 0.15f), Color.Red)
        WorkStatus.PREVENTIVE -> WorkCardStyle(Icons.Default.Schedule, Color.Yellow, "Скоро", Color.Yellow.copy(alpha = 0.15f), Color.Yellow)
        WorkStatus.COMPLETED -> WorkCardStyle(Icons.Default.Check, Color.Green, "Выполнено", Color.Green.copy(alpha = 0.15f), Color.Green)
        WorkStatus.SKIPPED -> WorkCardStyle(Icons.Default.Close, Color(0xFF9C27B0), "Пропущено", Color(0xFF9C27B0).copy(alpha = 0.15f), Color(0xFF9C27B0))
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(style.iconColor.copy(alpha = 0.25f), CircleShape)
                    .padding(4.dp)
                    .background(style.iconColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = work.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))

                Text(
                    text = "$remainingKm км",
                    color = if (remainingKm > 0) TextPrimary else Color.Red,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier.width(90.dp).background(style.bg, RoundedCornerShape(50)).padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = style.text,
                    color = style.iconColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ServiceRecordCard(
    record: ServiceRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDate(record.date),
                    color = ColorBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${record.mileage} км",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = record.reason.ifBlank { "Обслуживание" },
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            record.lines.take(2).forEach { line ->
                Text(
                    text = "• ${line.name}${if (line.article.isNotBlank()) " [${line.article}]" else ""} — ${formatMoney(line.cost)} ${line.currency}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (record.lines.size > 2) {
                Text("...и ещё ${record.lines.size - 2}", color = TextSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDelete) {
                    Text("Удалить", color = ColorRed, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun QuickAction(title: String, icon: @Composable () -> Unit, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(Modifier.height(8.dp))
            Text(title, color = TextPrimary, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun StatusBlock(title: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(80.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("$count", color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        Text(value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

data class WorkCardStyle(
    val icon: ImageVector,
    val iconColor: Color,
    val text: String,
    val bg: Color,
    val textColor: Color
)
