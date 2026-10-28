package com.example.carmaintenance

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupScreen(
    cars: List<Car>,
    works: List<MaintenanceWork>,
    records: List<ServiceRecord>,
    parts: List<PartCatalogItem>,
    templates: List<RegulationTemplate>,
    onBack: () -> Unit,
    onImported: () -> Unit
) {
    val context = LocalContext.current
    var backups by remember { mutableStateOf(listLocalBackups(context)) }
    var status by remember { mutableStateOf<BackupResult?>(null) }
    var pendingRestore by remember { mutableStateOf<File?>(null) }

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val json = buildBackupJson(cars, works, records, parts, templates)
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                status = BackupResult(true, "Файл сохранён.")
            } catch (e: Exception) {
                status = BackupResult(false, "Ошибка экспорта: ${e.message}")
            }
        }
    }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (json != null) {
                    status = importBackupJson(context, json)
                    if (status?.success == true) onImported()
                } else {
                    status = BackupResult(false, "Файл пуст.")
                }
            } catch (e: Exception) {
                status = BackupResult(false, "Ошибка импорта: ${e.message}")
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = ColorBlue) }
            Text("Резервная копия", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { createLauncher.launch("backup_${System.currentTimeMillis()}.json") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Экспортировать в файл") }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { openLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ColorBlue.copy(alpha = 0.2f))
        ) { Text("Импортировать из файла", color = ColorBlue) }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                val json = buildBackupJson(cars, works, records, parts, templates)
                createLocalBackup(context, json)
                backups = listLocalBackups(context)
                status = BackupResult(true, "Локальная копия создана.")
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Создать локальную копию сейчас") }

        status?.let { s ->
            Spacer(Modifier.height(8.dp))
            Text(
                s.message,
                color = if (s.success) ColorGreen else ColorRed,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("Локальные копии (${backups.size}/10)", color = TextPrimary, fontWeight = FontWeight.Bold)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(backups) { f ->
                Card(colors = CardDefaults.cardColors(containerColor = CardColor)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).format(Date(f.lastModified())),
                                color = TextPrimary, fontWeight = FontWeight.Medium
                            )
                            Text("${f.length() / 1024} КБ", color = TextSecondary, fontSize = 12.sp)
                        }
                        TextButton(onClick = { pendingRestore = f }) { Text("Восстановить", color = ColorYellow) }
                    }
                }
            }
        }
    }

    if (pendingRestore != null) {
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            containerColor = CardColor,
            title = { Text("Восстановить из копии?", color = TextPrimary) },
            text = { Text("Текущие данные будут заменены содержимым копии от ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).format(Date(pendingRestore!!.lastModified()))}.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    status = restoreFromLocalBackup(context, pendingRestore!!)
                    if (status?.success == true) onImported()
                    pendingRestore = null
                }) { Text("Восстановить", color = ColorGreen) }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Отмена", color = TextSecondary) } }
        )
    }
}
