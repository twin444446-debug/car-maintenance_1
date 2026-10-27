package com.example.carmaintenance

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@Composable
fun AddCarDialog(existingCar: Car?, onDismiss: () -> Unit, onSave: (Car) -> Unit) {
    var brand by remember { mutableStateOf(existingCar?.brand ?: "") }; var model by remember { mutableStateOf(existingCar?.model ?: "") }
    var year by remember { mutableStateOf(existingCar?.year ?: "") }; var vin by remember { mutableStateOf(existingCar?.vin ?: "") }
    var mileage by remember { mutableStateOf(existingCar?.mileage ?: "") }; var engine by remember { mutableStateOf(existingCar?.engine ?: "") }
    var hasLpg by remember { mutableStateOf(existingCar?.hasLpg ?: false) }; var notes by remember { mutableStateOf(existingCar?.notes ?: "") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = CardColor, title = { Text(if (existingCar == null) "Добавить автомобиль" else "Изменить автомобиль", color = TextPrimary) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            field("Марка *", brand) { brand = it }; field("Модель *", model) { model = it }; field("Год выпуска", year, KeyboardType.Number) { year = it }
            field("Пробег (км)", mileage, KeyboardType.Number) { mileage = it }; field("VIN-код автомобиля", vin) { vin = it }; field("Двигатель", engine) { engine = it }
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(hasLpg, { hasLpg = it }); Text("Установлено ГБО", color = TextPrimary) }
            field("Заметки", notes) { notes = it }
        }},
        confirmButton = { TextButton(onClick = { if (brand.isNotBlank() && model.isNotBlank()) onSave(existingCar?.copy(brand=brand,model=model,year=year,vin=vin,mileage=mileage,engine=engine,fuel=if(hasLpg) "Бензин/Газ" else "Бензин",hasLpg=hasLpg,notes=notes) ?: Car(brand=brand,model=model,year=year,vin=vin,mileage=mileage,engine=engine,fuel=if(hasLpg) "Бензин/Газ" else "Бензин",hasLpg=hasLpg,notes=notes)) }) { Text("Сохранить", color = ColorGreen) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = TextSecondary) } })
}

@Composable 
private fun field(
    label: String, 
    value: String, 
    type: KeyboardType = KeyboardType.Text, 
    onValue: (String) -> Unit
) = OutlinedTextField(
    value = value,
    onValueChange = onValue,
    label = { Text(label) },
    keyboardOptions = KeyboardOptions(keyboardType = type),
    singleLine = label != "Заметки",
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
fun MileageDialog(current: String, onDismiss:()->Unit, onSave:(String)->Unit) { var v by remember { mutableStateOf(current) }; AlertDialog(onDismissRequest=onDismiss,containerColor=CardColor,title={Text("Текущий пробег",color=TextPrimary)},text={field("Пробег (км)",v,KeyboardType.Number){v=it}},confirmButton={TextButton(onClick={onSave(v)}){Text("Сохранить",color=ColorGreen)}},dismissButton={TextButton(onClick=onDismiss){Text("Отмена",color=TextSecondary)}}) }

@Composable
fun ServiceRecordDialog(car: Car, initial: ServiceRecord? = null, suggestedName: String = "", parts: List<PartCatalogItem>, onDismiss:()->Unit, onSave:(ServiceRecord)->Unit) {
    var date by remember { mutableStateOf(initial?.date ?: System.currentTimeMillis()) }; var place by remember { mutableStateOf(initial?.servicePlace ?: "") }
    var reason by remember { mutableStateOf(initial?.reason ?: "") }; var mileage by remember { mutableStateOf((initial?.mileage ?: car.mileage.toIntOrNull() ?: 0).toString()) }; var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var lines by remember { mutableStateOf(initial?.lines ?: listOf(ServiceLine(name=suggestedName))) }
    val context = androidx.compose.ui.platform.LocalContext.current
    AlertDialog(onDismissRequest=onDismiss,containerColor=CardColor,title={Text(if(initial==null)"Новая запись обслуживания" else "Изменить запись",color=TextPrimary)},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Дата: ${formatDate(date)}",color=TextPrimary); Button(onClick={val c=Calendar.getInstance().apply{timeInMillis=date};DatePickerDialog(context,{_,y,m,d->date=Calendar.getInstance().apply{set(y,m,d,12,0,0)}.timeInMillis},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show()},colors=ButtonDefaults.buttonColors(containerColor=NavColor)){Text("Изменить дату")}
            field("СТО / магазин",place){place=it}; field("Причина",reason){reason=it}; field("Пробег (км)",mileage,KeyboardType.Number){mileage=it}; field("Примечания",notes){notes=it}
            Text("Работы и запчасти",color=TextPrimary)
            lines.forEachIndexed { i,l -> LineEditor(l,onChange={x->lines=lines.toMutableList().also{it[i]=x}},onDelete={lines=lines.toMutableList().also{it.removeAt(i)}}) }
            TextButton(onClick={lines=lines+ServiceLine(name="")}){Text("+ Добавить работу / запчасть",color=ColorGreen)}
            if(parts.isNotEmpty()) Text("Сохранённые запчасти можно повторно выбрать в перечне работ.",color=TextSecondary)
        }
    },confirmButton={TextButton(onClick={onSave(ServiceRecord(id=initial?.id ?: java.util.UUID.randomUUID().toString(),carId=car.id,workId=initial?.workId,date=date,servicePlace=place,reason=reason,mileage=mileage.toIntOrNull()?:0,lines=lines.filter{it.name.isNotBlank()},notes=notes))}){Text("Сохранить",color=ColorGreen)}},dismissButton={TextButton(onClick=onDismiss){Text("Отмена",color=TextSecondary)}})
}

