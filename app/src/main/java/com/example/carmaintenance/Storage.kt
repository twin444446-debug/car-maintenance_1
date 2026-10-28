package com.example.carmaintenance

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

private const val PREFS = "car_maintenance"
private const val CARS = "cars"
private const val WORKS = "works_v2"
private const val TEMPLATES = "templates_v2"
private const val RECORDS = "service_records_v2"
private const val PARTS = "parts_v2"
private const val LEGACY_HISTORY = "history"
private const val MIGRATED = "logic_migrated_v2"

private fun Context.prefs() = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

fun loadCars(context: Context): List<Car> {
    val a = runCatching { JSONArray(context.prefs().getString(CARS, "[]")) }.getOrElse { JSONArray() }
    return buildList {
        for (i in 0 until a.length()) {
            val o = a.optJSONObject(i) ?: continue
            add(Car(
                id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                brand = o.optString("brand"), model = o.optString("model"), year = o.optString("year"),
                vin = o.optString("vin"), mileage = o.optString("mileage"), engine = o.optString("engine"),
                fuel = o.optString("fuel"), hasLpg = o.optBoolean("hasLpg"), notes = o.optString("notes"),
                imageUri = o.optString("imageUri")
            ))
        }
    }
}

fun saveCars(context: Context, cars: List<Car>) {
    val a = JSONArray()
    cars.forEach { c -> a.put(JSONObject().apply {
        put("id", c.id); put("brand", c.brand); put("model", c.model); put("year", c.year); put("vin", c.vin)
        put("mileage", c.mileage); put("engine", c.engine); put("fuel", c.fuel); put("hasLpg", c.hasLpg)
        put("notes", c.notes); put("imageUri", c.imageUri)
    }) }
    context.prefs().edit().putString(CARS, a.toString()).apply()
}

fun loadTemplates(context: Context): List<RegulationTemplate> {
    val a = runCatching { JSONArray(context.prefs().getString(TEMPLATES, "[]")) }.getOrElse { JSONArray() }
    return buildList {
        for (i in 0 until a.length()) {
            val o = a.optJSONObject(i) ?: continue
            add(RegulationTemplate(
                id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                name = o.optString("name"),
                groupName = o.optString("groupName", ""), // <-- ДОБАВЛЕНО
                category = runCatching { WorkCategory.valueOf(o.optString("category", "GENERAL")) }.getOrDefault(WorkCategory.GENERAL),
                intervalKm = o.optInt("intervalKm"), intervalMonths = o.optInt("intervalMonths"),
                warningKm = o.optInt("warningKm"), warningMonths = o.optInt("warningMonths"),
                preventive = o.optBoolean("preventive"), notes = o.optString("notes")
            ))
        }
    }
}

fun saveTemplates(context: Context, templates: List<RegulationTemplate>) {
    val a = JSONArray()
    templates.forEach { t -> a.put(JSONObject().apply {
        put("id", t.id); put("name", t.name); 
        put("groupName", t.groupName) // <-- ДОБАВЛЕНО
        put("category", t.category.name); put("intervalKm", t.intervalKm)
        put("intervalMonths", t.intervalMonths); put("warningKm", t.warningKm); put("warningMonths", t.warningMonths)
        put("preventive", t.preventive); put("notes", t.notes)
    }) }
    context.prefs().edit().putString(TEMPLATES, a.toString()).apply()
}

fun loadWorks(context: Context): List<MaintenanceWork> {
    migrateIfNeeded(context)
    val a = runCatching { JSONArray(context.prefs().getString(WORKS, "[]")) }.getOrElse { JSONArray() }
    return buildList {
        for (i in 0 until a.length()) {
            val o = a.optJSONObject(i) ?: continue
            add(MaintenanceWork(
                id = o.optString("id").ifBlank { UUID.randomUUID().toString() }, carId = o.optString("carId"),
                templateId = o.optString("templateId"), name = o.optString("name"),
                groupName = o.optString("groupName", ""), // <-- ДОБАВЛЕНО
                category = runCatching { WorkCategory.valueOf(o.optString("category", "GENERAL")) }.getOrDefault(if (o.optString("name").startsWith("ГБО:")) WorkCategory.LPG else WorkCategory.GENERAL),
                intervalKm = o.optInt("intervalKm"), intervalMonths = o.optInt("intervalMonths"),
                warningKm = o.optInt("warningKm"), warningMonths = o.optInt("warningMonths"),
                lastMileage = o.optInt("lastMileage"), lastDate = o.optLong("lastDate"),
                hasLastExecution = o.optBoolean("hasLastExecution"), skipped = o.optBoolean("skipped"),
                preventive = o.optBoolean("preventive"), notes = o.optString("notes")
            ))
        }
    }
}

