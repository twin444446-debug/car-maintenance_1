package com.example.carmaintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MaintenanceMatrixScreen(
    car: Car?,
    works: List<MaintenanceWork>,
    records: List<ServiceRecord>,
    onBack: () -> Unit
) {
    if (car == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Сначала добавьте автомобиль", color = TextSecondary)
            TextButton(onClick = onBack) { Text("← Назад", color = ColorBlue) }
        }
        return
    }

    val currentMileage = car.mileage.toIntOrNull() ?: 0
    val carWorks = works.filter { it.carId == car.id }

    // Строим таблицу до 100 000 или до (текущий пробег + 20 000)
    val maxMileage = maxOf(100000, ((currentMileage / 10000) + 2) * 10000)
    val mileageSteps = (1..(maxMileage / 10000)).map { it * 10000 }

    // Размеры (пока базовые, потом подгоним)
    val colNameWidth = 140.dp
    val colStepWidth = 100.dp 
    val rowHeight = 60.dp

    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) { Text("← Назад", color = ColorBlue) }
            Text("Матрица ТО", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        
        Text("${car.brand} ${car.model} • Пробег: $currentMileage км", color = ColorGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, TextSecondary.copy(alpha = 0.3f))
                .horizontalScroll(rememberScrollState())
        ) {
            Column {
                // Заголовок таблицы (Шаги пробега)
                Row(modifier = Modifier.background(CardColor)) {
                    Box(
                        modifier = Modifier.width(colNameWidth).padding(8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text("Работа", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    
                    mileageSteps.forEach { step ->
                        Box(
                            modifier = Modifier.width(colStepWidth).padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${step / 1000}к", 
                                color = if (currentMileage >= step) ColorGreen else TextSecondary,
                                fontWeight = FontWeight.Bold, 
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))

                // Тело таблицы (Список работ)
                LazyColumn(modifier = Modifier.fillMaxHeight()) {
                    items(carWorks) { work ->
                        Row(
                            modifier = Modifier.background(if (carWorks.indexOf(work) % 2 == 0) BgColor else CardColor.copy(alpha = 0.5f))
                        ) {
                            // Название работы
                            Column(
                                modifier = Modifier.width(colNameWidth).height(rowHeight).padding(8.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    work.name, 
                                    color = TextPrimary, 
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (work.intervalKm > 0) {
                                    Text("каждые ${work.intervalKm / 1000}к", color = TextSecondary, fontSize = 10.sp)
                                }
                            }

                            // Ячейки
                            mileageSteps.forEach { step ->
                                val isTargetStep = work.intervalKm > 0 && step % work.intervalKm == 0
                                
                                if (isTargetStep) {
                                    // Ищем выполнение в радиусе +/- 3000 км от шага
                                    val record = records.find { r ->
                                        r.workId == work.id && 
                                        Math.abs(r.mileage - step) <= 3000
                                    }

                                    val isOverdue = step <= currentMileage && record == null
                                    val cellColor = when {
                                        record != null -> ColorGreen.copy(alpha = 0.8f)
                                        isOverdue -> ColorRed.copy(alpha = 0.15f)
                                        else -> Color.Transparent
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(colStepWidth)
                                            .height(rowHeight)
                                            .border(0.5.dp, TextSecondary.copy(alpha = 0.1f))
                                            .background(cellColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (record != null) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                Text(
                                                    text = "${record.mileage}",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = formatShortDate(record.date),
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    fontSize = 9.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        } else if (isOverdue) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = ColorRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .width(colStepWidth)
                                            .height(rowHeight)
                                            .border(0.5.dp, TextSecondary.copy(alpha = 0.1f))
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f))
                    }
                }
            }
        }
    }
}

private fun formatShortDate(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val sdf = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
