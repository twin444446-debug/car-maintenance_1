package com.example.carmaintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppScreen() {
    var selectedStatusFilter by remember { mutableStateOf<WorkStatus?>(null) }
    val context = LocalContext.current
    var cars by remember { mutableStateOf(loadCars(context)) }
    var templates by remember { mutableStateOf(loadTemplates(context).ifEmpty { defaultTemplates().also { saveTemplates(context, it) } }) }
    var works by remember { mutableStateOf(loadWorks(context)) }
    var records by remember { mutableStateOf(loadServiceRecords(context)) }
    var parts by remember { mutableStateOf(loadPartCatalog(context)) }
    var selectedCarId by remember {
        mutableStateOf(
            context.getSharedPreferences("car_maintenance", android.content.Context.MODE_PRIVATE)
                .getString("selected_car_id", null)
                ?.takeIf { id -> cars.any { it.id == id } } ?: cars.firstOrNull()?.id
        )
    }
    var screen by remember { mutableStateOf("Главная") }
    var carDialog by remember { mutableStateOf(false) }
    var editingCar by remember { mutableStateOf<Car?>(null) }
    var mileageDialog by remember { mutableStateOf(false) }
    var serviceDialog by remember { mutableStateOf(false) }
    var serviceInitial by remember { mutableStateOf<ServiceRecord?>(null) }
    var suggestedWork by remember { mutableStateOf("") }
    var actionWork by remember { mutableStateOf<MaintenanceWork?>(null) }
    var regulationDialog by remember { mutableStateOf(false) }
    var editingRegulation by remember { mutableStateOf<MaintenanceWork?>(null) }
    var templateEditor by remember { mutableStateOf(false) }
    var errorDecoder by remember { mutableStateOf(false) }
    var selectedRecord by remember { mutableStateOf<ServiceRecord?>(null) }
    var partsScreen by remember { mutableStateOf(false) }

    val car = cars.firstOrNull { it.id == selectedCarId }

    fun saveAndRefreshWorks(newWorks: List<MaintenanceWork>) {
        works = newWorks
        saveWorks(context, works)
    }

    fun createDefaultForCar(c: Car) {
        val source = templates.filter {
            it.category == WorkCategory.GENERAL || (it.category == WorkCategory.LPG && c.hasLpg)
        }
        val existingForCar = works.filter { it.carId == c.id }
        val existingTemplateIds = existingForCar.map { it.templateId }.filter { it.isNotBlank() }.toSet()
        val add = source.filter { template ->
            template.id !in existingTemplateIds && existingForCar.none { work ->
                work.name.equals(template.name, ignoreCase = true) && work.category == template.category
            }
        }.map { template ->
            MaintenanceWork(
                carId = c.id,
                templateId = template.id,
                name = template.name,
                category = template.category,
                intervalKm = template.intervalKm,
                intervalMonths = template.intervalMonths,
                warningKm = template.warningKm,
                warningMonths = template.warningMonths,
                preventive = template.preventive,
                notes = template.notes
            )
        }
        saveAndRefreshWorks(works + add)
    }

    fun completeWork(
    w: MaintenanceWork, 
    km: Int, 
    date: Long, 
    note: String,
    lines: List<ServiceLine> // <--- Здесь должен быть List<ServiceLine>, а не String!
) {
    val record = ServiceRecord(
        carId = w.carId, 
        date = date, 
        mileage = km, 
        reason = note,
        lines = lines, // <--- И здесь передаем lines
        notes = note
    )
    
    records = records + record
    saveServiceRecords(context, records)
    
    saveAndRefreshWorks(works.map {
        if (it.id == w.id) it.copy(lastMileage = km, lastDate = date, hasLastExecution = true, skipped = false)
        else it
    })
}

    Scaffold(containerColor = BgColor, bottomBar = { BottomNav(screen) { screen = it } }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when {
                templateEditor -> TemplateEditorScreen(
                    templates = templates,
                    onBack = { templateEditor = false },
                    onSave = { templates = it; saveTemplates(context, templates) }
                )
                errorDecoder -> ErrorDecoderScreen(onBack = { errorDecoder = false })
                partsScreen -> PartsCatalogScreen(
                    car, parts,
                    onBack = { partsScreen = false },
                    onAdd = { p -> parts = parts + p; savePartCatalog(context, parts) }
                )
                else -> when (screen) {
                    "Главная" -> HomeScreenRedesign(
    car, cars, works, records,
    onCarSelect = {
        selectedCarId = it
        context.getSharedPreferences("car_maintenance", android.content.Context.MODE_PRIVATE)
            .edit().putString("selected_car_id", it).apply()
    },
    onAddCar = { editingCar = null; carDialog = true },
    onAddWork = { suggestedWork = ""; serviceInitial = null; serviceDialog = true },
    onWork = { actionWork = it },
    onMileage = { mileageDialog = true },
    onOpenCar = { screen = "Автомобиль" },
    onStatusClick = { status ->
        selectedStatusFilter = status
        screen = "Перечень работ"
    }
)
                    "Перечень работ" -> WorkListScreen(
    car, records, parts,
    filter = selectedStatusFilter,
    onClearFilter = { selectedStatusFilter = null },
    onAdd = { suggestedWork = ""; serviceInitial = null; serviceDialog = true },
    onEdit = { selectedRecord = it; serviceInitial = it; serviceDialog = true },
    onPartSave = { parts = parts + it; savePartCatalog(context, parts) },
    works = works,
    onWorkClick = { actionWork = it }
)
                    "Регламент" -> RegulationScreen(
                        car, works.filter { it.carId == car?.id }, records,
                        onWork = { actionWork = it },
                        onAdd = { editingRegulation = null; regulationDialog = true },
                        onFill = { if (car != null) createDefaultForCar(car) }
                    )
                    "Статистика" -> StatisticsScreen(car, records)
                    "Ещё" -> MoreScreen(
                        onTemplate = { templateEditor = true },
                        onDecoder = { errorDecoder = true },
                        onParts = { partsScreen = true }
                    )
                    "Автомобиль" -> CarDetailScreen(
                        car,
                        onBack = { screen = "Главная" },
                        onEdit = { editingCar = car; carDialog = true },
                        onMileage = { mileageDialog = true },
                        onFill = { if (car != null) createDefaultForCar(car) },
                        onDelete = {
                            if (car != null) {
                                cars = cars.filter { it.id != car.id }
                                saveCars(context, cars)
                                selectedCarId = cars.firstOrNull()?.id
                                context.getSharedPreferences("car_maintenance", android.content.Context.MODE_PRIVATE)
                                    .edit().putString("selected_car_id", selectedCarId ?: "").apply()
                                screen = "Главная"
                            }
                        }
                    )
                }
            }
        }
    }

    if (carDialog) {
        AddCarDialog(editingCar, { carDialog = false }) { newCar ->
            cars = if (cars.any { it.id == newCar.id }) {
                cars.map { if (it.id == newCar.id) newCar else it }
            } else cars + newCar
            saveCars(context, cars)
            selectedCarId = newCar.id
            context.getSharedPreferences("car_maintenance", android.content.Context.MODE_PRIVATE)
                .edit().putString("selected_car_id", newCar.id).apply()
            carDialog = false
            editingCar = null
            if (works.none { it.carId == newCar.id }) createDefaultForCar(newCar)
        }
    }

    if (mileageDialog && car != null) {
        MileageDialog(car.mileage, { mileageDialog = false }) { value ->
            cars = cars.map { if (it.id == car.id) it.copy(mileage = value) else it }
            saveCars(context, cars)
            mileageDialog = false
        }
    }

    if (serviceDialog && car != null) {
        ServiceRecordDialog(car, serviceInitial, suggestedWork, parts, {
            serviceDialog = false
            serviceInitial = null
        }) { record ->
            records = if (records.any { it.id == record.id }) {
                records.map { if (it.id == record.id) record else it }
            } else records + record
            saveServiceRecords(context, records)
            serviceDialog = false
            serviceInitial = null
        }
    }

    if (actionWork != null && car != null) {
        val currentWork = actionWork!!
        WorkActionsDialog(
            currentWork, car, records,
            onDismiss = { actionWork = null },
            onComplete = { km, date, note, lines -> 
    completeWork(currentWork, km, date, note, lines)
    actionWork = null 
},
            onSkip = {
                saveAndRefreshWorks(works.map { if (it.id == currentWork.id) it.copy(skipped = true) else it })
                actionWork = null
            },
            onEdit = { editingRegulation = currentWork; actionWork = null; regulationDialog = true },
            onDelete = {
                saveAndRefreshWorks(works.filter { it.id != currentWork.id })
                actionWork = null
            },
            onShowInList = { screen = "Перечень работ"; actionWork = null }
        )
    }

    if (regulationDialog && car != null) {
        RegulationWorkDialog(car.id, editingRegulation, templates, {
            regulationDialog = false
            editingRegulation = null
        }) { work ->
            saveAndRefreshWorks(
                if (works.any { it.id == work.id }) works.map { if (it.id == work.id) work else it }
                else works + work
            )
            regulationDialog = false
            editingRegulation = null
        }
    }
}