fun saveWorks(context: Context, works: List<MaintenanceWork>) {
    val a = JSONArray()
    works.forEach { w -> a.put(JSONObject().apply {
        put("id", w.id); put("carId", w.carId); put("templateId", w.templateId); put("name", w.name); 
        put("groupName", w.groupName) // <-- ДОБАВЛЕНО
        put("category", w.category.name)
        put("intervalKm", w.intervalKm); put("intervalMonths", w.intervalMonths); put("warningKm", w.warningKm); put("warningMonths", w.warningMonths)
        put("lastMileage", w.lastMileage); put("lastDate", w.lastDate); put("hasLastExecution", w.hasLastExecution)
        put("skipped", w.skipped); put("preventive", w.preventive); put("notes", w.notes)
    }) }
    context.prefs().edit().putString(WORKS, a.toString()).apply()
}

fun loadServiceRecords(context: Context): List<ServiceRecord> {
    migrateIfNeeded(context)
    val a = runCatching { JSONArray(context.prefs().getString(RECORDS, "[]")) }.getOrElse { JSONArray() }
    return buildList {
        for (i in 0 until a.length()) {
            val o = a.optJSONObject(i) ?: continue
            val lines = buildList {
                val la = o.optJSONArray("lines") ?: JSONArray()
                for (j in 0 until la.length()) {
                    val x = la.optJSONObject(j) ?: continue
                    add(ServiceLine(
                        id = x.optString("id").ifBlank { UUID.randomUUID().toString() },
                        type = runCatching { ServiceLineType.valueOf(x.optString("type", "WORK")) }.getOrDefault(ServiceLineType.WORK),
                        name = x.optString("name"), article = x.optString("article"), cost = x.optDouble("cost"),
                        currency = x.optString("currency", "RUB"), oilType = x.optString("oilType"), oilName = x.optString("oilName"), notes = x.optString("notes")
                    ))
                }
            }
            add(ServiceRecord(
                id = o.optString("id").ifBlank { UUID.randomUUID().toString() }, carId = o.optString("carId"),
                workId = o.optString("workId").takeIf { it.isNotBlank() },
                date = o.optLong("date", System.currentTimeMillis()), servicePlace = o.optString("servicePlace"),
                reason = o.optString("reason"), mileage = o.optInt("mileage"), lines = lines, notes = o.optString("notes")
            ))
        }
    }
}

fun saveServiceRecords(context: Context, records: List<ServiceRecord>) {
    val a = JSONArray()
    records.forEach { r -> a.put(JSONObject().apply {
        put("id", r.id); put("carId", r.carId)
        put("workId", r.workId)
        put("date", r.date); put("servicePlace", r.servicePlace); put("reason", r.reason); put("mileage", r.mileage); put("notes", r.notes)
        put("lines", JSONArray().apply { r.lines.forEach { l -> put(JSONObject().apply {
            put("id", l.id); put("type", l.type.name); put("name", l.name); put("article", l.article); put("cost", l.cost); put("currency", l.currency); put("oilType", l.oilType); put("oilName", l.oilName); put("notes", l.notes)
        }) } })
    }) }
    context.prefs().edit().putString(RECORDS, a.toString()).apply()
}

fun loadPartCatalog(context: Context): List<PartCatalogItem> {
    val a = runCatching { JSONArray(context.prefs().getString(PARTS, "[]")) }.getOrElse { JSONArray() }
    return buildList {
        for (i in 0 until a.length()) {
            val o = a.optJSONObject(i) ?: continue
            add(PartCatalogItem(
                id = o.optString("id").ifBlank { UUID.randomUUID().toString() }, carId = o.optString("carId"),
                name = o.optString("name"), article = o.optString("article"), category = o.optString("category", "Запчасть"),
                lastCost = o.optDouble("lastCost"), currency = o.optString("currency", "RUB")
            ))
        }
    }
}

fun savePartCatalog(context: Context, parts: List<PartCatalogItem>) {
    val a = JSONArray()
    parts.forEach { p -> a.put(JSONObject().apply {
        put("id", p.id); put("carId", p.carId); put("name", p.name); put("article", p.article); put("category", p.category); put("lastCost", p.lastCost); put("currency", p.currency)
    }) }
    context.prefs().edit().putString(PARTS, a.toString()).apply()
}

