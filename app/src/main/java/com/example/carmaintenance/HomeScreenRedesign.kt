package com.example.carmaintenance

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreenRedesign(
    car: Car?,
    cars: List<Car>,
    works: List<MaintenanceWork>,
    records: List<ServiceRecord>,
    onCarSelect: (String) -> Unit,
    onAddCar: () -> Unit,
    onAddWork: () -> Unit,
    onWork: (MaintenanceWork) -> Unit,
    onMileage: () -> Unit,
    onOpenCar: () -> Unit,
    onEditCar: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onStatusClick: (WorkStatus) -> Unit
) {
    // Состояние для увеличенного фото (по индексу автомобиля)
    var expandedImageIndex by remember { mutableStateOf(-1) }

    if (car == null) {
        Box(modifier = Modifier.fillMaxSize().background(BgColor), contentAlignment = Alignment.Center) {
            Card(modifier = Modifier.fillMaxWidth().padding(20.dp), colors = CardDefaults.cardColors(containerColor = CardColor), shape = RoundedCornerShape(22.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Учёт автомобильного ТО", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text("Добавьте автомобиль, чтобы начать учёт обслуживания.", color = TextSecondary, textAlign = TextAlign.Center, fontSize = 14.sp)
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = onAddCar, colors = ButtonDefaults.buttonColors(containerColor = ColorGreen)) {
                        Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Добавить автомобиль")
                    }
                }
            }
        }
        return
    }

    val carWorks = works.filter { it.carId == car.id }
    val statuses = carWorks.map { getWorkStatus(it, car, records) }
    val requiredCount = statuses.count { it == WorkStatus.REQUIRED }
    val completedCount = statuses.count { it == WorkStatus.COMPLETED }
    val preventiveCount = statuses.count { it == WorkStatus.PREVENTIVE }
    val skippedCount = statuses.count { it == WorkStatus.SKIPPED }

    val nearestWorks = carWorks.sortedBy {
        when (getWorkStatus(it, car, records)) {
            WorkStatus.REQUIRED -> 0; WorkStatus.SKIPPED -> 1; WorkStatus.PREVENTIVE -> 2; WorkStatus.COMPLETED -> 3; else -> 4
        }
    }.take(6)

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgColor),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "home_header") {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenCar) { Icon(Icons.Default.Menu, contentDescription = "Меню", tint = TextPrimary) }
                Column(Modifier.weight(1f)) {
                    Text("Главная", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Состояние автомобиля и обслуживание", color = TextSecondary, fontSize = 11.sp)
                }
                IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = "Настройки", tint = TextPrimary) }
            }
        }

        item(key = "car_pager_section") {
            val initialIndex = cars.indexOfFirst { it.id == car.id }.coerceAtLeast(0)
            val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { cars.size })

            LaunchedEffect(car.id) {
                val newIndex = cars.indexOfFirst { it.id == car.id }
                if (newIndex >= 0 && pagerState.currentPage != newIndex) pagerState.scrollToPage(newIndex)
            }

            LaunchedEffect(pagerState.currentPage) {
                val selectedCar = cars[pagerState.currentPage]
                if (selectedCar.id != car.id) onCarSelect(selectedCar.id)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HorizontalPager(
                    state = pagerState, contentPadding = PaddingValues(horizontal = 14.dp), pageSpacing = 10.dp, modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val currentCar = cars[page]
                    val isImageExpanded = expandedImageIndex == page
                    val imageHeight = if (isImageExpanded) 280.dp else 130.dp
                    val imageContentScale = if (isImageExpanded) ContentScale.Fit else ContentScale.Crop

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardColor), shape = RoundedCornerShape(22.dp)) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(imageHeight)
                                    .background(Color(0xFF1E293B))
                                    .animateContentSize(animationSpec = tween(300)) // Плавная анимация высоты
                                    .clickable { 
                                        expandedImageIndex = if (isImageExpanded) -1 else page 
                                    } // Клик сворачивает/разворачивает
                            ) {
                                if (currentCar.imageUri.isNotBlank()) {
                                    AsyncImage(
                                        model = currentCar.imageUri,
                                        contentDescription = "Фото ${currentCar.brand} ${currentCar.model}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = imageContentScale // Crop для маленького, Fit для большого
                                    )
                                } else {
                                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color.White.copy(alpha = 0.1f), modifier = Modifier.align(Alignment.Center).size(72.dp))
                                }
                                IconButton(
                                    onClick = onEditCar,
                                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = Color.White)
                                }
                            }
                            Column(Modifier.padding(16.dp)) {
                                Text("${currentCar.brand} ${currentCar.model}", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${currentCar.year.ifBlank { "Год не указан" }} • ${currentCar.engine.ifBlank { "Двигатель не указан" }}", color = TextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(12.dp)); HorizontalDivider(color = TextSecondary.copy(alpha = 0.18f)); Spacer(Modifier.height(10.dp))
                                Text("ТЕКУЩИЙ ПРОБЕГ", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${currentCar.mileage.ifBlank { "0" }} км", color = ColorBlue, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    TextButton(onClick = onMileage) { Text("Изменить", color = ColorYellow) }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column { Text("VIN", color = TextSecondary, fontSize = 11.sp); Text(currentCar.vin.ifBlank { "не указан" }, color = TextPrimary, fontSize = 13.sp) }
                                    Column(horizontalAlignment = Alignment.End) { Text("Топливо", color = TextSecondary, fontSize = 11.sp); Text("${currentCar.fuel.ifBlank { "не указано" }}${if (currentCar.hasLpg) " • ГБО" else ""}", color = TextPrimary, fontSize = 13.sp) }
                                }
                            }
                        }
                    }
                }
                if (cars.size > 1) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        cars.forEachIndexed { index, _ ->
                            Box(Modifier.padding(horizontal = 4.dp).size(if (pagerState.currentPage == index) 10.dp else 7.dp)
                                .background(if (pagerState.currentPage == index) ColorBlue else TextSecondary.copy(alpha = 0.45f), RoundedCornerShape(50)))
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text("Автомобиль ${pagerState.currentPage + 1} из ${cars.size}", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }

        item(key = "stats_grid") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCardCompact(title = "Выполнено", count = completedCount, color = ColorGreen, modifier = Modifier.weight(1f).clickable { onStatusClick(WorkStatus.COMPLETED) })
                    StatCardCompact(title = "Необходимо", count = requiredCount, color = ColorRed, modifier = Modifier.weight(1f).clickable { onStatusClick(WorkStatus.REQUIRED) })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCardCompact(title = "Рекомендации", count = preventiveCount, color = ColorYellow, modifier = Modifier.weight(1f).clickable { onStatusClick(WorkStatus.PREVENTIVE) })
                    StatCardCompact(title = "Пропущено", count = skippedCount, color = ColorPurple, modifier = Modifier.weight(1f).clickable { onStatusClick(WorkStatus.SKIPPED) })
                }
            }
        }

        item(key = "add_record_button") {
            Button(
                onClick = onAddWork,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorGreen),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Добавить работу", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        item(key = "nearest_works_header") {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Ближайшие работы", color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${carWorks.size} всего", color = TextSecondary, fontSize = 12.sp)
            }
        }

        if (nearestWorks.isEmpty()) {
            item(key = "empty_works") {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardColor), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Регламент пока не заполнен", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text("Добавьте работы в разделе «Регламент».", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(nearestWorks, key = { it.id }) { work -> WorkCard(work, car, records) { onWork(work) } }
        }
    }
}

@Composable
fun StatCardCompact(title: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(56.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text("$count", color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}