@Composable private fun LineEditor(line: ServiceLine,onChange:(ServiceLine)->Unit,onDelete:()->Unit){Column(verticalArrangement=Arrangement.spacedBy(6.dp)){field("Название",line.name){onChange(line.copy(name=it))};Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){field("Артикул",line.article){onChange(line.copy(article=it))};field("Стоимость",if(line.cost==0.0)"" else line.cost.toString(),KeyboardType.Decimal){onChange(line.copy(cost=it.toDoubleOrNull()?:0.0))}};Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){field("Валюта",line.currency){onChange(line.copy(currency=it.uppercase()))};field("Тип масла",line.oilType){onChange(line.copy(oilType=it))}};field("Название масла",line.oilName){onChange(line.copy(oilName=it))};TextButton(onClick=onDelete){Text("Удалить строку",color=ColorRed)}}}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkActionsDialog(
    work: MaintenanceWork,
    car: Car,
    records: List<ServiceRecord>,
    onDismiss: () -> Unit,
    onComplete: (km: Int, date: Long, note: String, lines: List<ServiceLine>) -> Unit,
    onSkip: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShowInList: () -> Unit
) {
    var showCompleteForm by remember { mutableStateOf(false) }
    var km by remember { mutableStateOf(car.mileage.ifBlank { "0" }) }
    var note by remember { mutableStateOf("") }
    
    val lines = remember { mutableStateListOf(ServiceLine(name = work.name, type = ServiceLineType.WORK)) }

    if (!showCompleteForm) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = CardColor,
            title = { Text(work.name, color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Выполнено:", color = TextSecondary)
                        Text(if (work.hasLastExecution) "${work.lastMileage} км / ${formatDate(work.lastDate)}" else "Не выполнялось", color = ColorGreen, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Следующая замена:", color = TextSecondary)
                        val nextMileage = if (work.hasLastExecution) work.lastMileage + work.intervalKm else (car.mileage.toIntOrNull() ?: 0) + work.intervalKm
                        val monthsText = if (work.intervalMonths > 0) " / ${work.intervalMonths} мес." else ""
                        Text("$nextMileage км$monthsText", color = ColorRed, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = { Button(onClick = { showCompleteForm = true }, colors = ButtonDefaults.buttonColors(containerColor = ColorGreen)) { Text("Выполнить", color = Color.White, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = TextSecondary) } }
        )
    } else {
        AlertDialog(
            onDismissRequest = { showCompleteForm = false },
            containerColor = CardColor,
            title = { Text("Выполнение: ${work.name}", color = TextPrimary, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = km, onValueChange = { km = it }, label = { Text("Пробег (км)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = ColorGreen, unfocusedBorderColor = TextSecondary, focusedLabelColor = ColorGreen, unfocusedLabelColor = TextSecondary, cursorColor = ColorGreen)
                    )
                    Text("Работы и материалы", color = TextPrimary, fontWeight = FontWeight.Bold)
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
                    }
                    Button(onClick = { lines.add(ServiceLine(name = "", type = ServiceLineType.WORK)) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ColorBlue.copy(alpha = 0.2f))) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = ColorBlue); Spacer(Modifier.width(8.dp)); Text("Добавить строку", color = ColorBlue)
                    }
                    OutlinedTextField(
                        value = note, onValueChange = { note = it }, label = { Text("Примечание ко всей записи") }, modifier = Modifier.fillMaxWidth(), minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = ColorGreen, unfocusedBorderColor = TextSecondary, focusedLabelColor = ColorGreen, unfocusedLabelColor = TextSecondary, cursorColor = ColorGreen)
                    )
                }
            },
            confirmButton = { TextButton(onClick = { onComplete(km.toIntOrNull() ?: 0, System.currentTimeMillis(), note, lines.toList()) }) { Text("Сохранить", color = ColorGreen, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { showCompleteForm = false }) { Text("Назад", color = TextSecondary) } }
        )
    }
}