fun loadHistory(context: Context): List<WorkHistory> = loadServiceRecords(context).flatMap { r ->
    r.lines.filter { it.type == ServiceLineType.WORK || it.type == ServiceLineType.CONSUMABLE || it.type == ServiceLineType.PART }
        .map { WorkHistory(it.id, r.carId, it.name, r.date, r.mileage, it.notes.ifBlank { r.reason }) }
}

fun loadDefaultTemplate(context: Context): List<MaintenanceWork> = loadTemplates(context).map {
    MaintenanceWork(carId = "", templateId = it.id, name = it.name, groupName = it.groupName, category = it.category, intervalKm = it.intervalKm, intervalMonths = it.intervalMonths, warningKm = it.warningKm, warningMonths = it.warningMonths, preventive = it.preventive, notes = it.notes)
}

fun saveDefaultTemplate(context: Context, works: List<MaintenanceWork>) {
    saveTemplates(context, works.map { RegulationTemplate(it.templateId.ifBlank { it.id }, it.name, it.groupName, it.category, it.intervalKm, it.intervalMonths, it.warningKm, it.warningMonths, it.preventive, it.notes) })
}

private fun migrateIfNeeded(context: Context) {
    val p = context.prefs()
    if (p.getBoolean(MIGRATED, false)) return

    val oldWorks = runCatching { JSONArray(p.getString("works", "[]")) }.getOrElse { JSONArray() }
    val oldHistory = runCatching { JSONArray(p.getString(LEGACY_HISTORY, "[]")) }.getOrElse { JSONArray() }
    val oldDefault = runCatching { JSONArray(p.getString("default_template", "[]")) }.getOrElse { JSONArray() }

    val templates = loadTemplates(context).toMutableList()
    fun categoryFor(name: String, raw: JSONObject? = null): WorkCategory {
        val stored = raw?.optString("category").orEmpty()
        return runCatching { WorkCategory.valueOf(stored) }.getOrNull()
            ?: if (name.startsWith("ГБО:")) WorkCategory.LPG else WorkCategory.GENERAL
    }
    fun addTemplate(name: String, category: WorkCategory, id: String, km: Int, months: Int,
                    warningKm: Int, warningMonths: Int, preventive: Boolean, notes: String, groupName: String = "") {
        if (name.isBlank()) return
        val existing = templates.firstOrNull { it.name.trim().equals(name.trim(), ignoreCase = true) && it.category == category }
        if (existing == null) templates.add(RegulationTemplate(
            id = id.ifBlank { UUID.randomUUID().toString() }, name = name.trim(), groupName = groupName, category = category,
            intervalKm = km, intervalMonths = months, warningKm = warningKm,
            warningMonths = warningMonths, preventive = preventive, notes = notes
        ))
    }

    for (i in 0 until oldDefault.length()) {
        val o = oldDefault.optJSONObject(i) ?: continue
        val name = o.optString("name")
        addTemplate(name, categoryFor(name, o), o.optString("id"), o.optInt("intervalKm"),
            o.optInt("intervalMonths"), o.optInt("warningKm"), o.optInt("warningMonths"),
            o.optBoolean("preventive"), o.optString("notes"), o.optString("groupName", ""))
    }
    for (i in 0 until oldWorks.length()) {
        val o = oldWorks.optJSONObject(i) ?: continue
        val name = o.optString("name")
        addTemplate(name, categoryFor(name, o), o.optString("templateId").ifBlank { o.optString("id") },
            o.optInt("intervalKm"), o.optInt("intervalMonths"), o.optInt("warningKm"),
            o.optInt("warningMonths"), o.optBoolean("preventive"), o.optString("notes"), o.optString("groupName", ""))
    }

    val migratedWorks = mutableListOf<MaintenanceWork>()
    for (i in 0 until oldWorks.length()) {
        val o = oldWorks.optJSONObject(i) ?: continue
        val name = o.optString("name")
        val category = categoryFor(name, o)
        val template = templates.firstOrNull {
            it.name.trim().equals(name.trim(), ignoreCase = true) && it.category == category
        }
        migratedWorks.add(MaintenanceWork(
            id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
            carId = o.optString("carId"), templateId = template?.id ?: "", name = name,
            groupName = o.optString("groupName", ""), // <-- ДОБАВЛЕНО
            category = category, intervalKm = o.optInt("intervalKm"), intervalMonths = o.optInt("intervalMonths"),
            warningKm = o.optInt("warningKm"), warningMonths = o.optInt("warningMonths"),
            lastMileage = o.optInt("lastMileage"), lastDate = o.optLong("lastDate"),
            hasLastExecution = o.optBoolean("hasLastExecution"), skipped = o.optBoolean("skipped"),
            preventive = o.optBoolean("preventive"), notes = o.optString("notes")
        ))
    }

    val existingRecords = runCatching { JSONArray(p.getString(RECORDS, "[]")) }.getOrElse { JSONArray() }
    val records = mutableListOf<ServiceRecord>()
    val existingLegacyIds = mutableSetOf<String>()
    for (i in 0 until existingRecords.length()) {
        val o = existingRecords.optJSONObject(i) ?: continue
        val recordId = o.optString("id").ifBlank { UUID.randomUUID().toString() }
        val lines = mutableListOf<ServiceLine>()
        val lineArray = o.optJSONArray("lines") ?: JSONArray()
        for (j in 0 until lineArray.length()) {
            val x = lineArray.optJSONObject(j) ?: continue
            lines.add(ServiceLine(
                id = x.optString("id").ifBlank { UUID.randomUUID().toString() },
                type = runCatching { ServiceLineType.valueOf(x.optString("type", "WORK")) }.getOrDefault(ServiceLineType.WORK),
                name = x.optString("name"), article = x.optString("article"), cost = x.optDouble("cost"),
                currency = x.optString("currency", "RUB"), oilType = x.optString("oilType"),
                oilName = x.optString("oilName"), notes = x.optString("notes")
            ))
        }
        records.add(ServiceRecord(id = recordId, carId = o.optString("carId"),
            workId = o.optString("workId").takeIf { it.isNotBlank() },
            date = o.optLong("date", 0L),
            servicePlace = o.optString("servicePlace"), reason = o.optString("reason"), mileage = o.optInt("mileage"),
            lines = lines, notes = o.optString("notes")))
        existingLegacyIds.add(recordId)
    }
    for (i in 0 until oldHistory.length()) {
        val o = oldHistory.optJSONObject(i) ?: continue
        val legacyId = o.optString("id").ifBlank { "legacy-history-$i" }
        if (!existingLegacyIds.add(legacyId)) continue
        val name = o.optString("workName").ifBlank { "Работа из старого журнала" }
        records.add(ServiceRecord(
            id = legacyId, carId = o.optString("carId"), date = o.optLong("date", 0L),
            mileage = o.optInt("mileage"), reason = o.optString("notes"),
            lines = listOf(ServiceLine(id = "${legacyId}-line", name = name, type = ServiceLineType.WORK)),
            notes = o.optString("notes")
        ))
    }

    if (templates.isNotEmpty()) saveTemplates(context, templates)
    if (oldWorks.length() > 0) saveWorks(context, migratedWorks)
    if (oldHistory.length() > 0 || existingRecords.length() > 0) saveServiceRecords(context, records)
    p.edit().putBoolean(MIGRATED, true).apply()
}

