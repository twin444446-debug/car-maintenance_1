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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorScreen(templates: List<RegulationTemplate>, onBack: () -> Unit, onSave: (List<RegulationTemplate>) -> Unit) {
    var list by remember(templates) { mutableStateOf(templates) }
    var show by remember { mutableStateOf(false) }
    var templateToDelete by remember { mutableStateOf<RegulationTemplate?>(null) }
    
    // Состояние для фильтра: 0 - Автомобиль, 1 - ГБО
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Шаблоны регламента", color = TextPrimary, fontSize = 20.sp, style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Назад", color = ColorBlue) }
        }
        
        Spacer(Modifier.height(8.dp))

        // Переключатель между стандартными и ГБО
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                colors = SegmentedButtonDefaults.colors(activeContainerColor = ColorGreen.copy(alpha = 0.2f), activeContentColor = ColorGreen)
            ) { Text("Автомобиль", maxLines = 1) }
            
            SegmentedButton(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                colors = SegmentedButtonDefaults.colors(activeContainerColor = ColorGreen.copy(alpha = 0.2f), activeContentColor = ColorGreen)
            ) { Text("ГБО", maxLines = 1) }
        }

        Spacer(Modifier.height(8.dp))

        // Фильтруем список в зависимости от выбранной вкладки
        val filteredList = list.filter { 
            if (selectedTab == 0) it.category == WorkCategory.GENERAL 
            else it.category == WorkCategory.LPG 
        }

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredList) { t ->
                Card(colors = CardDefaults.cardColors(containerColor = CardColor), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t.name, color = TextPrimary)
                            val groupText = if (t.groupName.isNotBlank()) " • ${t.groupName}" else ""
                            Text("${t.intervalKm} км / ${t.intervalMonths} мес$groupText", color = TextSecondary, fontSize = 12.sp)
                        }
                        
                        // Защита системных шаблонов от удаления
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
        
        Button(onClick = { show = true }, modifier = Modifier.fillMaxWidth()) { 
            Text("Добавить шаблон") 
        }
    }

    // Диалог добавления (с учетом выбранной вкладки)
    if (show) {
        TemplateDialog(null, defaultIsLpg = selectedTab == 1, onDismiss = { show = false }) { 
            list = list + it
            onSave(list)
            show = false 
        }
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
fun TemplateDialog(
    existing: RegulationTemplate?, 
    defaultIsLpg: Boolean = false,
    onDismiss: () -> Unit, 
    onSave: (RegulationTemplate) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var km by remember { mutableStateOf(existing?.intervalKm?.toString() ?: "") }
    var months by remember { mutableStateOf(existing?.intervalMonths?.toString() ?: "") }
    var warningKm by remember { mutableStateOf(existing?.warningKm?.toString() ?: "") }
    var warningMonths by remember { mutableStateOf(existing?.warningMonths?.toString() ?: "") }
    var lpg by remember { mutableStateOf(existing?.category == WorkCategory.LPG || defaultIsLpg) }
    var preventive by remember { mutableStateOf(existing?.preventive ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss, 
        containerColor = CardColor, 
        title = { Text(if (existing == null) "Новый шаблон" else "Редактировать шаблон", color = TextPrimary) },
        text = { 
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(km, { km = it }, label = { Text("Интервал км") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(months, { months = it }, label = { Text("Месяцы") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(warningKm, { warningKm = it }, label = { Text("Предупр. км") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(warningMonths, { warningMonths = it }, label = { Text("Предупр. мес") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) { 
                    Checkbox(lpg, { lpg = it }, enabled = existing == null) // Блокируем смену типа при редактировании
                    Text("Работа по ГБО", color = TextPrimary) 
                }
                Row(verticalAlignment = Alignment.CenterVertically) { 
                    Checkbox(preventive, { preventive = it })
                    Text("Профилактическая рекомендация", color = TextPrimary) 
                }
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
