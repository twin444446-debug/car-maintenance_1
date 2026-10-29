package com.example.carmaintenance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TemplateEditorScreen(templates: List<RegulationTemplate>, onBack: () -> Unit, onSave: (List<RegulationTemplate>) -> Unit) {
    var list by remember(templates) { mutableStateOf(templates) }
    var show by remember { mutableStateOf(false) }
    var templateToDelete by remember { mutableStateOf<RegulationTemplate?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Шаблоны регламента", color = TextPrimary, fontSize = 20.sp, style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Назад", color = ColorBlue) }
        }
        
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list) { t ->
                Card(colors = CardDefaults.cardColors(containerColor = CardColor), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t.name, color = TextPrimary)
                            Text("${if(t.category==WorkCategory.LPG) "ГБО" else "Автомобиль"} • ${t.intervalKm} км / ${t.intervalMonths} мес", color = TextSecondary, fontSize = 12.sp)
                        }
                        
                        // Если шаблон системный — показываем замок, иначе кнопку удаления
                        if (t.isSystem) {
                            Icon(Icons.Default.Lock, contentDescription = "Системный шаблон", tint = TextSecondary.copy(alpha = 0.5f))
                        } else {
                            TextButton(onClick = { templateToDelete = t }) { 
                                Text("Удалить", color = ColorRed) 
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { show = true }, modifier = Modifier.fillMaxWidth()) { Text("Добавить шаблон") }
    }

    if (show) {
        TemplateDialog(null, { show = false }) { list = list + it; onSave(list); show = false }
    }

    // Диалог подтверждения удаления
    if (templateToDelete != null) {
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            containerColor = CardColor,
            title = { Text("Удалить шаблон?", color = TextPrimary) },
            text = { Text("Вы уверены, что хотите удалить шаблон \"${templateToDelete!!.name}\"?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    list = list.filter { it.id != templateToDelete!!.id }
                    onSave(list)
                    templateToDelete = null
                }) { Text("Удалить", color = ColorRed, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { templateToDelete = null }) { Text("Отмена", color = TextSecondary) }
            }
        )
    }
}

@Composable
fun TemplateDialog(existing: RegulationTemplate?, onDismiss: () -> Unit, onSave: (RegulationTemplate) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var km by remember { mutableStateOf(existing?.intervalKm?.toString() ?: "") }
    var months by remember { mutableStateOf(existing?.intervalMonths?.toString() ?: "") }
    var warningKm by remember { mutableStateOf(existing?.warningKm?.toString() ?: "") }
    var warningMonths by remember { mutableStateOf(existing?.warningMonths?.toString() ?: "") }
    var lpg by remember { mutableStateOf(existing?.category == WorkCategory.LPG) }
    var preventive by remember { mutableStateOf(existing?.preventive ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss, 
        containerColor = CardColor, 
        title = { Text("Шаблон работы", color = TextPrimary) },
        text = { 
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(km, { km = it }, label = { Text("Интервал км") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(months, { months = it }, label = { Text("Месяцы") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(warningKm, { warningKm = it }, label = { Text("Предупр. км") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(warningMonths, { warningMonths = it }, label = { Text("Предупр. мес") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Row { Checkbox(lpg, { lpg = it }); Text("Работа по ГБО", color = TextPrimary) }
                Row { Checkbox(preventive, { preventive = it }); Text("Профилактическая рекомендация", color = TextPrimary) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onSave(
                        existing?.copy(
                            name = name, 
                            category = if (lpg) WorkCategory.LPG else WorkCategory.GENERAL,
                            intervalKm = km.toIntOrNull() ?: 0, 
                            intervalMonths = months.toIntOrNull() ?: 0,
                            warningKm = warningKm.toIntOrNull() ?: 0, 
                            warningMonths = warningMonths.toIntOrNull() ?: 0,
                            preventive = preventive
                        ) ?: RegulationTemplate(
                            name = name, 
                            category = if (lpg) WorkCategory.LPG else WorkCategory.GENERAL,
                            intervalKm = km.toIntOrNull() ?: 0, 
                            intervalMonths = months.toIntOrNull() ?: 0,
                            warningKm = warningKm.toIntOrNull() ?: 0, 
                            warningMonths = warningMonths.toIntOrNull() ?: 0,
                            preventive = preventive,
                            isSystem = false
                        )
                    )
                }
            }) { Text("Сохранить", color = ColorGreen) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = TextSecondary) } }
    )
}