// ==================== РЕЗЕРВНОЕ КОПИРОВАНИЕ ====================

data class BackupResult(val success: Boolean, val message: String)

private const val BACKUP_DIR = "backups"
private const val BACKUP_PREFIX = "backup_"
private const val BACKUP_SUFFIX = ".json"
private const val MAX_BACKUPS = 10

private fun Context.backupDir(): File {
    val dir = File(filesDir, BACKUP_DIR)
    if (!dir.exists()) dir.mkdirs()
    return dir
}

fun buildBackupJson(
    cars: List<Car>,
    works: List<MaintenanceWork>,
    records: List<ServiceRecord>,
    parts: List<PartCatalogItem>,
    templates: List<RegulationTemplate>
): String {
    val root = JSONObject()
    root.put("version", 1)
    root.put("exportedAt", System.currentTimeMillis())
    root.put("cars", carsToJson(cars))
    root.put("works", worksToJson(works))
    root.put("records", recordsToJson(records))
    root.put("parts", partsToJson(parts))
    root.put("templates", templatesToJson(templates))
    return root.toString(2)
}

private fun carsToJson(cars: List<Car>): JSONArray = JSONArray().also { a ->
    cars.forEach { c -> a.put(JSONObject().apply {
        put("id", c.id); put("brand", c.brand); put("model", c.model); put("year", c.year); put("vin", c.vin)
        put("mileage", c.mileage); put("engine", c.engine); put("fuel", c.fuel); put("hasLpg", c.hasLpg)
        put("notes", c.notes); put("imageUri", c.imageUri)
    }) }
}

