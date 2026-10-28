package com.example.carmaintenance

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

fun copyImageToInternalStorage(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "car_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun AppScreen() {
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
    var selectedStatusFilter by remember { mutableStateOf<WorkStatus?>(null) }
    
    var selectedCompletedWork by remember { mutableStateOf<Pair<MaintenanceWork, ServiceRecord>?>(null) }
    var workToDelete by remember { mutableStateOf<Pair<MaintenanceWork, ServiceRecord>?>(null) }
    var carToDelete by remember { mutableStateOf<Car?>(null) }
    var recordToDelete by remember { mutableStateOf<ServiceRecord?>(null) }
    var editingCompletedWork by remember { mutableStateOf<Pair<MaintenanceWork, ServiceRecord>?>(null) }
    
    var showAboutDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var carsMenuExpanded by remember { mutableStateOf(false) }

    val car = cars.firstOrNull { it.id == selectedCarId }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun recalculateWorkAfterRecordRemoval(
        work: MaintenanceWork,
        remainingRecords: List<ServiceRecord>
    ): MaintenanceWork {
        val workRecords = remainingRecords
            .filter { it.workId == work.id }
            .sortedByDescending { it.date }

        val last = workRecords.firstOrNull()
        return if (last == null) {
            work.copy(
                hasLastExecution = false,
                lastMileage = 0,
                lastDate = 0L,
                skipped = false
            )
        } else {
            work.copy(
                hasLastExecution = true,
                lastMileage = last.mileage,
                lastDate = last.date,
                skipped = false
            )
        }
    }

    LaunchedEffect(works, records) {
        val updatedWorks = works.map { work ->
            val workRecords = records.filter { it.workId == work.id }
            when {
                workRecords.isEmpty() && (work.hasLastExecution || work.lastMileage != 0 || work.lastDate != 0L) ->
                    work.copy(hasLastExecution = false, lastMileage = 0, lastDate = 0L)
                workRecords.isNotEmpty() -> {
                    val last = workRecords.maxByOrNull { it.date }!!
                    if (!work.hasLastExecution || work.lastMileage != last.mileage || work.lastDate != last.date) {
                        work.copy(hasLastExecution = true, lastMileage = last.mileage, lastDate = last.date)
                    } else work
                }
                else -> work
            }
        }
        if (updatedWorks != works) {
            works = updatedWorks
            saveWorks(context, works)
        }
    }

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
                carId = c.id, templateId = template.id, name = template.name, category = template.category,
                intervalKm = template.intervalKm, intervalMonths = template.intervalMonths,
                warningKm = template.warningKm, warningMonths = template.warningMonths,
                preventive = template.preventive, notes = template.notes
            )
        }
        saveAndRefreshWorks(works + add)
    }

    fun completeWork(
        w: MaintenanceWork, km: Int, date: Long, note: String, lines: List<ServiceLine>
    ) {
        val record = ServiceRecord(
            carId = w.carId, 
            workId = w.id,
            date = date, mileage = km, reason = note, lines = lines, notes = note
        )
        records = records + record
        saveServiceRecords(context, records)
        saveAndRefreshWorks(works.map {
            if (it.id == w.id) it.copy(lastMileage = km, lastDate = date, hasLastExecution = true, skipped = false) else it
        })
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = CardColor, drawerContentColor = TextPrimary) {
                Spacer(Modifier.height(24.dp))
                Text("Учёт автомобильного ТО", modifier = Modifier.padding(16.dp), color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))
                
                NavigationDrawerItem(
                    label = { Text("Мои автомобили", color = TextPrimary) },
                    icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = ColorBlue) },
                    selected = false,
                    onClick = { carsMenuExpanded = !carsMenuExpanded },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, selectedContainerColor = Color.Transparent),
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    badge = { Icon(if (carsMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextSecondary) }
                )
                
                if (carsMenuExpanded) {
                    cars.forEach { carItem ->
                        NavigationDrawerItem(
                            label = { Text("${carItem.brand} ${carItem.model}".ifBlank { "Автомобиль" }, color = if (carItem.id == selectedCarId) ColorGreen else TextPrimary, fontWeight = if (carItem.id == selectedCarId) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = if (carItem.id == selectedCarId) ColorGreen else TextSecondary) },
                            selected = carItem.id == selectedCarId,
                            onClick = {
                                selectedCarId = carItem.id
                                context.getSharedPreferences("car_maintenance", android.content.Context.MODE_PRIVATE).edit().putString("selected_car_id", carItem.id).apply()
                                screen = "Главная"
                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, selectedContainerColor = ColorGreen.copy(alpha = 0.1f)),
                            modifier = Modifier.padding(start = 32.dp, end = 16.dp, top = 2.dp, bottom = 2.dp)
                        )
                    }
                }

                DrawerItem("Добавить автомобиль", Icons.Default.Add, ColorGreen, false) {
                    editingCar = null; carDialog = true; scope.launch { drawerState.close() }
                }
                DrawerItem("Статистика", Icons.Default.Info, ColorYellow, screen == "Статистика") {
                    screen = "Статистика"; scope.launch { drawerState.close() }
                }
                DrawerItem("Шаблоны регламента", Icons.Default.List, ColorGreen, templateEditor) {
                    templateEditor = true; scope.launch { drawerState.close() }
                }
                DrawerItem("Запчасти и расходники", Icons.Default.Build, ColorBlue, partsScreen) {
                    partsScreen = true; scope.launch { drawerState.close() }
                }
                DrawerItem("Коды ошибок", Icons.Default.Search, ColorPurple, errorDecoder) {
                    errorDecoder = true; scope.launch { drawerState.close() }
                }
                HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
                DrawerItem("О приложении", Icons.Default.Info, ColorBlue, showAboutDialog) {
                    showAboutDialog = true; scope.launch { drawerState.close() }
                }
                DrawerItem("Настройки", Icons.Default.Settings, Color.Gray, showSettingsDialog) {
                    showSettingsDialog = true; scope.launch { drawerState.close() }
                }
                DrawerItem("Резервная копия", Icons.Default.CloudUpload, ColorGreen, showBackupDialog) {
                    showBackupDialog = true; scope.launch { drawerState.close() }
                }
            }
        }
    ) {
        Scaffold(containerColor = BgColor, bottomBar = { BottomNav(screen) { screen = it } }) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                when {
                    templateEditor -> TemplateEditorScreen(templates = templates, onBack = { templateEditor = false }, onSave = { templates = it; saveTemplates(context, templates) })
                    errorDecoder -> ErrorDecoderScreen(onBack = { errorDecoder = false })
                    partsScreen -> PartsCatalogScreen(car, parts, onBack = { partsScreen = false }, onAdd = { p -> parts = parts + p; savePartCatalog(context, parts) })
                    else -> when (screen) {
                        "Главная" -> HomeScreenRedesign(
                            car, cars, works, records,
                            onCarSelect = { selectedCarId = it; context.getSharedPreferences("car_maintenance", android.content.Context.MODE_PRIVATE).edit().putString("selected_car_id", it).apply() },
                            onAddCar = { editingCar = null; carDialog = true },
                            onAddWork = { suggestedWork = ""; serviceInitial = null; serviceDialog = true },
                            onWork = { actionWork = it },
                            onMileage = { mileageDialog = true },
                            onOpenCar = { scope.launch { drawerState.open() } },
                            onEditCar = { editingCar = car; carDialog = true },
                            onStatusClick = { status -> selectedStatusFilter = status; screen = "Регламент" }
                        )
                        "Перечень работ" -> WorkListScreen(
                            car, records, parts,
                            onAdd = { suggestedWork = ""; serviceInitial = null; serviceDialog = true },
                            onEdit = { selectedRecord = it; serviceInitial = it; serviceDialog = true },
                            onPartSave = { parts = parts + it; savePartCatalog(context, parts) },
                            works = works,
                            onWorkClick = { work, record -> selectedCompletedWork = work to record },
                            onDeleteRecord = { recordToDelete = it }
                        )
                        "Регламент" -> RegulationScreen(
                            car, 
                            works.filter { it.carId == car?.id }, 
                            records,
                            filter = selectedStatusFilter,
                            onFilterChange = { selectedStatusFilter = it },
                            onWork = { actionWork = it }, 
                            onAdd = { editingRegulation = null; regulationDialog = true }, 
                            onFill = { if (car != null) createDefaultForCar(car) }
                        )
                        "Статистика" -> StatisticsScreen(car, records)
                        "История" -> HistoryScreen(
                            car, records, works,
                            onEditRecord = { record ->
                                if (record.workId != null) {
                                    val work = works.firstOrNull { it.id == record.workId }
                                    if (work != null) editingCompletedWork = work to record
                                } else {
                                    selectedRecord = record; serviceInitial = record; serviceDialog = true
                                }
                            },
                            onDeleteRecord = { recordToDelete = it }
                        )
                        "Автомобиль" -> CarDetailScreen(
                            car,
                            onBack = { screen = "Главная" },
                            onEdit = { editingCar = car; carDialog = true },
                            onMileage = { mileageDialog = true },
                            onDelete = { carToDelete = car }
                        )
                    }
                }
            }
        }
    }

    // Диалоги
    if (carDialog) {
        AddCarDialog(editingCar, { carDialog = false }) { newCar ->
            cars = if (cars.any { it.id == newCar.id }) cars.map { if (it.id == newCar.id) newCar else it } else cars + newCar
            saveCars(context, cars)
            selectedCarId = newCar.id
            context.getSharedPreferences("car_maintenance", android.content.Context.MODE_PRIVATE).edit().putString("selected_car_id", newCar.id).apply()
            carDialog = false; editingCar = null
            if (works.none { it.carId == newCar.id }) createDefaultForCar(newCar)
        }
    }

    if (mileageDialog && car != null) {
        MileageDialog(car.mileage, { mileageDialog = false }) { value ->
            cars = cars.map { if (it.id == car.id) it.copy(mileage = value) else it }
            saveCars(context, cars); mileageDialog = false
        }
    }

    if (serviceDialog && car != null) {
        ServiceRecordDialog(car, serviceInitial, suggestedWork, parts, works, { serviceDialog = false; serviceInitial = null }) { record ->
            records = if (records.any { it.id == record.id }) records.map { if (it.id == record.id) record else it } else records + record
            saveServiceRecords(context, records); serviceDialog = false; serviceInitial = null
        }
    }

    if (actionWork != null && car != null) {
        val currentWork = actionWork!!
        key(car.mileage) {
            WorkActionsDialog(
                currentWork, car, records,
                onDismiss = { actionWork = null },
                onComplete = { km, date, note, lines -> completeWork(currentWork, km, date, note, lines); actionWork = null },
                onSkip = { saveAndRefreshWorks(works.map { if (it.id == currentWork.id) it.copy(skipped = true) else it }); actionWork = null },
                onEdit = { editingRegulation = currentWork; actionWork = null; regulationDialog = true },
                onDelete = { saveAndRefreshWorks(works.filter { it.id != currentWork.id }); actionWork = null },
                onShowInList = { screen = "Перечень работ"; actionWork = null },
                onEditRecord = { record -> editingCompletedWork = currentWork to record; actionWork = null },
                onCancelRecord = { record ->
                    val newRecords = records.filter { it.id != record.id }
                    records = newRecords
                    saveServiceRecords(context, newRecords)
                    saveAndRefreshWorks(works.map {
                        if (it.id == currentWork.id) recalculateWorkAfterRecordRemoval(currentWork, newRecords)
                        else it
                    })
                    actionWork = null
                }
            )
        }
    }

    if (editingCompletedWork != null && car != null) {
        val (work, record) = editingCompletedWork!!
        WorkActionsDialog(
            work = work,
            car = car,
            records = records,
            initialRecord = record,
            onDismiss = { editingCompletedWork = null },
            onComplete = { km, date, note, lines ->
                records = records.map {
                    if (it.id == record.id) it.copy(mileage = km, date = date, notes = note, lines = lines)
                    else it
                }
                saveServiceRecords(context, records)
                saveAndRefreshWorks(works.map {
                    if (it.id == work.id) it.copy(lastMileage = km, lastDate = date, hasLastExecution = true, skipped = false)
                    else it
                })
                editingCompletedWork = null
            },
            onSkip = { },
            onEdit = { },
            onDelete = { },
            onShowInList = { },
            onEditRecord = { rec ->
                editingCompletedWork = work to rec
            },
            onCancelRecord = { rec ->
                val newRecords = records.filter { it.id != rec.id }
                records = newRecords
                saveServiceRecords(context, newRecords)
                saveAndRefreshWorks(works.map {
                    if (it.id == work.id) recalculateWorkAfterRecordRemoval(work, newRecords)
                    else it
                })
                editingCompletedWork = null
            }
        )
    }

    if (regulationDialog && car != null) {
        RegulationWorkDialog(car.id, editingRegulation, templates, { regulationDialog = false; editingRegulation = null }) { work ->
            saveAndRefreshWorks(if (works.any { it.id == work.id }) works.map { if (it.id == work.id) work else it } else works + work)
            regulationDialog = false; editingRegulation = null
        }
    }

    if (selectedCompletedWork != null) {
        val (work, record) = selectedCompletedWork!!
        CompletedWorkInfoDialog(
            work = work, car = car!!, record = record,
            onDismiss = { selectedCompletedWork = null },
            onEdit = { editingCompletedWork = work to record; selectedCompletedWork = null },
            onDelete = { workToDelete = work to record; selectedCompletedWork = null },
            onCancelCompletion = {
                val newRecords = records.filter { it.id != record.id }
                records = newRecords
                saveServiceRecords(context, newRecords)
                saveAndRefreshWorks(works.map {
                    if (it.id == work.id) recalculateWorkAfterRecordRemoval(work, newRecords)
                    else it
                })
                selectedCompletedWork = null
            }
        )
    }

    if (workToDelete != null) {
        val (work, record) = workToDelete!!
        AlertDialog(
            onDismissRequest = { workToDelete = null },
            containerColor = CardColor,
            title = { Text("Удалить запись?", color = TextPrimary) },
            text = { Text("Вы уверены, что хотите удалить запись о выполнении работы \"${work.name}\"? Это действие нельзя отменить.", color = TextSecondary) },
            confirmButton = { 
                TextButton(onClick = { 
                    val newRecords = records.filter { it.id != record.id }
                    records = newRecords
                    saveServiceRecords(context, newRecords)
                    if (record.workId != null) {
                        val w = works.firstOrNull { it.id == record.workId }
                        if (w != null) {
                            saveAndRefreshWorks(works.map {
                                if (it.id == w.id) recalculateWorkAfterRecordRemoval(w, newRecords)
                                else it
                            })
                        }
                    }
                    workToDelete = null 
                }) { Text("Удалить", color = ColorRed, fontWeight = FontWeight.Bold) } 
            },
            dismissButton = { TextButton(onClick = { workToDelete = null }) { Text("Отмена", color = TextSecondary) } }
        )
    }

    if (recordToDelete != null) {
        val record = recordToDelete!!
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            containerColor = CardColor,
            title = { Text("Удалить запись?", color = TextPrimary) },
            text = { Text("Вы уверены, что хотите удалить эту запись? Это действие нельзя отменить.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    val newRecords = records.filter { it.id != record.id }
                    records = newRecords
                    saveServiceRecords(context, newRecords)
                    if (record.workId != null) {
                        val w = works.firstOrNull { it.id == record.workId }
                        if (w != null) {
                            saveAndRefreshWorks(works.map {
                                if (it.id == w.id) recalculateWorkAfterRecordRemoval(w, newRecords)
                                else it
                            })
                        }
                    }
                    recordToDelete = null
                }) { Text("Удалить", color = ColorRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) { Text("Отмена", color = TextSecondary) }
            }
        )
    }

    if (carToDelete != null) {
        val carToRemove = carToDelete!!
        AlertDialog(
            onDismissRequest = { carToDelete = null },
            containerColor = CardColor,
            title = { Text("Удалить автомобиль?", color = TextPrimary) },
            text = { Text("Вы уверены, что хотите удалить автомобиль \"${carToRemove.brand} ${carToRemove.model}\"? Все записи об обслуживании этого автомобиля также будут удалены. Это действие нельзя отменить.", color = TextSecondary) },
            confirmButton = { 
                TextButton(onClick = { 
                    cars = cars.filter { it.id != carToRemove.id }
                    saveCars(context, cars)
                    works = works.filter { it.carId != carToRemove.id }
                    saveWorks(context, works)
                    records = records.filter { it.carId != carToRemove.id }
                    saveServiceRecords(context, records)
                    selectedCarId = cars.firstOrNull()?.id
                    context.getSharedPreferences("car_maintenance", android.content.Context.MODE_PRIVATE).edit().putString("selected_car_id", selectedCarId ?: "").apply()
                    screen = "Главная"
                    carToDelete = null 
                }) { Text("Удалить", color = ColorRed, fontWeight = FontWeight.Bold) } 
            },
            dismissButton = { TextButton(onClick = { carToDelete = null }) { Text("Отмена", color = TextSecondary) } }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = CardColor,
            title = { Text("О приложении", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Учёт автомобильного ТО", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Версия 1.2", color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Text("Приложение для учёта обслуживания автомобилей, регламентных работ, запчастей и расходов.", color = TextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Разработано для личного использования.", color = TextSecondary, fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(onClick = { showAboutDialog = false }) { Text("Закрыть", color = ColorGreen) } }
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            containerColor = CardColor,
            title = { Text("Настройки", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Настройки пока в разработке.", color = TextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Здесь появятся опции для изменения темы, резервного копирования и экспорта данных.", color = TextSecondary, fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(onClick = { showSettingsDialog = false }) { Text("Закрыть", color = ColorGreen) } }
        )
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            containerColor = CardColor,
            title = { Text("Резервная копия", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Функция резервного копирования находится в разработке.", color = TextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Пока вы можете сделать резервную копию, скопировав файлы данных приложения (при наличии доступа).", color = TextSecondary, fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(onClick = { showBackupDialog = false }) { Text("Закрыть", color = ColorGreen) } }
        )
    }
}

@Composable
private fun DrawerItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label, color = TextPrimary) },
        icon = { Icon(icon, contentDescription = null, tint = tint) },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, selectedContainerColor = Color.Transparent),
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