@Composable
private fun BottomNav(screen: String, onSelect: (String) -> Unit) {
    NavigationBar(containerColor = NavColor) {
        listOf(
            "Главная" to Icons.Default.Home,
            "Перечень работ" to Icons.Default.List,
            "Регламент" to Icons.Default.Build,
            "Статистика" to Icons.Default.Info,
            "Ещё" to Icons.Default.MoreVert
        ).forEach { (title, icon) ->
            NavigationBarItem(
                selected = screen == title,
                onClick = { onSelect(title) },
                icon = { Icon(imageVector = icon, contentDescription = title) },
                label = { Text(title, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ColorGreen,
                    selectedTextColor = ColorGreen,
                    indicatorColor = ColorGreen.copy(alpha = .1f),
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}

@Composable
private fun Page(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(title, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun HomeScreen(
    car: Car?, cars: List<Car>, works: List<MaintenanceWork>, records: List<ServiceRecord>,
    onCarSelect: (String) -> Unit, onAddCar: () -> Unit, onAddWork: () -> Unit,
    onWork: (MaintenanceWork) -> Unit, onMileage: () -> Unit, onOpenCar: () -> Unit
) {
    if (car == null) {
        Page("Учёт автомобильного ТО") { Button(onClick = onAddCar) { Text("Добавить автомобиль") } }
        return
    }
    val carWorks = works.filter { it.carId == car.id }
    val statuses = carWorks.map { getWorkStatus(it, car, records) }
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                Modifier.fillMaxWidth().clickable { onOpenCar() },
                colors = CardDefaults.cardColors(containerColor = CardColor),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("${car.brand} ${car.model}", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("${car.year.ifBlank { "—" }} • ${car.engine.ifBlank { "двигатель не указан" }}", color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Text("${car.mileage.ifBlank { "0" }} км", color = ColorBlue, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("VIN: ${car.vin.ifBlank { "не указан" }}", color = TextSecondary)
                    Text("Топливо: ${car.fuel.ifBlank { "не указано" }}${if (car.hasLpg) " • ГБО" else ""}", color = TextSecondary)
                    TextButton(onClick = onMileage) { Text("Изменить пробег", color = ColorYellow) }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBlock("Выполнено", statuses.count { it == WorkStatus.COMPLETED }, ColorGreen, Modifier.weight(1f))
                StatusBlock("Необходимо", statuses.count { it == WorkStatus.REQUIRED }, ColorRed, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBlock("Рекомендации", statuses.count { it == WorkStatus.PREVENTIVE }, ColorYellow, Modifier.weight(1f))
                StatusBlock("Пропущено", statuses.count { it == WorkStatus.SKIPPED }, ColorPurple, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAction("Добавить запись", { Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = ColorGreen) }, ColorGreen, onAddWork, Modifier.weight(1f))
                QuickAction("Автомобиль", { Icon(imageVector = Icons.Default.Build, contentDescription = null, tint = ColorBlue) }, ColorBlue, onOpenCar, Modifier.weight(1f))
            }
        }
        item { SectionTitle("Ближайшие работы") }
        items(carWorks.sortedBy {
            when (getWorkStatus(it, car, records)) {
                WorkStatus.REQUIRED -> 0
                WorkStatus.SKIPPED -> 1
                WorkStatus.PREVENTIVE -> 2
                else -> 3
            }
        }.take(8)) { work ->
            WorkCard(work, car, records) { onWork(work) }
        }
    }
}

@Composable
private fun WorkListScreen(
    car: Car?, 
    records: List<ServiceRecord>, 
    parts: List<PartCatalogItem>,
    filter: WorkStatus? = null,
    onClearFilter: () -> Unit,
    onAdd: () -> Unit, 
    onEdit: (ServiceRecord) -> Unit, 
    onPartSave: (PartCatalogItem) -> Unit,
    works: List<MaintenanceWork>,
    onWorkClick: (MaintenanceWork) -> Unit
) {
    if (car == null) {
        Page("Перечень работ") { Text("Сначала добавьте автомобиль", color = TextSecondary) }
        return
    }
    
    Page("Перечень работ") {
        // Плашка активного фильтра
        if (filter != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Фильтр: ${filter.title()}", color = ColorBlue, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClearFilter) { Text("Сбросить", color = ColorRed) }
            }
            Spacer(Modifier.height(4.dp))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAdd, modifier = Modifier.weight(1f)) { Text("Добавить запись") }
            Text("${records.count { it.carId == car.id }} записей", color = TextSecondary, modifier = Modifier.padding(12.dp))
        }
        Spacer(Modifier.height(8.dp))
        
        // Если фильтр активен — показываем работы, иначе — историю
        if (filter != null) {
            val filteredWorks = works.filter { it.carId == car.id && getWorkStatus(it, car, records) == filter }
            
            if (filteredWorks.isEmpty()) {
                Text("Нет работ с этим статусом", color = TextSecondary)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredWorks) { work ->
                        WorkCard(work, car, records) { onWorkClick(work) }
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(records.filter { it.carId == car.id }.sortedByDescending { it.date }) { record ->
                    Card(
                        Modifier.fillMaxWidth().clickable { onEdit(record) },
                        colors = CardDefaults.cardColors(containerColor = CardColor)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(formatDate(record.date), color = ColorBlue)
                            Text(record.servicePlace.ifBlank { "Самостоятельно" }, color = TextSecondary)
                            Text(record.reason.ifBlank { "Обслуживание" }, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("Пробег: ${record.mileage} км", color = TextSecondary)
                            record.lines.forEach { line ->
                                Text(
                                    "• ${line.name}${if (line.article.isNotBlank()) " [${line.article}]" else ""} — ${formatMoney(line.cost)} ${line.currency}",
                                    color = TextPrimary, fontSize = 13.sp
                                )
                            }
                            Text("Итого: ${formatTotals(record)}", color = ColorGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RegulationScreen(
    car: Car?, works: List<MaintenanceWork>, records: List<ServiceRecord>,
    onWork: (MaintenanceWork) -> Unit, onAdd: () -> Unit, onFill: () -> Unit
) {
    if (car == null) {
        Page("Регламент") { Text("Сначала добавьте автомобиль", color = TextSecondary) }
        return
    }
    val generalWorks = works.filter { it.category == WorkCategory.GENERAL }
    val lpgWorks = works.filter { it.category == WorkCategory.LPG }
    Page("Регламент") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onFill, modifier = Modifier.weight(1f)) { Text("Заполнить из шаблона") }
            Button(onClick = onAdd, modifier = Modifier.weight(1f)) { Text("Добавить") }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { SectionTitle("Обслуживание автомобиля") }
            items(generalWorks) { work -> WorkCard(work, car, records) { onWork(work) } }
            if (car.hasLpg) {
                item { SectionTitle("Регламент ГБО") }
                items(lpgWorks) { work -> WorkCard(work, car, records) { onWork(work) } }
            }
        }
    }
}

@Composable
private fun StatisticsScreen(car: Car?, records: List<ServiceRecord>) {
    if (car == null) {
        Page("Статистика") { Text("Сначала добавьте автомобиль", color = TextSecondary) }
        return
    }
    val carRecords = records.filter { it.carId == car.id }
    val totals = carRecords.flatMap { it.lines }.groupBy { it.currency }.mapValues { (_, lines) -> lines.sumOf { it.cost } }
    Page("Статистика") {
        StatusBlock("Записей обслуживания", carRecords.size, ColorBlue, Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        SectionTitle("Расходы по валютам")
        totals.forEach { (currency, value) -> InfoRow(currency, formatMoney(value)) }
        Spacer(Modifier.height(12.dp))
        SectionTitle("Работы")
        InfoRow("Всего строк", carRecords.sumOf { it.lines.size }.toString())
        InfoRow("Последнее обслуживание", formatDate(carRecords.maxOfOrNull { it.date } ?: 0L))
        Text(
            "Расход топлива не рассчитывается: для автомобиля с бензином и ГБО такой показатель без дополнительных данных может быть некорректным.",
            color = TextSecondary, fontSize = 12.sp
        )
    }
}

@Composable
private fun MoreScreen(onTemplate: () -> Unit, onDecoder: () -> Unit, onParts: () -> Unit) {
    Page("Ещё") {
        Button(onClick = onTemplate, modifier = Modifier.fillMaxWidth()) { Text("Шаблоны регламента") }
        Button(onClick = onDecoder, modifier = Modifier.fillMaxWidth()) { Text("Расшифровка кодов ошибок") }
        Button(onClick = onParts, modifier = Modifier.fillMaxWidth()) { Text("Запчасти и расходники") }
        Text("Шаблоны общие, а интервалы, статусы и история — отдельно для каждого автомобиля.", color = TextSecondary)
    }
}

@Composable
private fun CarDetailScreen(
    car: Car?, onBack: () -> Unit, onEdit: () -> Unit, onMileage: () -> Unit,
    onFill: () -> Unit, onDelete: () -> Unit
) {
    if (car == null) {
        onBack()
        return
    }
    Page("Автомобиль") {
        TextButton(onClick = onBack) { Text("← Назад", color = ColorBlue) }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardColor)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${car.brand} ${car.model}", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                InfoRow("Год", car.year)
                InfoRow("Пробег", "${car.mileage} км")
                InfoRow("VIN", car.vin)
                InfoRow("Двигатель", car.engine)
                InfoRow("Топливо", car.fuel)
                InfoRow("ГБО", if (car.hasLpg) "Установлено" else "Нет")
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onMileage, modifier = Modifier.fillMaxWidth()) { Text("Изменить пробег") }
        Button(onClick = onFill, modifier = Modifier.fillMaxWidth()) { Text("Заполнить регламент") }
        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Редактировать") }
        TextButton(onClick = onDelete) { Text("Удалить автомобиль", color = ColorRed) }
    }
}

@Composable
private fun PartsCatalogScreen(
    car: Car?, parts: List<PartCatalogItem>, onBack: () -> Unit, onAdd: (PartCatalogItem) -> Unit
) {
    if (car == null) {
        Page("Запчасти и расходники") { Text("Сначала добавьте автомобиль", color = TextSecondary) }
        return
    }
    var show by remember { mutableStateOf(false) }
    Page("Запчасти и расходники") {
        TextButton(onClick = onBack) { Text("← Назад", color = ColorBlue) }
        Text("Каталог создаётся вручную для этого автомобиля. Встроенной базы рекомендаций нет.", color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Button(onClick = { show = true }, modifier = Modifier.fillMaxWidth()) { Text("Добавить запчасть / расходник") }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(parts.filter { it.carId == car.id }) { part ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardColor)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(part.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Артикул: ${part.article.ifBlank { "—" }}", color = TextSecondary)
                        Text("Последняя цена: ${formatMoney(part.lastCost)} ${part.currency}", color = TextSecondary)
                    }
                }
            }
        }
    }
    if (show) {
        PartCatalogDialog(car.id, onDismiss = { show = false }) { part ->
            onAdd(part)
            show = false
        }
    }
}

@Composable
private fun PartCatalogDialog(carId: String, onDismiss: () -> Unit, onSave: (PartCatalogItem) -> Unit) {
    var name by remember { mutableStateOf("") }
    var article by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Запчасть") }
    var cost by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("EUR") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardColor,
        title = { Text("Новая запчасть", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") })
                OutlinedTextField(value = article, onValueChange = { article = it }, label = { Text("Артикул") })
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Категория") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cost, onValueChange = { cost = it }, label = { Text("Цена") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = currency, onValueChange = { currency = it.uppercase() },
                        label = { Text("Валюта") }, modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onSave(
                        PartCatalogItem(
                            carId = carId, name = name, article = article, category = category,
                            lastCost = cost.toDoubleOrNull() ?: 0.0, currency = currency.uppercase()
                        )
                    )
                }
            }) { Text("Сохранить", color = ColorGreen) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = TextSecondary) }
        }
    )
}

@Composable
private fun ErrorDecoderScreen(onBack: () -> Unit) {
    var code by remember { mutableStateOf("") }
    Page("Расшифровка кодов ошибок") {
        TextButton(onClick = onBack) { Text("← Назад", color = ColorBlue) }
        Text("Введите код OBD-II, например P0300, чтобы получить расшифровку.", color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = code, onValueChange = { code = it.uppercase() },
            label = { Text("Код ошибки") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        if (code.isNotBlank()) Text(obdMeaning(code), color = TextPrimary)
    }
}

private fun obdMeaning(code: String): String = when (code.uppercase()) {
    "P0300" -> "P0300 — случайные/множественные пропуски воспламенения."
    "P0301" -> "P0301 — пропуски воспламенения в цилиндре 1."
    "P0302" -> "P0302 — пропуски воспламенения в цилиндре 2."
    "P0171" -> "P0171 — слишком бедная смесь."
    "P0172" -> "P0172 — слишком богатая смесь."
    "P0420" -> "P0420 — эффективность системы нейтрализации ниже порога."
    else -> "Код $code: расшифровка пока отсутствует в локальном справочнике. Можно добавить её позже."
}

// Вспомогательная функция для красивого названия статуса
private fun WorkStatus.title(): String = when(this) {
    WorkStatus.COMPLETED -> "Выполнено"
    WorkStatus.REQUIRED -> "Необходимо"
    WorkStatus.PREVENTIVE -> "Рекомендации"
    WorkStatus.SKIPPED -> "Пропущено"
}