private fun worksToJson(works: List<MaintenanceWork>): JSONArray = JSONArray().also { a ->
    works.forEach { w -> a.put(JSONObject().apply {
        put("id", w.id); put("carId", w.carId); put("templateId", w.templateId); put("name", w.name); 
        put("groupName", w.groupName) // <-- ДОБАВЛЕНО
        put("category", w.category.name)
        put("intervalKm", w.intervalKm); put("intervalMonths", w.intervalMonths); put("warningKm", w.warningKm); put("warningMonths", w.warningMonths)
        put("lastMileage", w.lastMileage); put("lastDate", w.lastDate); put("hasLastExecution", w.hasLastExecution)
        put("skipped", w.skipped); put("preventive", w.preventive); put("notes", w.notes)
    }) }
}

private fun recordsToJson(records: List<ServiceRecord>): JSONArray = JSONArray().also { a ->
    records.forEach { r -> a.put(JSONObject().apply {
        put("id", r.id); put("carId", r.carId); put("workId", r.workId)
        put("date", r.date); put("servicePlace", r.servicePlace); put("reason", r.reason); put("mileage", r.mileage); put("notes", r.notes)
        put("lines", JSONArray().apply { r.lines.forEach { l -> put(JSONObject().apply {
            put("id", l.id); put("type", l.type.name); put("name", l.name); put("article", l.article); put("cost", l.cost)
            put("currency", l.currency); put("oilType", l.oilType); put("oilName", l.oilName); put("notes", l.notes)
        }) } })
    }) }
}

private fun partsToJson(parts: List<PartCatalogItem>): JSONArray = JSONArray().also { a ->
    parts.forEach { p -> a.put(JSONObject().apply {
        put("id", p.id); put("carId", p.carId); put("name", p.name); put("article", p.article)
        put("category", p.category); put("lastCost", p.lastCost); put("currency", p.currency)
    }) }
}

private fun templatesToJson(templates: List<RegulationTemplate>): JSONArray = JSONArray().also { a ->
    templates.forEach { t -> a.put(JSONObject().apply {
        put("id", t.id); put("name", t.name); put("groupName", t.groupName) // <-- ДОБАВЛЕНО
        put("category", t.category.name); put("intervalKm", t.intervalKm)
        put("intervalMonths", t.intervalMonths); put("warningKm", t.warningKm); put("warningMonths", t.warningMonths)
        put("preventive", t.preventive); put("notes", t.notes)
    }) }
}

fun createLocalBackup(context: Context, json: String): File {
    val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
    val file = File(context.backupDir(), "$BACKUP_PREFIX$ts$BACKUP_SUFFIX")
    file.writeText(json)
    pruneOldBackups(context)
    return file
}

private fun pruneOldBackups(context: Context) {
    val files = context.backupDir().listFiles { f -> f.name.startsWith(BACKUP_PREFIX) }?.sortedByDescending { it.lastModified() } ?: return
    files.drop(MAX_BACKUPS).forEach { it.delete() }
}

fun listLocalBackups(context: Context): List<File> =
    context.backupDir().listFiles { f -> f.name.startsWith(BACKUP_PREFIX) }
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()

fun importBackupJson(context: Context, json: String): BackupResult {
    return try {
        val root = JSONObject(json)
        val carsArr = root.optJSONArray("cars") ?: JSONArray()
        val worksArr = root.optJSONArray("works") ?: JSONArray()
        val recordsArr = root.optJSONArray("records") ?: JSONArray()
        val partsArr = root.optJSONArray("parts") ?: JSONArray()
        val templatesArr = root.optJSONArray("templates") ?: JSONArray()

        context.prefs().edit()
            .putString(CARS, carsArr.toString())
            .putString(WORKS, worksArr.toString())
            .putString(RECORDS, recordsArr.toString())
            .putString(PARTS, partsArr.toString())
            .putString(TEMPLATES, templatesArr.toString())
            .putBoolean(MIGRATED, true)
            .commit()

        BackupResult(true, "Импортировано: ${carsArr.length()} авто, ${worksArr.length()} работ, ${recordsArr.length()} записей.")
    } catch (e: Exception) {
        BackupResult(false, "Ошибка импорта: ${e.message}")
    }
}

fun restoreFromLocalBackup(context: Context, file: File): BackupResult {
    return try {
        importBackupJson(context, file.readText())
    } catch (e: Exception) {
        BackupResult(false, "Ошибка чтения файла: ${e.message}")
    }
}