@Composable
private fun BottomNav(screen: String, onSelect: (String) -> Unit) {
    NavigationBar(containerColor = NavColor) {
        listOf(
            "Главная" to Icons.Default.Home,
            "Перечень работ" to Icons.Default.List,
            "Регламент" to Icons.Default.Build,
            "Статистика" to Icons.Default.Info,
            "История" to Icons.Default.History
        ).forEach { (title, icon) ->
            NavigationBarItem(
                selected = screen == title, onClick = { onSelect(title) }, icon = { Icon(imageVector = icon, contentDescription = title) }, label = { Text(title, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = ColorGreen, selectedTextColor = ColorGreen, indicatorColor = ColorGreen.copy(alpha = .1f), unselectedIconColor = TextSecondary, unselectedTextColor = TextSecondary)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkListScreen(
    car: Car?, 
    records: List<ServiceRecord>, 
    parts: List<PartCatalogItem>,
    onAdd: () -> Unit, 
    onEdit: (ServiceRecord) -> Unit, 
    onPartSave: (PartCatalogItem) -> Unit,
    works: List<MaintenanceWork>,
    onWorkClick: (MaintenanceWork, ServiceRecord) -> Unit,
    onDeleteRecord: (ServiceRecord) -> Unit
) {
    if (car == null) { Page("Перечень работ") { Text("Сначала добавьте автомобиль", color = TextSecondary) }; return }
    
    var searchQuery by remember { mutableStateOf("") }
    
    Page("Перечень работ") {
        Row( modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp) ) {
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Поиск по названию...", color = TextSecondary) }, singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = ColorGreen, unfocusedBorderColor = TextSecondary, cursorColor = ColorGreen)
            )
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = "Очистить", tint = TextSecondary) }
            }
        }
        
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAdd, modifier = Modifier.weight(1f)) { Text("Добавить запись") }
            Text("${records.count { it.carId == car.id && it.workId == null }} записей", color = TextSecondary, modifier = Modifier.padding(12.dp))
        }
        Spacer(Modifier.height(8.dp))
        
        val carRecords = records
            .filter { it.carId == car.id && it.workId == null }
            .sortedByDescending { it.date }
        
        val filteredItems = carRecords.filter { record ->
            if (searchQuery.isBlank()) true else {
                record.lines.any { it.name.contains(searchQuery, ignoreCase = true) } || 
                record.reason.contains(searchQuery, ignoreCase = true)
            }
        }

        if (filteredItems.isEmpty()) {
            Text(if (searchQuery.isNotBlank()) "Ничего не найдено" else "Пока нет записей вне регламента", color = TextSecondary)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredItems) { record ->
                    ServiceRecordCard(
                        record = record,
                        onClick = { onEdit(record) },
                        onDelete = { onDeleteRecord(record) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegulationScreen(
    car: Car?, 
    works: List<MaintenanceWork>, 
    records: List<ServiceRecord>, 
    filter: WorkStatus?,
    onFilterChange: (WorkStatus?) -> Unit,
    onWork: (MaintenanceWork) -> Unit, 
    onAdd: () -> Unit, 
    onFill: () -> Unit
) {
    if (car == null) { Page("Регламент") { Text("Сначала добавьте автомобиль", color = TextSecondary) }; return }
    
    var searchQuery by remember { mutableStateOf("") }
    
    Page("Регламент") {
        Row( modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp) ) {
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Поиск по названию...", color = TextSecondary) }, singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = ColorGreen, unfocusedBorderColor = TextSecondary, cursorColor = ColorGreen)
            )
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = "Очистить", tint = TextSecondary) }
            }
        }
        
        Spacer(Modifier.height(8.dp))

        Row( modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp) ) {
            FilterChip(selected = filter == null, onClick = { onFilterChange(null) }, label = { Text("Все") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ColorGreen.copy(alpha = 0.2f), selectedLabelColor = ColorGreen, labelColor = TextPrimary))
            FilterChip(selected = filter == WorkStatus.REQUIRED, onClick = { onFilterChange(WorkStatus.REQUIRED) }, label = { Text("Требуется") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ColorRed.copy(alpha = 0.2f), selectedLabelColor = ColorRed, labelColor = TextPrimary))
            FilterChip(selected = filter == WorkStatus.COMPLETED, onClick = { onFilterChange(WorkStatus.COMPLETED) }, label = { Text("Выполнено") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ColorGreen.copy(alpha = 0.2f), selectedLabelColor = ColorGreen, labelColor = TextPrimary))
            FilterChip(selected = filter == WorkStatus.SKIPPED, onClick = { onFilterChange(WorkStatus.SKIPPED) }, label = { Text("Пропущено") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ColorPurple.copy(alpha = 0.2f), selectedLabelColor = ColorPurple, labelColor = TextPrimary))
            FilterChip(selected = filter == WorkStatus.PREVENTIVE, onClick = { onFilterChange(WorkStatus.PREVENTIVE) }, label = { Text("Рекомендовано") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ColorYellow.copy(alpha = 0.2f), selectedLabelColor = ColorYellow, labelColor = TextPrimary))
        }

        Spacer(Modifier.height(8.dp))
        
        val generalWorks = works.filter { it.category == WorkCategory.GENERAL }
        val lpgWorks = works.filter { it.category == WorkCategory.LPG }
        
        val filteredGeneral = generalWorks.filter { work ->
            val matchesSearch = searchQuery.isBlank() || work.name.contains(searchQuery, ignoreCase = true)
            val matchesFilter = filter == null || getWorkStatus(work, car, records) == filter
            matchesSearch && matchesFilter
        }
        val filteredLpg = lpgWorks.filter { work ->
            val matchesSearch = searchQuery.isBlank() || work.name.contains(searchQuery, ignoreCase = true)
            val matchesFilter = filter == null || getWorkStatus(work, car, records) == filter
            matchesSearch && matchesFilter
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (filteredGeneral.isNotEmpty()) {
                item { SectionTitle("Обслуживание автомобиля") }
                items(filteredGeneral) { work -> WorkCard(work, car, records) { onWork(work) } }
            }
            if (car.hasLpg && filteredLpg.isNotEmpty()) {
                item { SectionTitle("Регламент ГБО") }
                items(filteredLpg) { work -> WorkCard(work, car, records) { onWork(work) } }
            }
            if (filteredGeneral.isEmpty() && (!car.hasLpg || filteredLpg.isEmpty())) {
                item { Text(if (searchQuery.isNotBlank()) "Ничего не найдено" else "Нет работ с этим статусом", color = TextSecondary) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreen(
    car: Car?, 
    records: List<ServiceRecord>, 
    works: List<MaintenanceWork>,
    onEditRecord: (ServiceRecord) -> Unit,
    onDeleteRecord: (ServiceRecord) -> Unit
) {
    if (car == null) { Page("История") { Text("Сначала добавьте автомобиль", color = TextSecondary) }; return }
    
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("Все") }
    
    Page("История") {
        Row( modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp) ) {
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Поиск...", color = TextSecondary) }, singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = ColorGreen, unfocusedBorderColor = TextSecondary, cursorColor = ColorGreen)
            )
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = "Очистить", tint = TextSecondary) }
            }
        }
        
        Spacer(Modifier.height(8.dp))

        Row( modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp) ) {
            FilterChip(selected = filterType == "Все", onClick = { filterType = "Все" }, label = { Text("Все") })
            FilterChip(selected = filterType == "Регламентные", onClick = { filterType = "Регламентные" }, label = { Text("Регламентные") })
            FilterChip(selected = filterType == "Свободные", onClick = { filterType = "Свободные" }, label = { Text("Свободные") })
        }

        Spacer(Modifier.height(8.dp))
        
        val carRecords = records
            .filter { it.carId == car.id }
            .filter { 
                when (filterType) {
                    "Регламентные" -> it.workId != null
                    "Свободные" -> it.workId == null
                    else -> true
                }
            }
            .filter { record ->
                if (searchQuery.isBlank()) true else {
                    record.lines.any { it.name.contains(searchQuery, ignoreCase = true) } || 
                    record.reason.contains(searchQuery, ignoreCase = true)
                }
            }
            .sortedByDescending { it.date }

        if (carRecords.isEmpty()) {
            Text(if (searchQuery.isNotBlank()) "Ничего не найдено" else "Пока нет записей", color = TextSecondary)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(carRecords) { record ->
                    HistoryRecordCard(
                        record = record,
                        onClick = { onEditRecord(record) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticsScreen(car: Car?, records: List<ServiceRecord>) {
    if (car == null) { Page("Статистика") { Text("Сначала добавьте автомобиль", color = TextSecondary) }; return }
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
        Text("Расход топлива не рассчитывается: для автомобиля с бензином и ГБО такой показатель без дополнительных данных может быть некорректным.", color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun CarDetailScreen(
    car: Car?, 
    onBack: () -> Unit, 
    onEdit: () -> Unit, 
    onMileage: () -> Unit, 
    onDelete: () -> Unit
) {
    if (car == null) { onBack(); return }
    Page("Автомобиль") {
        TextButton(onClick = onBack) { Text("← Назад", color = ColorBlue) }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardColor)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${car.brand} ${car.model}", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                InfoRow("Год", car.year); InfoRow("Пробег", "${car.mileage} км"); InfoRow("VIN", car.vin)
                InfoRow("Двигатель", car.engine); InfoRow("Топливо", car.fuel); InfoRow("ГБО", if (car.hasLpg) "Установлено" else "Нет")
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onMileage, modifier = Modifier.fillMaxWidth()) { Text("Изменить пробег") }
        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Редактировать") }
        TextButton(onClick = onDelete) { Text("Удалить автомобиль", color = ColorRed) }
    }
}

@Composable
private fun PartsCatalogScreen(car: Car?, parts: List<PartCatalogItem>, onBack: () -> Unit, onAdd: (PartCatalogItem) -> Unit) {
    if (car == null) { Page("Запчасти и расходники") { Text("Сначала добавьте автомобиль", color = TextSecondary) }; return }
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
    if (show) { PartCatalogDialog(car.id, onDismiss = { show = false }) { part -> onAdd(part); show = false } }
}

@Composable
private fun PartCatalogDialog(carId: String, onDismiss: () -> Unit, onSave: (PartCatalogItem) -> Unit) {
    var name by remember { mutableStateOf("") }; var article by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Запчасть") }; var cost by remember { mutableStateOf("") }; var currency by remember { mutableStateOf("RUB") }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = CardColor, title = { Text("Новая запчасть", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") })
                OutlinedTextField(value = article, onValueChange = { article = it }, label = { Text("Артикул") })
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Категория") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Цена") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = currency, onValueChange = { currency = it.uppercase() }, label = { Text("Валюта") }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(PartCatalogItem(carId = carId, name = name, article = article, category = category, lastCost = cost.toDoubleOrNull() ?: 0.0, currency = currency.uppercase())) }) { Text("Сохранить", color = ColorGreen) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = TextSecondary) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ErrorDecoderScreen(onBack: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var selectedBase by remember { mutableStateOf(ErrorBase.HYUNDAI_ACCENT) }

    Page("Расшифровка кодов ошибок") {
        TextButton(onClick = onBack) { Text("← Назад", color = ColorBlue) }
        Text("Выберите базу и введите код (например, P0300 или 0031).", color = TextSecondary)
        Spacer(Modifier.height(8.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selectedBase == ErrorBase.HYUNDAI_ACCENT,
                onClick = { selectedBase = ErrorBase.HYUNDAI_ACCENT },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                colors = SegmentedButtonDefaults.colors(activeContainerColor = ColorGreen.copy(alpha = 0.2f), activeContentColor = ColorGreen),
                modifier = Modifier.weight(1f)
            ) { Text("Hyundai Accent", maxLines = 1) }

            SegmentedButton(
                selected = selectedBase == ErrorBase.BOSCH_ME1797,
                onClick = { selectedBase = ErrorBase.BOSCH_ME1797 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                colors = SegmentedButtonDefaults.colors(activeContainerColor = ColorGreen.copy(alpha = 0.2f), activeContentColor = ColorGreen),
                modifier = Modifier.weight(1f)
            ) { Text("Bosch ME17.9.7", maxLines = 1) }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { code = it.uppercase() },
            label = { Text("Код ошибки") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = ColorGreen,
                unfocusedBorderColor = TextSecondary,
                cursorColor = ColorGreen
            )
        )
        Spacer(Modifier.height(8.dp))
        if (code.isNotBlank()) {
            Text(
                text = obdMeaning(code, selectedBase),
                color = TextPrimary
            )
        }
    }
}

private enum class ErrorBase { HYUNDAI_ACCENT, BOSCH_ME1797 }

// ===== Базы кодов ошибок =====

// База кодов ошибок Hyundai Accent 2008 (MC) 1.4
// Только общие коды OBD-II (P0xxx), применимые к любым авто,
// плюс подтверждённые коды для Accent из TSB и таблиц по модели.
private val hyundaiAccentCodes: Map<String, String> = mapOf(
    // --- Подтверждённые коды для Accent (TSB / таблицы модели) ---
    "P0102" to "ДМРВ. Низкий уровень сигнала",
    "P0103" to "ДМРВ. Высокий уровень сигнала",
    "P0108" to "Датчик абсолютного давления (MAP). Высокий уровень сигнала",
    "P0112" to "Датчик температуры воздуха. Низкий уровень сигнала",
    "P0113" to "Датчик температуры воздуха. Высокий уровень сигнала",
    "P0116" to "Датчик температуры ОЖ. Неисправность цепи",
    "P0117" to "Датчик температуры ОЖ. Низкий уровень сигнала",
    "P0118" to "Датчик температуры ОЖ. Высокий уровень сигнала",
    "P0121" to "ДПДЗ. Неисправность цепи",
    "P0122" to "ДПДЗ. Низкий уровень сигнала",
    "P0123" to "ДПДЗ. Высокий уровень сигнала",
    "P0128" to "Термостат. Температура ниже нормы",
    "P0130" to "Датчик кислорода (Bank 1 Sensor 1). Неисправность цепи",
    "P0131" to "Датчик кислорода (B1S1). Низкий уровень сигнала",
    "P0132" to "Датчик кислорода (B1S1). Высокий уровень сигнала",
    "P0133" to "Датчик кислорода (B1S1). Замедленный отклик",
    "P0134" to "Датчик кислорода (B1S1). Нет активности",
    "P0135" to "Датчик кислорода (B1S1). Неисправность нагревателя",
    "P0137" to "Датчик кислорода (B1S2). Низкий уровень сигнала",
    "P0140" to "Датчик кислорода (B1S2). Нет активности",
    "P0201" to "Форсунка 1-го цилиндра. Неисправность цепи",
    "P0202" to "Форсунка 2-го цилиндра. Неисправность цепи",
    "P0203" to "Форсунка 3-го цилиндра. Неисправность цепи",
    "P0204" to "Форсунка 4-го цилиндра. Неисправность цепи",
    "P0300" to "Случайные/множественные пропуски воспламенения",
    "P0301" to "Пропуски воспламенения в 1-м цилиндре",
    "P0302" to "Пропуски воспламенения во 2-м цилиндре",
    "P0303" to "Пропуски воспламенения в 3-м цилиндре",
    "P0304" to "Пропуски воспламенения в 4-м цилиндре",
    "P0326" to "Датчик детонации. Неисправность цепи",
    "P0335" to "Датчик положения коленвала. Неисправность цепи",
    "P0336" to "Датчик положения коленвала. Сбой сигнала",
    "P0342" to "Датчик положения распредвала. Низкий уровень сигнала",
    "P0343" to "Датчик положения распредвала. Высокий уровень сигнала",
    "P0422" to "Эффективность нейтрализатора ниже нормы",
    "P0441" to "Система улавливания паров. Неверный расход воздуха",
    "P0442" to "Система улавливания паров. Малая утечка",
    "P0444" to "Клапан продувки адсорбера. Обрыв цепи",
    "P0445" to "Клапан продувки адсорбера. КЗ цепи",
    "P0451" to "Датчик давления паров топлива. Сигнал вне диапазона",
    "P0461" to "Датчик уровня топлива. Сигнал вне диапазона",
    "P0501" to "Датчик скорости автомобиля. Неисправность цепи",
    "P0506" to "Пониженные обороты холостого хода",
    "P0507" to "Повышенные обороты холостого хода",
    "P0562" to "Пониженное напряжение бортовой сети",
    "P0563" to "Повышенное напряжение бортовой сети",
    "P0606" to "Внутренняя неисправность ЭБУ",

    // --- Общие OBD-II коды (все машины) ---
    "P0171" to "Слишком бедная топливовоздушная смесь",
    "P0172" to "Слишком богатая топливовоздушная смесь",
    "P0420" to "Эффективность нейтрализатора ниже порога"
)

// База кодов ошибок ЭБУ Bosch ME17.9.7 (УАЗ Патриот, Евро-3)
// Источники: samara-stavr.ru, chiptuner.ru, forum.uazbuka.ru
private val boschMe1797Codes: Map<String, String> = mapOf(
    // --- Нагреватели датчиков кислорода ---
    "0030" to "ДК1. Неисправность цепи нагревателя",
    "0031" to "ДК1. Обрыв или замыкание на «массу» цепи нагревателя",
    "0032" to "ДК1. Короткое замыкание на «бортсеть» цепи нагревателя",
    "0036" to "ДК2. Неисправность цепи нагревателя",
    "0037" to "ДК2. Обрыв или замыкание на «массу» цепи нагревателя",
    "0038" to "ДК2. Короткое замыкание на «бортсеть» цепи нагревателя",

    // --- ДМРВ, ДТВ, ДТОЖ ---
    "0101" to "ДМРВ. Сигнал вне допустимого диапазона",
    "0102" to "ДМРВ. Низкий уровень сигнала",
    "0103" to "ДМРВ. Высокий уровень сигнала",
    "0112" to "ДТВ. Низкий уровень сигнала",
    "0113" to "ДТВ. Высокий уровень сигнала",
    "0116" to "ДТОЖ. Сигнал вне допустимого диапазона",
    "0117" to "ДТОЖ. Низкий уровень сигнала",
    "0118" to "ДТОЖ. Высокий уровень сигнала",

    // --- Дроссельная заслонка (датчики) ---
    "0122" to "ДПДЗ 1. Низкий уровень сигнала",
    "0123" to "ДПДЗ 1. Высокий уровень сигнала",
    "0222" to "ДПДЗ 2. Низкий уровень сигнала",
    "0223" to "ДПДЗ 2. Высокий уровень сигнала",

    // --- Датчики кислорода (сигнальные цепи) ---
    "0130" to "ДК1. Неисправность сигнальной цепи",
    "0131" to "ДК1. Низкий уровень сигнала",
    "0132" to "ДК1. Высокий уровень сигнала",
    "0133" to "ДК1. Медленный отклик на изменение состава смеси",
    "0134" to "ДК1. Потеря активности сигнала или обрыв цепи",
    "0135" to "ДК1. Неисправность цепи нагревателя",
    "0136" to "ДК2. Неисправность сигнальной цепи",
    "0137" to "ДК2. Низкий уровень сигнала",
    "0138" to "ДК2. Высокий уровень сигнала",
    "0140" to "ДК2. Потеря активности сигнала или обрыв цепи",
    "0141" to "ДК2. Неисправность цепи нагревателя",

    // --- Топливоподача ---
    "0171" to "Система топливоподачи слишком «бедная» при максимальном обогащении",
    "0172" to "Система топливоподачи слишком «богатая» при максимальном обеднении",
    "2187" to "Система топливоподачи дрейфует к «бедной» области на холостом ходу",
    "2188" to "Система топливоподачи дрейфует к «богатой» области на холостом ходу",

    // --- Форсунки ---
    "0201" to "Форсунка 1. Неисправность цепи управления",
    "0202" to "Форсунка 2. Неисправность цепи управления",
    "0203" to "Форсунка 3. Неисправность цепи управления",
    "0204" to "Форсунка 4. Неисправность цепи управления",
    "0261" to "Форсунка 1. Обрыв или КЗ на «массу» цепи управления",
    "0262" to "Форсунка 1. КЗ на «бортсеть» цепи управления",
    "0264" to "Форсунка 2. Обрыв или КЗ на «массу» цепи управления",
    "0265" to "Форсунка 2. КЗ на «бортсеть» цепи управления",
    "0267" to "Форсунка 3. Обрыв или КЗ на «массу» цепи управления",
    "0268" to "Форсунка 3. КЗ на «бортсеть» цепи управления",
    "0270" to "Форсунка 4. Обрыв или КЗ на «массу» цепи управления",
    "0271" to "Форсунка 4. КЗ на «бортсеть» цепи управления",

    // --- Пропуски воспламенения ---
    "0300" to "Случайные/множественные пропуски воспламенения",
    "0301" to "Цилиндр 1. Пропуски воспламенения",
    "0302" to "Цилиндр 2. Пропуски воспламенения",
    "0303" to "Цилиндр 3. Пропуски воспламенения",
    "0304" to "Цилиндр 4. Пропуски воспламенения",

    // --- Датчик детонации, коленвал, распредвал ---
    "0327" to "Датчик детонации. Низкий уровень сигнала",
    "0335" to "Датчик положения коленчатого вала. Неисправность цепи",
    "0340" to "Датчик фазы. Неисправность",

    // --- Нейтрализатор ---
    "0420" to "Эффективность нейтрализатора ниже допустимой нормы",
    "0422" to "Эффективность нейтрализатора ниже порога",

    // --- Адсорбер ---
    "0441" to "Система улавливания паров. Неверный расход воздуха через КПА",
    "0444" to "Клапан продувки адсорбера. Обрыв цепи управления",
    "0458" to "Клапан продувки адсорбера. КЗ на «массу»",
    "0459" to "Клапан продувки адсорбера. КЗ на «бортсеть»",

    // --- Вентиляторы, реле ---
    "0480" to "Реле вентилятора 1. Обрыв цепи управления",
    "0481" to "Реле вентилятора 2. Обрыв цепи управления",
    "0485" to "Напряжение питания вентилятора вне диапазона",
    "0691" to "Реле вентилятора 1. КЗ на «массу»",
    "0692" to "Реле вентилятора 1. КЗ на «бортсеть»",
    "0693" to "Реле вентилятора 2. КЗ на «массу»",
    "0694" to "Реле вентилятора 2. КЗ на «бортсеть»",

    // --- Скорость, педаль тормоза ---
    "0500" to "Датчик скорости автомобиля. Нет сигнала",
    "0501" to "Датчик скорости. Сигнал вне диапазона",
    "0504" to "Выключатели педали тормоза. Рассогласование сигналов",

    // --- Напряжение бортовой сети ---
    "0560" to "Напряжение бортовой сети",
    "0561" to "Напряжение бортовой сети нестабильно",
    "0562" to "Напряжение бортовой сети. Низкий уровень",
    "0563" to "Напряжение бортовой сети. Высокий уровень",

    // --- ЭБУ ---
    "0606" to "Контроллер СУД. Неисправность АЦП",
    "0627" to "Реле бензонасоса. Обрыв цепи управления",
    "0628" to "Реле бензонасоса. КЗ на «массу»",
    "0629" to "Реле бензонасоса. КЗ на «бортсеть»",
    "0645" to "Реле муфты кондиционера. Обрыв цепи управления",
    "0646" to "Реле муфты кондиционера. КЗ на «массу»",
    "0647" to "Реле муфты кондиционера. КЗ на «бортсеть»",

    // --- Электронный дроссель (мониторинг) ---
    "1335" to "Положение дроссельной заслонки вне допустимого диапазона",
    "1336" to "Рассогласование сигналов датчиков «А»/«В» дросселя",
    "1388" to "Рассогласование сигналов датчиков «А»/«В» педали акселератора",
    "1389" to "Обороты двигателя вне допустимого диапазона",
    "1390" to "Некорректная реакция на неисправность в системе",
    "1391" to "Отсутствует реакция на неисправность в системе",
    "1545" to "Привод дроссельной заслонки. Положение заслонки вне диапазона",
    "1558" to "Привод дроссельной заслонки. Возвратная пружина неисправна",
    "1559" to "Привод дроссельной заслонки. Положение покоя вне диапазона",
    "1564" to "Адаптация нуля заслонки прервана из-за низкого напряжения",

    // --- Иммобилайзер ---
    "1570" to "Иммобилизатор. Цепь неисправна",

    // --- Датчики кислорода (диагностические, после нейтрализатора) ---
    "2195" to "Нет совпадения сигналов датчиков кислорода 1 и 2",
    "2270" to "ДК2. Сигнал в состоянии «бедно»",
    "2271" to "ДК2. Сигнал в состоянии «богато»",

    // --- CAN ---
    "C001" to "Неисправность информационной CAN-шины"
)

private fun obdMeaning(code: String, base: ErrorBase): String {
    val normalized = code.trim().uppercase()
    return when (base) {
        ErrorBase.HYUNDAI_ACCENT -> {
            // Hyundai: коды обычно в формате P0300 или 0300
            val key = if (normalized.startsWith("P") || normalized.startsWith("C") || normalized.startsWith("B") || normalized.startsWith("U")) {
                normalized
            } else {
                "P$normalized"
            }
            val description = hyundaiAccentCodes[key]
            if (description != null) "$key — $description"
            else "Код $normalized: расшифровка пока отсутствует в базе Hyundai Accent."
        }
        ErrorBase.BOSCH_ME1797 -> {
            // Bosch: коды вида 0031 или C001
            val key = when {
                normalized.startsWith("P") || normalized.startsWith("C") -> normalized.drop(1)
                else -> normalized
            }
            val description = boschMe1797Codes[key]
            if (description != null) "$normalized — $description"
            else "Код $normalized: расшифровка пока отсутствует в базе Bosch ME17.9.7."
        }
    }
}

private fun WorkStatus.title(): String = when(this) {
    WorkStatus.COMPLETED -> "Выполнено"
    WorkStatus.REQUIRED -> "Необходимо"
    WorkStatus.PREVENTIVE -> "Рекомендации"
    WorkStatus.SKIPPED -> "Пропущено"
}
