package com.example.carmaintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    onOpenCar: () -> Unit
) {
    if (car == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgColor),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardColor
                ),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Учёт автомобильного ТО",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        "Добавьте автомобиль, чтобы начать учёт обслуживания.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )

                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = onAddCar,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorGreen
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Добавить автомобиль")
                    }
                }
            }
        }

        return
    }

    val carWorks = works.filter { it.carId == car.id }

    val statuses = carWorks.map {
        getWorkStatus(it, car, records)
    }

    val requiredCount = statuses.count {
        it == WorkStatus.REQUIRED
    }

    val completedCount = statuses.count {
        it == WorkStatus.COMPLETED
    }

    val preventiveCount = statuses.count {
        it == WorkStatus.PREVENTIVE
    }

    val skippedCount = statuses.count {
        it == WorkStatus.SKIPPED
    }

    val nearestWorks = carWorks
        .sortedBy {
            when (getWorkStatus(it, car, records)) {
                WorkStatus.REQUIRED -> 0
                WorkStatus.SKIPPED -> 1
                WorkStatus.PREVENTIVE -> 2
                WorkStatus.COMPLETED -> 3
                else -> 4
            }
        }
        .take(6)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor),
        contentPadding = PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = 14.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // Заголовок и добавление автомобиля
item {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Главная",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        TextButton(onClick = onAddCar) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = ColorGreen
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "Добавить авто",
                color = ColorGreen
            )
        }
    }
}

        // Карточка автомобиля
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenCar() },
                colors = CardDefaults.cardColors(
                    containerColor = CardColor
                ),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "${car.brand} ${car.model}",
                                color = TextPrimary,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "${car.year.ifBlank { "Год не указан" }} • ${car.engine.ifBlank { "Двигатель не указан" }}",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }

                        IconButton(
                            onClick = onOpenCar
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Автомобиль",
                                tint = ColorBlue
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "ТЕКУЩИЙ ПРОБЕГ",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "${car.mileage.ifBlank { "0" }} км",
                                color = ColorBlue,
                                fontSize = 27.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(
                            onClick = onMileage
                        ) {
                            Text(
                                "Изменить",
                                color = ColorYellow
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    HorizontalDivider(
                        color = TextSecondary.copy(alpha = 0.15f)
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        "VIN: ${car.vin.ifBlank { "не указан" }}",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        "Топливо: ${
                            car.fuel.ifBlank { "не указано" }
                        }${
                            if (car.hasLpg) " • ГБО установлено"
                            else ""
                        }",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Главный статус
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (requiredCount > 0) {
                        ColorRed.copy(alpha = 0.16f)
                    } else {
                        ColorGreen.copy(alpha = 0.12f)
                    }
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (requiredCount > 0)
                                "Требуется обслуживание"
                            else
                                "Критичных работ нет",
                            color = if (requiredCount > 0)
                                ColorRed
                            else
                                ColorGreen,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            if (requiredCount > 0)
                                "Есть работы, которые необходимо выполнить."
                            else
                                "Следите за ближайшими регламентными работами.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Text(
                        requiredCount.toString(),
                        color = if (requiredCount > 0)
                            ColorRed
                        else
                            ColorGreen,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Статусы
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusBlock(
                    "Выполнено",
                    completedCount,
                    ColorGreen,
                    Modifier.weight(1f)
                )

                StatusBlock(
                    "Необходимо",
                    requiredCount,
                    ColorRed,
                    Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusBlock(
                    "Рекомендации",
                    preventiveCount,
                    ColorYellow,
                    Modifier.weight(1f)
                )

                StatusBlock(
                    "Пропущено",
                    skippedCount,
                    ColorPurple,
                    Modifier.weight(1f)
                )
            }
        }

        // Быстрые действия
        item {
            Text(
                "Быстрые действия",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(
                    start = 4.dp,
                    top = 4.dp
                )
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickAction(
                    "Добавить запись",
                    {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = ColorGreen
                        )
                    },
                    ColorGreen,
                    onAddWork,
                    Modifier.weight(1f)
                )

                QuickAction(
                    "Автомобиль",
                    {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = null,
                            tint = ColorBlue
                        )
                    },
                    ColorBlue,
                    onOpenCar,
                    Modifier.weight(1f)
                )
            }
        }

        // Ближайшие работы
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 4.dp,
                        top = 6.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Ближайшие работы",
                    color = TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    "${carWorks.size} всего",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        if (nearestWorks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = CardColor
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            "Регламент пока не заполнен",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(5.dp))

                        Text(
                            "Добавьте работы в разделе «Регламент».",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(nearestWorks) { work ->
                WorkCard(
                    work,
                    car,
                    records
                ) {
                    onWork(work)
                }
            }
        }
    }
}
