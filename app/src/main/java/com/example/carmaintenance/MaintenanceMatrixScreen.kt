package com.example.carmaintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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

    // Состояние вкладки: 0 - Автомобиль, 1 - ГБО
    var selectedTab by remember { mutableIntStateOf(0) }

    // Фильтруем работы по вкладке
    val filteredWorks = if (selectedTab == 0) {
        carWorks.filter { it.category == WorkCategory.GENERAL }
    } else {
        carWorks.filter { it.category == WorkCategory.LPG }
    }

    // Сетка всегда с шагом 10 000 км
    val maxMileage = maxOf(120000, ((currentMileage / 10000) + 2) * 10000)
    val mileageSteps = (1..(maxMileage / 10000)).map { it * 10000 }

    // Увеличили ширину и высоту для 3 строк
    val colNameWidth = 160.dp
    val colStepWidth = 70.dp
    val rowHeight = 80.dp

    var selectedWork by remember { mutableStateOf<MaintenanceWork?>(null) }

    val groupedWorks = filteredWorks
        .groupBy { it.groupName.ifBlank { "9. БЕЗ ГРУППЫ" } }
        .toList()
        .sortedBy { (groupName, _) ->
            val number = groupName.takeWhile { it.isDigit() }.toIntOrNull()
            number ?: 999
        }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onBack) { Text("← Назад", color = ColorBlue) }
                Text("Матрица ТО", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            
            Text("${car.brand} ${car.model} • Пробег: $currentMileage км", color = ColorGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))

            // Вкладки (показываем "ГБО" только если у машины есть ГБО)
            if (car.hasLpg) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = ColorGreen.copy(alpha = 0.2f), activeContentColor = ColorGreen),
                        modifier = Modifier.weight(1f)
                    ) { Text("Автомобиль", maxLines = 1) }

                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = ColorGreen.copy(alpha = 0.2f), activeContentColor = ColorGreen),
                        modifier = Modifier.weight(1f)
                    ) { Text("ГБО", maxLines = 1) }
                }
                Spacer(Modifier.height(8.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, TextSecondary.copy(alpha = 0.3f))
                    .horizontalScroll(rememberScrollState())
            ) {
                Column {
                    // Заголовок таблицы
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

                    // Тело таблицы
                    LazyColumn(modifier = Modifier.fillMaxHeight()) {
                        if (groupedWorks.isEmpty()) {
                            item {
                                Text(
                                    "Нет работ в этой категории",
                                    color = TextSecondary,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                        groupedWorks.forEach { (groupName, groupWorks) ->
                            item {
                                Row(modifier = Modifier.background(CardColor.copy(alpha = 0.8f))) {
                                    Box(modifier = Modifier.width(colNameWidth + (colStepWidth * mileageSteps.size)).padding(8.dp)) {
                                        Text(groupName, color = ColorBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                                HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))
                            }

                            items(groupWorks) { work ->
                                Row(modifier = Modifier.background(if (groupWorks.indexOf(work) % 2 == 0) BgColor else CardColor.copy(alpha = 0.5f))) {
                                    // Ячейка с названием (кликабельная)
                                    Column(
                                        modifier = Modifier
                                            .width(colNameWidth)
                                            .height(rowHeight)
                                            .clickable { selectedWork = work }
                                            .padding(8.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            work.name, 
                                            color = TextPrimary, 
                                            fontSize = 12.sp,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (work.intervalKm > 0) {
                                            Text("кажд. ${work.intervalKm / 1000}к", color = TextSecondary, fontSize = 10.sp)
                                        }
                                    }

                                    // Ячейки с шагами
                                    mileageSteps.forEach { step ->
                                        // Ищем запись, которая при округлении попадает в этот шаг
                                        val record = records.find { r ->
                                            r.workId == work.id && 
                                            (Math.round(r.mileage / 10000f) * 10000) == step
                                        }

                                        // Проверяем, была ли замена на этом шаге или на любом предыдущем
                                        val wasDoneEarlier = records.any { r ->
                                            r.workId == work.id && r.mileage <= step
                                        }

                                        // Ячейка плановая, если шаг кратен интервалу
                                        val isPlannedStep = work.intervalKm > 0 && step % work.intervalKm == 0

                                        // Просрочка: шаг плановый, пробег машины его прошел, записи нет, и раньше не было
                                        val isOverdue = isPlannedStep && step <= currentMileage && record == null && !wasDoneEarlier

                                        // Закрыто предыдущей заменой: шаг плановый, записи нет, но раньше было выполнено
                                        val isCoveredByPrevious = isPlannedStep && record == null && wasDoneEarlier

                                        val cellColor = when {
                                            record != null -> ColorGreen.copy(alpha = 0.8f)       // Запись на этом шаге
                                            isCoveredByPrevious -> ColorGreen.copy(alpha = 0.2f) // Закрыто предыдущей
                                            isOverdue -> ColorRed.copy(alpha = 0.15f)            // Просрочено
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
                                            when {
                                                record != null -> {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                        Text(text = "${record.mileage}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                                        Text(text = formatShortDate(record.date), color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp, maxLines = 1)
                                                    }
                                                }
                                                isCoveredByPrevious -> {
                                                    // Бледная галочка (выполнено раньше)
                                                    Icon(Icons.Default.Check, contentDescription = "Выполнено ранее", tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                                }
                                                isOverdue -> {
                                                    // Красный крестик (просрочено)
                                                    Icon(Icons.Default.Close, contentDescription = "Просрочено", tint = Color.Red, modifier = Modifier.size(16.dp))
                                                }
                                            }
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

        // Всплывающая подсказка
        if (selectedWork != null) {
            val work = selectedWork!!
            val lastRecord = records.filter { it.workId == work.id }.maxByOrNull { it.mileage }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() }, 
                        indication = null
                    ) { selectedWork = null }
            )

            Popup(
                alignment = Alignment.Center,
                onDismissRequest = { selectedWork = null },
                properties = PopupProperties(focusable = true)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardColor),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(work.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))
                        
                        if (work.groupName.isNotBlank()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Группа:", color = TextSecondary, fontSize = 13.sp)
                                Text(work.groupName, color = ColorBlue, fontSize = 13.sp)
                            }
                        }
                        
                        if (work.intervalKm > 0) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Интервал:", color = TextSecondary, fontSize = 13.sp)
                                Text("${work.intervalKm} км", color = TextPrimary, fontSize = 13.sp)
                            }
                        }
                        
                        if (lastRecord != null) {
                            Spacer(Modifier.height(4.dp))
                            Text("Последнее:", color = TextSecondary, fontSize = 12.sp)
                            Text("${lastRecord.mileage} км • ${formatDate(lastRecord.date)}", color = ColorGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            
                            if (work.intervalKm > 0) {
                                val nextMileage = lastRecord.mileage + work.intervalKm
                                val remaining = nextMileage - currentMileage
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Остаток:", color = TextSecondary, fontSize = 13.sp)
                                    Text("$remaining км", color = if (remaining < 0) Color.Red else ColorGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Text("Нет записей о выполнении", color = TextSecondary, fontSize = 13.sp)
                        }
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
