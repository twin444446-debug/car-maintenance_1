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

// --- КАРТОЧКА РАБОТЫ ---
@Composable
fun WorkCard(work: MaintenanceWork, car: Car, records: List<ServiceRecord>, onClick: () -> Unit) {
    val status = getWorkStatus(work, car, records)
    val currentMileage = car.mileage.toIntOrNull() ?: 0
    
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

            // Название и строка с СЛЕДУЮЩИМ пробегом
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
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val nextMileage = if (work.hasLastExecution) work.lastMileage + work.intervalKm else currentMileage + work.intervalKm
                    
                    if (work.hasLastExecution) {
                        Text(
                            text = "$nextMileage км",
                            color = ColorRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "— ($nextMileage км",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    
                    if (work.intervalMonths > 0) {
                        Text(
                            text = " / ${work.intervalMonths} мес.",
                            color = ColorRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Text(
                        text = if (work.hasLastExecution) "" else ")",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Правый бейдж фиксированной ширины
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .background(style.bg, RoundedCornerShape(50))
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = style.text,
                    color = style.textColor,
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

// --- ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ---

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
    Text(
        text = text,
        color = TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        Text(value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// Вспомогательный data-класс
data class WorkCardStyle(
    val icon: ImageVector,
    val iconColor: Color,
    val text: String,
    val bg: Color,
    val textColor: Color
)
