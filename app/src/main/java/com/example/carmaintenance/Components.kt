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
    
    // Вычисляем, что писать под названием
    val subtitle = buildString {
        if (work.hasLastExecution) {
            append("Пробег: ${work.lastMileage} км")
            if (work.intervalKm > 0) {
                val remaining = work.intervalKm - (car.mileage.toIntOrNull() ?: 0 - work.lastMileage)
                if (status == WorkStatus.REQUIRED || status == WorkStatus.PREVENTIVE) {
                    if (remaining > 0) append(" • Через $remaining км")
                    else append(" • Просрочено")
                } else if (status == WorkStatus.COMPLETED) {
                    append(" • Выполнено ${formatDate(work.lastDate)}")
                }
            } else if (status == WorkStatus.COMPLETED) {
                append(" • Выполнено ${formatDate(work.lastDate)}")
            }
        } else {
            append("Пробег: ${car.mileage} км")
            if (work.intervalKm > 0) append(" • Через ${work.intervalKm} км")
        }
    }

    // Определяем цвета и иконки в зависимости от статуса
    val style = when (status) {
        WorkStatus.REQUIRED -> WorkCardStyle(
            icon = Icons.Default.PriorityHigh, 
            iconColor = ColorRed, 
            text = "Требуется", 
            bg = ColorRed.copy(alpha = 0.15f), 
            textColor = ColorRed
        )
        WorkStatus.PREVENTIVE -> WorkCardStyle(
            icon = Icons.Default.Schedule,
            iconColor = ColorYellow, 
            text = "Скоро", 
            bg = ColorYellow.copy(alpha = 0.15f), 
            textColor = ColorYellow
        )
        WorkStatus.COMPLETED -> WorkCardStyle(
            icon = Icons.Default.Check, 
            iconColor = ColorGreen, 
            text = "Выполнено", 
            bg = ColorGreen.copy(alpha = 0.15f), 
            textColor = ColorGreen
        )
        WorkStatus.SKIPPED -> WorkCardStyle(
            icon = Icons.Default.Close, 
            iconColor = ColorPurple, 
            text = "Пропущено", 
            bg = ColorPurple.copy(alpha = 0.15f), 
            textColor = ColorPurple
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Левая иконка
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(style.iconColor.copy(alpha = 0.15f), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    tint = style.iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Название и подзаголовок
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = work.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            // Правая плашка (Бейдж)
            Box(
                modifier = Modifier
                    .background(style.bg, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = style.text,
                    color = style.textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Стрелочка
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

// Вспомогательный data-класс для стилей карточки работы
data class WorkCardStyle(
    val icon: ImageVector,
    val iconColor: Color,
    val text: String,
    val bg: Color,
    val textColor: Color
)
