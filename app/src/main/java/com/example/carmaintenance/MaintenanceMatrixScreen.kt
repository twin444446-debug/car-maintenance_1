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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

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

    var selectedTab by remember { mutableIntStateOf(0) }

    val filteredWorks = if (selectedTab == 0) {
        carWorks.filter { it.category == WorkCategory.GENERAL }
    } else {
        carWorks.filter { it.category == WorkCategory.LPG }
    }

    // Шаг сетки: 10 000 для авто, 15 000 для ГБО
    val gridStep = if (selectedTab == 1) 15000 else 10000

    // Запас вперед на 100 000 км от текущего пробега, округленный по шагу сетки
    val maxMileageRaw = maxOf(120000, currentMileage + 100000)
    val maxMileage = ((maxMileageRaw + gridStep - 1) / gridStep) * gridStep
    val mileageSteps = (1..(maxMileage / gridStep)).map { it * gridStep }

    val colNameWidth = 160.dp
    val colStepWidth = 70.dp
    val rowHeight = 80.dp

    var selectedWork by remember { mutableStateOf<MaintenanceWork?>(null) }

    // --- ЛОГИКА АВТОПРОКРУТКИ ---
    val horizontalScrollState = rememberScrollState()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    
    LaunchedEffect(selectedTab) { // Перезапускаем скролл при смене вкладки
        delay(100)
        val currentStepIndex = mileageSteps.indexOfFirst { it >= currentMileage }.coerceAtLeast(0)
        val scrollToStep = mileageSteps.getOrElse(currentStepIndex) { currentMileage }
        
        val scrollPositionPx = with(density) {
            (colNameWidth + (colStepWidth * (scrollToStep / gridStep.toFloat()))).toPx()
        }
        
        val screenWidthPx = with(density) { screenWidthDp.dp.toPx() }
        val targetScroll = (scrollPositionPx - screenWidthPx / 2).toInt().coerceAtLeast(0)
        
        horizontalScrollState.animateScrollTo(targetScroll)
    }

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
                    .horizontalScroll(horizontalScrollState)
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
                                    // Ячейка с названием
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

                                    // --- ЛОГИКА РАСЧЕТА ДЛЯ ЭТОЙ РАБОТЫ ---
                                    val workRecords = records.filter { it.workId == work.id }.sortedBy { it.mileage }
                                    val lastRecord = workRecords.maxByOrNull { it.mileage }

                                    val plannedPoints = mutableListOf<Int>()
                                    if (work.intervalKm > 0) {
                                        var p = work.intervalKm
                                        while (p <= maxMileage) {
                                            plannedPoints.add(p)
                                            p += work.intervalKm
                                        }
                                    }

                                    val projectedPoints = mutableListOf<Int>()
                                    if (work.intervalKm > 0 && lastRecord != null) {
                                        var p = lastRecord.mileage + work.intervalKm
                                        while (p <= maxMileage) {
                                            projectedPoints.add(Math.round(p / gridStep.toFloat()) * gridStep)
                                            p += work.intervalKm
                                        }
                                    }

                                    // Ячейки с шагами
                                    mileageSteps.forEach { step ->
                                        val record = workRecords.find { Math.round(it.mileage / gridStep.toFloat()) * gridStep == step }

                                        val isActual = record != null
                                        val isPlannedOriginal = plannedPoints.contains(step)
                                        val isProjected = projectedPoints.contains(step)
                                        val isPast = step <= currentMileage
                                        val wasDoneEarlier = workRecords.any { it.mileage <= step }

                                        val cellColor: Color
                                        var iconTint: Color? = null
                                        var iconVector: androidx.compose.ui.graphics.vector.ImageVector? = null
                                        var isFuturePlan = false

                                        when {
                                            // 1. Выполнено - ЗЕЛЕНЫЙ ФОН, БЕЛАЯ ГАЛОЧКА
                                            isActual -> {
                                                cellColor = ColorGreen.copy(alpha = 0.8f)
                                                iconVector = Icons.Default.Check
                                                iconTint = Color.White
                                            }
                                            // 2. Предстоящая (Будущая) - БЛЕДНО-КРАСНЫЙ ФОН, КРАСНАЯ ГАЛОЧКА В КРУЖКЕ
                                            !isActual && !isPast && isProjected -> {
                                                cellColor = Color.Red.copy(alpha = 0.05f)
                                                iconVector = Icons.Default.Check
                                                iconTint = Color.Red
                                                isFuturePlan = true
                                            }
                                            // 3. Отмененная старая точка - СЕРАЯ ГАЛОЧКА
                                            !isActual && wasDoneEarlier && isPast && isPlannedOriginal -> {
                                                cellColor = Color.Transparent
                                                iconVector = Icons.Default.Check
                                                iconTint = TextSecondary.copy(alpha = 0.3f)
                                            }
                                            // 4. ПРОСРОЧКА (Авто/Ручная) - БЛЕДНО-ФИОЛЕТОВЫЙ ФОН, КРАСНЫЙ КРЕСТИК
                                            !isActual && !wasDoneEarlier && isPast && (isPlannedOriginal || isProjected) -> {
                                                cellColor = ColorPurple.copy(alpha = 0.15f)
                                                iconVector = Icons.Default.Close
                                                iconTint = Color.Red
                                            }
                                            // 5. Будущая плановая точка (отменена) - ПРОЗРАЧНЫЙ ФОН, СЕРАЯ ГАЛОЧКА
                                            !isActual && !isPast && isPlannedOriginal && !isProjected -> {
                                                cellColor = Color.Transparent
                                                iconVector = Icons.Default.Check
                                                iconTint = TextSecondary.copy(alpha = 0.2f)
                                            }
                                            else -> {
                                                cellColor = Color.Transparent
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(colStepWidth)
                                                .height(rowHeight)
                                                .border(
                                                    width = if (isFuturePlan) 1.dp else 0.5.dp,
                                                    color = if (isFuturePlan) Color.Red.copy(alpha = 0.4f) else TextSecondary.copy(alpha = 0.1f)
                                                )
                                                .background(cellColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (iconVector != null && iconTint != null) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    if (isFuturePlan) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .border(1.dp, Color.Red.copy(alpha = 0.6f), RoundedCornerShape(50))
                                                                .padding(4.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = iconVector!!,
                                                                contentDescription = "Предстоящая замена",
                                                                tint = iconTint!!,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    } else {
                                                        Icon(
                                                            imageVector = iconVector!!,
                                                            contentDescription = null,
                                                            tint = iconTint!!,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    
                                                    if (isActual && record != null) {
                                                        Text(text = "${record.mileage}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                                        Text(text = formatShortDate(record.date), color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp, maxLines = 1)
                                                    }
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
            val workRecords = records.filter { it.workId == work.id }
            val lastRecord = workRecords.maxByOrNull { it.mileage }
            
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
