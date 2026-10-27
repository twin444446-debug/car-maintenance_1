package com.example.carmaintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
    
    // Определяем стили
    val style = when (status) {
        WorkStatus.REQUIRED -> WorkCardStyle(Icons.Default.PriorityHigh, ColorRed, "Требуется", ColorRed.copy(alpha = 0.15f), ColorRed)
        WorkStatus.PREVENTIVE -> WorkCardStyle(Icons.Default.Schedule, ColorYellow, "Скоро", ColorYellow.copy(alpha = 0.15f), ColorYellow)
        WorkStatus.COMPLETED -> WorkCardStyle(Icons.Default.Check, ColorGreen, "Выполнено", ColorGreen.copy(alpha = 0.15f), ColorGreen)
        WorkStatus.SKIPPED -> WorkCardStyle(Icons.Default.Close, ColorPurple, "Пропущено", ColorPurple.copy(alpha = 0.15f), ColorPurple)
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
            // Левая иконка
            Box(
                modifier = Modifier.size(40.dp).background(style.iconColor.copy(alpha = 0.15f), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = style.icon, contentDescription = null, tint = style.iconColor, modifier = Modifier.size(24.dp))
            }

            Spacer(Modifier.width(12.dp))

            // Название и компактная строка с пробегами
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
                
                // Строка с пробегами: [выполнено] -> [следующий] (срок)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (work.hasLastExecution) {
                        Text(
                            text = "${work.lastMileage} км",
                            color = ColorGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "—",
                            color = ColorGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Text(
                        text = " (",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    
                    // Вычисляем следующий пробег
                    val nextMileage = if (work.hasLastExecution) work.lastMileage + work.intervalKm else currentMileage + work.intervalKm
                    Text(
                        text = "$nextMileage км",
                        color = ColorRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (work.intervalMonths > 0) {
                        Text(
                            text = " / ${work.intervalMonths} мес.",
                            color = ColorRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = ")",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Правый бейдж и стрелка
            Box(
                modifier = Modifier.background(style.bg, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(text = style.text, color = style.textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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

// Вспомогательный data-класс для стилей карточки работы
data class WorkCardStyle(
    val icon: ImageVector,
    val iconColor: Color,
    val text: String,
    val bg: Color,
    val textColor: Color
)