@Composable
fun CompletedWorkInfoDialog(
    work: MaintenanceWork,
    car: Car,
    record: ServiceRecord,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardColor,
        title = { Text(work.name, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
fun RegulationWorkDialog(carId:String, existing:MaintenanceWork?, templates:List<RegulationTemplate>, onDismiss:()->Unit, onSave:(MaintenanceWork)->Unit){
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: WorkCategory.GENERAL) }
    var km by remember { mutableStateOf(existing?.intervalKm?.toString() ?: "") }
    var months by remember { mutableStateOf(existing?.intervalMonths?.toString() ?: "") }
    var warningKm by remember { mutableStateOf(existing?.warningKm?.toString() ?: "") }
    var warningMonths by remember { mutableStateOf(existing?.warningMonths?.toString() ?: "") }
    var preventive by remember { mutableStateOf(existing?.preventive ?: false) }
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = ColorBlue, unfocusedBorderColor = TextSecondary, focusedLabelColor = ColorBlue, unfocusedLabelColor = TextSecondary, cursorColor = ColorBlue)
    AlertDialog(onDismissRequest=onDismiss,containerColor=CardColor,title={Text(if(existing==null)"Добавить регламентную работу" else "Изменить регламентную работу",color=TextPrimary)},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
        OutlinedTextField(name,{name=it},label={Text("Название")},singleLine=true,modifier=Modifier.fillMaxWidth(), colors = fieldColors)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){FilterChip(selected=category==WorkCategory.GENERAL,onClick={category=WorkCategory.GENERAL},label={Text("Автомобиль")});FilterChip(selected=category==WorkCategory.LPG,onClick={category=WorkCategory.LPG},label={Text("ГБО")})}
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(km,{km=it},label={Text("Интервал км")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.weight(1f), colors = fieldColors);OutlinedTextField(months,{months=it},label={Text("Интервал мес.")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.weight(1f), colors = fieldColors)}
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(warningKm,{warningKm=it},label={Text("Предупреждение км")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.weight(1f), colors = fieldColors);OutlinedTextField(warningMonths,{warningMonths=it},label={Text("Предупреждение мес.")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.weight(1f), colors = fieldColors)}
        Row(verticalAlignment=Alignment.CenterVertically){Checkbox(preventive,{preventive=it});Text("Профилактическая рекомендация",color=TextPrimary)}
        Text("Можно создать работу без интервала — например, для проверки состояния детали. Её статус будет профилактическим.",color=TextSecondary,fontSize=12.sp)
    }},confirmButton={TextButton(onClick={if(name.isNotBlank())onSave(MaintenanceWork(id=existing?.id?:java.util.UUID.randomUUID().toString(),carId=carId,templateId=existing?.templateId?:(templates.firstOrNull{it.name==name}?.id?:""),name=name,category=category,intervalKm=km.toIntOrNull()?:0,intervalMonths=months.toIntOrNull()?:0,warningKm=warningKm.toIntOrNull()?:0,warningMonths=warningMonths.toIntOrNull()?:0,lastMileage=existing?.lastMileage?:0,lastDate=existing?.lastDate?:0L,hasLastExecution=existing?.hasLastExecution?:false,skipped=existing?.skipped?:false,preventive=preventive,notes=existing?.notes?:""))}){Text("Сохранить",color=ColorGreen)}},dismissButton={TextButton(onClick=onDismiss){Text("Отмена",color=TextSecondary)}})
}
