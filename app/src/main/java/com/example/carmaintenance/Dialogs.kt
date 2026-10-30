package com.example.carmaintenance

import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import java.util.Calendar

// ==================== ДИАЛОГ АВТОМОБИЛЯ ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCarDialog(existingCar: Car?, onDismiss: () -> Unit, onSave: (Car) -> Unit, onDelete: (() -> Unit)? = null) {
    var brand by remember { mutableStateOf(existingCar?.brand ?: "") }; var model by remember { mutableStateOf(existingCar?.model ?: "") }
    var year by remember { mutableStateOf(existingCar?.year ?: "") }; var vin by remember { mutableStateOf(existingCar?.vin ?: "") }
    var mileage by remember { mutableStateOf(existingCar?.mileage ?: "") }; var engine by remember { mutableStateOf(existingCar?.engine ?: "") }
    var hasLpg by remember { mutableStateOf(existingCar?.hasLpg ?: false) }; var notes by remember { mutableStateOf(existingCar?.notes ?: "") }
    var imageUri by remember { mutableStateOf(existingCar?.imageUri ?: "") }

    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val savedPath = copyImageToInternalStorage(context, uri)
            if (savedPath != null) {
                imageUri = savedPath
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = CardColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .width(32.dp)
                        .height(4.dp)
                        .background(TextSecondary.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                        .align(Alignment.CenterHorizontally)
                )

                Text(
                    text = if (existingCar == null) "Новый автомобиль" else "Редактировать автомобиль",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    if (imageUri.isNotBlank()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "Фото авто",
                                modifier = Modifier.fillMaxWidth().fillMaxHeight().background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { imageUri = "" },
                                modifier = Modifier.padding(4.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить фото", tint = Color.White)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Button(
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (imageUri.isBlank()) "Загрузить фото" else "Изменить фото", fontSize = 14.sp)
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))
                    Spacer(Modifier.height(16.dp))

                    Text("Основная информация", color = ColorGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        modernField("Марка *", brand, Modifier.weight(1f)) { brand = it }
                        modernField("Модель *", model, Modifier.weight(1f)) { model = it }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        modernField("Год выпуска", year, Modifier.weight(1f), KeyboardType.Number) { year = it }
                        modernField("Пробег (км)", mileage, Modifier.weight(1f), KeyboardType.Number) { mileage = it }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))
                    Spacer(Modifier.height(16.dp))

                    Text("Технические данные", color = ColorGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    
                    modernField("VIN-код", vin) { vin = it }
                    Spacer(Modifier.height(8.dp))
                    modernField("Двигатель", engine) { engine = it }
                    Spacer(Modifier.height(8.dp))
                    modernField("Заметки", notes) { notes = it }
                    
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = hasLpg, 
                            onCheckedChange = { hasLpg = it },
                            colors = CheckboxDefaults.colors(checkedColor = ColorGreen, uncheckedColor = TextSecondary)
                        )
                        Text("Установлено ГБО (Газ)", color = TextPrimary, fontSize = 14.sp)
                    }
                    
                    Spacer(Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (existingCar != null && onDelete != null) {
                        OutlinedButton(
                            onClick = { onDelete() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorRed),
                            border = BorderStroke(1.dp, ColorRed)
                        ) { Text("Удалить") }
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.5f))
                    ) { Text("Отмена") }
                    Button(
                        onClick = {
                            if (brand.isNotBlank() && model.isNotBlank()) {
                                val newCar = existingCar?.copy(
                                    brand = brand, model = model, year = year, vin = vin, mileage = mileage,
                                    engine = engine, fuel = if(hasLpg) "Бензин/Газ" else "Бензин", hasLpg = hasLpg,
                                    notes = notes, imageUri = imageUri
                                ) ?: Car(
                                    brand = brand, model = model, year = year, vin = vin, mileage = mileage,
                                    engine = engine, fuel = if(hasLpg) "Бензин/Газ" else "Бензин", hasLpg = hasLpg,
                                    notes = notes, imageUri = imageUri
                                )
                                onSave(newCar)
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorGreen)
                    ) { Text("Сохранить", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ==================== ДИАЛОГ ЗАПИСИ ОБСЛУЖИВАНИЯ (в новом стиле) ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceRecordDialog(
    car: Car, 
    initial: ServiceRecord? = null, 
    suggestedName: String = "", 
    parts: List<PartCatalogItem>, 
    works: List<MaintenanceWork>, 
    onDismiss: () -> Unit, 
    onSave: (ServiceRecord) -> Unit,
    onDelete: ((ServiceRecord) -> Unit)? = null
) {
    var date by remember { mutableStateOf(initial?.date ?: System.currentTimeMillis()) }
    var place by remember { mutableStateOf(initial?.servicePlace ?: "") }
    var reason by remember { mutableStateOf(initial?.reason ?: "") }
    var mileage by remember { mutableStateOf((initial?.mileage ?: car.mileage.toIntOrNull() ?: 0).toString()) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var lines by remember { mutableStateOf(initial?.lines ?: listOf(ServiceLine(name = suggestedName))) }
    
    var mode by remember { mutableStateOf(0) }
    var selectedWork by remember { mutableStateOf<MaintenanceWork?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current

    val availableWorks = works.filter { it.carId == car.id }.sortedBy {
        when (getWorkStatus(it, car, listOfNotNull(initial))) {
            WorkStatus.SKIPPED -> 0
            WorkStatus.REQUIRED -> 1
            WorkStatus.PREVENTIVE -> 2
            else -> 3
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = CardColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .width(32.dp)
                        .height(4.dp)
                        .background(TextSecondary.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                        .align(Alignment.CenterHorizontally)
                )

                Text(
                    text = if (initial == null) "Новая запись" else "Редактировать запись",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    if (initial == null) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = mode == 0,
                                onClick = { mode = 0 },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                colors = SegmentedButtonDefaults.colors(activeContainerColor = ColorGreen.copy(alpha = 0.2f), activeContentColor = ColorGreen),
                                modifier = Modifier.weight(1f)
                            ) { Text("Ремонт", maxLines = 1) }
                            
                            SegmentedButton(
                                selected = mode == 1,
                                onClick = { mode = 1 },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                colors = SegmentedButtonDefaults.colors(activeContainerColor = ColorGreen.copy(alpha = 0.2f), activeContentColor = ColorGreen),
                                modifier = Modifier.weight(1f)
                            ) { Text("По регламенту", maxLines = 1) }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    if (mode == 1 && availableWorks.isNotEmpty()) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedWork?.name ?: "Выберите работу...",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Регламентная работа", fontSize = 12.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = ColorGreen, unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                                    focusedContainerColor = BgColor.copy(alpha = 0.5f), unfocusedContainerColor = BgColor.copy(alpha = 0.5f)
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(CardColor)
                            ) {
                                availableWorks.forEach { work ->
                                    DropdownMenuItem(
                                        text = { 
                                            Column {
                                                Text(work.name, color = TextPrimary)
                                                Text("Статус: ${getWorkStatus(work, car, listOfNotNull(initial)).title()}", color = TextSecondary, fontSize = 12.sp)
                                            }
                                        },
                                        onClick = {
                                            selectedWork = work
                                            if (lines.isEmpty()) {
                                                lines = listOf(ServiceLine(name = work.name))
                                            } else {
                                                lines = lines.toMutableList().also { it[0] = it[0].copy(name = work.name) }
                                            }
                                            reason = work.name
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    Text("Дата: ${formatDate(date)}", color = TextPrimary, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = {
                            val c = Calendar.getInstance().apply { timeInMillis = date }
                            DatePickerDialog(context, { _, y, m, d -> date = Calendar.getInstance().apply { set(y, m, d, 12, 0, 0) }.timeInMillis }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavColor),
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("Изменить дату", fontSize = 14.sp) }
                    
                    Spacer(Modifier.height(12.dp))

                    modernField("СТО / магазин", place) { place = it }
                    Spacer(Modifier.height(8.dp))
                    modernField("Причина", reason) { reason = it }
                    Spacer(Modifier.height(8.dp))
                    modernField("Пробег (км)", mileage, type = KeyboardType.Number) { mileage = it }
                    Spacer(Modifier.height(8.dp))
                    modernField("Примечания", notes) { notes = it }
                    
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))
                    Spacer(Modifier.height(16.dp))

                    Text("Работы и запчасти", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    lines.forEachIndexed { i, l -> 
                        LineEditor(l, onChange = { x -> lines = lines.toMutableList().also { it[i] = x } }, onDelete = { lines = lines.toMutableList().also { it.removeAt(i) } }) 
                    }
                    TextButton(onClick = { lines = lines + ServiceLine(name = "") }, modifier = Modifier.fillMaxWidth()) { 
                        Text("+ Добавить строку", color = ColorGreen) 
                    }
                    if (parts.isNotEmpty()) {
                        Text("Сохранённые запчасти можно повторно выбрать в перечне работ.", color = TextSecondary, fontSize = 12.sp)
                    }
                    
                    Spacer(Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (initial != null && onDelete != null) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorRed),
                            border = BorderStroke(1.dp, ColorRed)
                        ) { Text("Удалить") }
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.5f))
                    ) { Text("Отмена") }
                    Button(
                        onClick = {
                            onSave(
                                ServiceRecord(
                                    id = initial?.id ?: java.util.UUID.randomUUID().toString(),
                                    carId = car.id,
                                    workId = selectedWork?.id ?: initial?.workId,
                                    date = date,
                                    servicePlace = place,
                                    reason = reason,
                                    mileage = mileage.toIntOrNull() ?: 0,
                                    lines = lines.filter { it.name.isNotBlank() },
                                    notes = notes
                                )
                            )
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorGreen)
                    ) { Text("Сохранить", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }

    if (showDeleteConfirm && initial != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = CardColor,
            title = { Text("Удалить запись?", color = TextPrimary) },
            text = { Text("Вы уверены, что хотите удалить эту запись? Это действие нельзя отменить.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(initial)
                    showDeleteConfirm = false
                    onDismiss()
                }) { Text("Удалить", color = ColorRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Отмена", color = TextSecondary) }
            }
        )
    }
}

// ==================== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ====================

@Composable
private fun modernField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    type: KeyboardType = KeyboardType.Text,
    onValue: (String) -> Unit
) = OutlinedTextField(
    value = value,
    onValueChange = onValue,
    label = { Text(label, fontSize = 12.sp) },
    keyboardOptions = KeyboardOptions(keyboardType = type),
    singleLine = true,
    modifier = modifier,
    shape = RoundedCornerShape(10.dp),
    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
    colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedBorderColor = ColorGreen,
        unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
        focusedContainerColor = BgColor.copy(alpha = 0.5f),
        unfocusedContainerColor = BgColor.copy(alpha = 0.5f),
        focusedLabelColor = ColorGreen,
        unfocusedLabelColor = TextSecondary,
        cursorColor = ColorGreen
    )
)

@Composable 
private fun field(
    label: String, 
    value: String, 
    type: KeyboardType = KeyboardType.Text, 
    modifier: Modifier = Modifier,
    onValue: (String) -> Unit
) = OutlinedTextField(
    value = value,
    onValueChange = onValue,
    label = { Text(label) },
    keyboardOptions = KeyboardOptions(keyboardType = type),
    singleLine = label != "Заметки",
    modifier = modifier,
    colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedBorderColor = ColorBlue,
        unfocusedBorderColor = TextSecondary,
        focusedLabelColor = ColorBlue,
        unfocusedLabelColor = TextSecondary,
        cursorColor = ColorBlue
    )
)

@Composable 
private fun LineEditor(line: ServiceLine, onChange: (ServiceLine)->Unit, onDelete: ()->Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        field("Название", line.name) { onChange(line.copy(name = it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            field("Артикул", line.article, modifier = Modifier.weight(1f)) { onChange(line.copy(article = it)) }
            field("Стоимость", if(line.cost == 0.0) "" else line.cost.toString(), KeyboardType.Decimal, modifier = Modifier.weight(1f)) { onChange(line.copy(cost = it.toDoubleOrNull() ?: 0.0)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            field("Валюта", line.currency, modifier = Modifier.weight(1f)) { onChange(line.copy(currency = it.uppercase())) }
            field("Тип масла", line.oilType, modifier = Modifier.weight(1f)) { onChange(line.copy(oilType = it)) }
        }
        field("Название", line.oilName) { onChange(line.copy(oilName = it)) }
        TextButton(onClick = onDelete) { Text("Удалить строку", color = ColorRed) }
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// ==================== ОСТАЛЬНЫЕ ДИАЛОГИ (без изменений) ====================

@Composable
fun MileageDialog(current: String, onDismiss:()->Unit, onSave:(String)->Unit) { var v by remember { mutableStateOf(current) }; AlertDialog(onDismissRequest=onDismiss,containerColor=CardColor,title={Text("Текущий пробег",color=TextPrimary)},text={field("Пробег (км)",v,KeyboardType.Number){v=it}},confirmButton={TextButton(onClick={onSave(v)}){Text("Сохранить",color=ColorGreen)}},dismissButton={TextButton(onClick=onDismiss){Text("Отмена",color=TextSecondary)}}) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkActionsDialog(
    work: MaintenanceWork,
    car: Car,
    records: List<ServiceRecord>,
    initialRecord: ServiceRecord? = null,
    onDismiss: () -> Unit,
    onComplete: (km: Int, date: Long, note: String, lines: List<ServiceLine>) -> Unit,
    onSkip: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShowInList: () -> Unit,
    onEditRecord: (ServiceRecord) -> Unit,
    onCancelRecord: (ServiceRecord) -> Unit
) {
    var showCompleteForm by remember { mutableStateOf(initialRecord != null) }
    
    var km by remember { mutableStateOf(initialRecord?.mileage?.toString() ?: car.mileage.ifBlank { "0" }) }
    var note by remember { mutableStateOf(initialRecord?.notes ?: "") }
    var selectedDate by remember { mutableStateOf(initialRecord?.date ?: System.currentTimeMillis()) }
    
    val lines = remember { 
        mutableStateListOf<ServiceLine>().apply {
            if (initialRecord != null) addAll(initialRecord.lines)
            else add(ServiceLine(name = work.name, type = ServiceLineType.WORK))
        } 
    }
    
    val context = androidx.compose.ui.platform.LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = CardColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .width(32.dp)
                        .height(4.dp)
                        .background(TextSecondary.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                        .align(Alignment.CenterHorizontally)
                )

                Text(
                    text = if (showCompleteForm) {
                        if (initialRecord != null) "Изменить выполнение: ${work.name}" else "Выполнение: ${work.name}"
                    } else {
                        work.name
                    },
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    if (!showCompleteForm) {
                        // --- РЕЖИМ ПРОСМОТРА ---
                        
                        InfoRow(
                            label = "Последняя замена, км:",
                            value = if (work.hasLastExecution && work.lastMileage > 0) "${work.lastMileage}" else "—",
                            valueColor = ColorGreen.copy(alpha = 0.7f)
                        )
                        InfoRow(
                            label = "Дата последней замены:",
                            value = if (work.hasLastExecution && work.lastDate > 0) formatDate(work.lastDate) else "—",
                            valueColor = ColorGreen.copy(alpha = 0.7f)
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = TextSecondary.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )
                        
                        val currentMileage = car.mileage.toIntOrNull() ?: 0
                        val nextMileage = if (work.hasLastExecution) work.lastMileage + work.intervalKm else currentMileage + work.intervalKm
                        val remainingKm = nextMileage - currentMileage
                        
                        InfoRow(
                            label = "Следующая замена, км:",
                            value = "$nextMileage",
                            valueColor = if (currentMileage >= nextMileage) Color.Red else TextPrimary
                        )
                        InfoRow(
                            label = "Остаток, км:",
                            value = "$remainingKm",
                            valueColor = if (remainingKm > 0) TextPrimary else Color.Red
                        )
                        
                        // --- Блок месяцев ---
                        if (work.intervalMonths > 0) {
                            val currentTime = System.currentTimeMillis()
                            val lastExecTime = if (work.hasLastExecution) work.lastDate else currentTime
                            
                            val monthsPassed = if (work.hasLastExecution) {
                                ((currentTime - lastExecTime) / (1000L * 60 * 60 * 24 * 30)).toInt()
                            } else 0
                            
                            val nextMonths = work.intervalMonths
                            val remainingMonths = nextMonths - monthsPassed
                            val isOverdue = remainingMonths <= 0
                            
                            InfoRow(
                                label = "Следующая замена, мес:",
                                value = "$nextMonths",
                                valueColor = if (isOverdue) Color.Red else TextPrimary
                            )
                            InfoRow(
                                label = "Остаток, мес:",
                                value = "$remainingMonths",
                                valueColor = if (remainingMonths > 0) TextPrimary else Color.Red
                            )
                        }

                        val completedRecords = records.filter { it.workId == work.id }
                        if (completedRecords.isNotEmpty()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = TextSecondary.copy(alpha = 0.3f),
                                thickness = 1.dp
                            )
                            Text("Выполнения:", color = TextPrimary, fontWeight = FontWeight.Bold)
                            completedRecords.forEach { record ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${record.mileage} км", color = TextPrimary, fontSize = 14.sp)
                                        Text(formatDate(record.date), color = TextSecondary, fontSize = 12.sp)
                                    }
                                    TextButton(onClick = { onEditRecord(record) }) {
                                        Text("Изменить", color = ColorYellow, fontSize = 12.sp)
                                    }
                                    TextButton(onClick = { onCancelRecord(record) }) {
                                        Text("Отменить", color = ColorRed, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        
                    } else {
                        // --- РЕЖИМ ВЫПОЛНЕНИЯ ---
                        
                        Text("Дата: ${formatDate(selectedDate)}", color = TextPrimary, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = selectedDate }
                                DatePickerDialog(context, { _, y, m, d -> 
                                    selectedDate = Calendar.getInstance().apply { set(y, m, d, 12, 0, 0) }.timeInMillis 
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavColor),
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("Изменить дату", fontSize = 14.sp) }
                        
                        Spacer(Modifier.height(12.dp))

                        modernField("Пробег (км)", km, type = KeyboardType.Number) { km = it }
                        
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))
                        Spacer(Modifier.height(16.dp))

                        Text("Работы и материалы", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        
                        lines.forEachIndexed { index, line ->
                            Card(colors = CardDefaults.cardColors(containerColor = BgColor), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedTextField(value = line.name, onValueChange = { lines[index] = line.copy(name = it) }, label = { Text("Наименование", fontSize = 12.sp) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = ColorGreen, unfocusedBorderColor = TextSecondary, focusedLabelColor = ColorGreen, unfocusedLabelColor = TextSecondary, cursorColor = ColorGreen))
                                    OutlinedTextField(value = line.article, onValueChange = { lines[index] = line.copy(article = it) }, label = { Text("Артикул", fontSize = 12.sp) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = ColorGreen, unfocusedBorderColor = TextSecondary, focusedLabelColor = ColorGreen, unfocusedLabelColor = TextSecondary, cursorColor = ColorGreen))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = if (line.cost == 0.0) "" else line.cost.toString(),
                                            onValueChange = { val c = it.replace(",", ".").toDoubleOrNull() ?: 0.0; lines[index] = line.copy(cost = c) },
                                            label = { Text("Цена", fontSize = 12.sp) }, modifier = Modifier.weight(1f), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = ColorGreen, unfocusedBorderColor = TextSecondary, focusedLabelColor = ColorGreen, unfocusedLabelColor = TextSecondary, cursorColor = ColorGreen)
                                        )
                                        var expanded by remember { mutableStateOf(false) }
                                        val currencies = listOf("RUB", "UAH")
                                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.weight(1f)) {
                                            OutlinedTextField(value = line.currency, onValueChange = {}, readOnly = true, label = { Text("Вал.", fontSize = 12.sp) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = ColorGreen, unfocusedBorderColor = TextSecondary, focusedLabelColor = ColorGreen, unfocusedLabelColor = TextSecondary))
                                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(CardColor)) {
                                                currencies.forEach { selection -> DropdownMenuItem(text = { Text(selection, color = TextPrimary) }, onClick = { lines[index] = line.copy(currency = selection); expanded = false }) }
                                            }
                                        }
                                    }
                                    if (lines.size > 1) { TextButton(onClick = { lines.removeAt(index) }, modifier = Modifier.align(Alignment.End)) { Text("Удалить строку", color = ColorRed, fontSize = 12.sp) } }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        
                        Button(onClick = { lines.add(ServiceLine(name = "", type = ServiceLineType.WORK)) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ColorBlue.copy(alpha = 0.2f)), shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = ColorBlue); Spacer(Modifier.width(8.dp)); Text("Добавить строку", color = ColorBlue)
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        modernField("Примечание ко всей записи", note) { note = it }
                        
                        Spacer(Modifier.height(16.dp))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!showCompleteForm) {
                        val currentMileage = car.mileage.toIntOrNull() ?: 0
                        val nextMileage = if (work.hasLastExecution) work.lastMileage + work.intervalKm else currentMileage + work.intervalKm
                        val showSkipButton = currentMileage >= nextMileage && !work.skipped

                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) { 
                            Text("Закрыть", fontSize = 14.sp, maxLines = 1) 
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (showSkipButton) {
                                OutlinedButton(
                                    onClick = onSkip,
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorPurple),
                                    border = BorderStroke(1.dp, ColorPurple),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) { 
                                    Text("Пропустить", fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1) 
                                }
                            }

                            Button(
                                onClick = { showCompleteForm = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ColorGreen),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) { 
                                Text("Выполнить", fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1) 
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { 
                                if (initialRecord != null) onDismiss() else showCompleteForm = false 
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) { 
                            Text(if (initialRecord != null) "Отмена" else "Назад", fontSize = 14.sp, maxLines = 1) 
                        }
                        
                        Button(
                            onClick = { onComplete(km.toIntOrNull() ?: 0, selectedDate, note, lines.toList()) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorGreen),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) { 
                            Text("Сохранить", fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1) 
                        }
                    }
                }
            }
        }
    }
}
                        
@Composable
fun CompletedWorkInfoDialog(
    work: MaintenanceWork,
    car: Car,
    record: ServiceRecord,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCancelCompletion: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardColor,
        title = { Text(work.name, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Выполнено:", color = TextSecondary)
                    Text("${record.mileage} км / ${formatDate(record.date)}", color = ColorGreen, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))
                Text("Работы и материалы:", color = TextPrimary, fontWeight = FontWeight.Bold)
                record.lines.forEach { line ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = line.name, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (line.article.isNotBlank()) { Text(text = "[${line.article}]", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        Text(text = "${formatMoney(line.cost)} ${line.currency}", color = ColorBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onCancelCompletion, modifier = Modifier.fillMaxWidth()) {
                    Text("Отменить выполнение", color = ColorRed, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = { TextButton(onClick = onEdit) { Text("Изменить", color = ColorYellow, fontWeight = FontWeight.Bold) } },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Удалить", color = ColorRed) }
                TextButton(onClick = onDismiss) { Text("Закрыть", color = TextSecondary) }
            }
        }
    )
}

@Composable
fun RegulationWorkDialog(
    carId: String, 
    existing: MaintenanceWork?, 
    templates: List<RegulationTemplate>, 
    onDismiss: () -> Unit, 
    onSave: (MaintenanceWork) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var groupName by remember { mutableStateOf(existing?.groupName ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: WorkCategory.GENERAL) }
    var km by remember { mutableStateOf(existing?.intervalKm?.toString() ?: "") }
    var months by remember { mutableStateOf(existing?.intervalMonths?.toString() ?: "") }
    var warningKm by remember { mutableStateOf(existing?.warningKm?.toString() ?: "") }
    var warningMonths by remember { mutableStateOf(existing?.warningMonths?.toString() ?: "") }
    var preventive by remember { mutableStateOf(existing?.preventive ?: false) }
    
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, 
        focusedBorderColor = ColorBlue, unfocusedBorderColor = TextSecondary, 
        focusedLabelColor = ColorBlue, unfocusedLabelColor = TextSecondary, 
        cursorColor = ColorBlue
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardColor,
        title = { Text(if (existing == null) "Добавить регламентную работу" else "Изменить регламентную работу", color = TextPrimary) },
        text = {
            Column(
                Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Название работы") }, 
                    singleLine = true, 
                    modifier = Modifier.fillMaxWidth(), 
                    colors = fieldColors
                )
                
                OutlinedTextField(
                    value = groupName, 
                    onValueChange = { groupName = it }, 
                    label = { Text("Группа (например, 1. МАСЛА)") }, 
                    singleLine = true, 
                    modifier = Modifier.fillMaxWidth(), 
                    colors = fieldColors
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = category == WorkCategory.GENERAL, onClick = { category = WorkCategory.GENERAL }, label = { Text("Автомобиль") })
                    FilterChip(selected = category == WorkCategory.LPG, onClick = { category = WorkCategory.LPG }, label = { Text("ГБО") })
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(km, { km = it }, label = { Text("Интервал км") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), colors = fieldColors)
                    OutlinedTextField(months, { months = it }, label = { Text("Интервал мес.") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), colors = fieldColors)
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(warningKm, { warningKm = it }, label = { Text("Предупреждение км") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), colors = fieldColors)
                    OutlinedTextField(warningMonths, { warningMonths = it }, label = { Text("Предупреждение мес.") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), colors = fieldColors)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(preventive, { preventive = it })
                    Text("Профилактическая рекомендация", color = TextPrimary)
                }
                
                Text("Можно создать работу без интервала — например, для проверки состояния детали. Её статус будет профилактическим.", color = TextSecondary, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onSave(MaintenanceWork(
                        id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                        carId = carId,
                        templateId = existing?.templateId ?: (templates.firstOrNull { it.name == name }?.id ?: ""),
                        name = name,
                        groupName = groupName,
                        category = category,
                        intervalKm = km.toIntOrNull() ?: 0,
                        intervalMonths = months.toIntOrNull() ?: 0,
                        warningKm = warningKm.toIntOrNull() ?: 0,
                        warningMonths = warningMonths.toIntOrNull() ?: 0,
                        lastMileage = existing?.lastMileage ?: 0,
                        lastDate = existing?.lastDate ?: 0L,
                        hasLastExecution = existing?.hasLastExecution ?: false,
                        skipped = existing?.skipped ?: false,
                        preventive = preventive,
                        notes = existing?.notes ?: ""
                    ))
                }
            }) { Text("Сохранить", color = ColorGreen) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = TextSecondary) }
        }
    )
}

private fun WorkStatus.title(): String = when(this) {
    WorkStatus.COMPLETED -> "Выполнено"
    WorkStatus.REQUIRED -> "Необходимо"
    WorkStatus.PREVENTIVE -> "Рекомендации"
    WorkStatus.SKIPPED -> "Пропущено"
}
