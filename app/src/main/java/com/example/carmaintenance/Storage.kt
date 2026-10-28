package com.example.carmaintenance

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
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
        put("id", t.id); put("name", t.name); put("category", t.category.name); put("intervalKm", t.intervalKm)
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
        put("id", w.id); put("carId", w.carId); put("templateId", w.templateId); put("name", w.name); put("category", w.category.name)
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
                workId = o.optString("workId").takeIf { it.isNotBlank() }, // <-- ДОБАВЛЕНО
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
        put("workId", r.workId) // <-- ДОБАВЛЕНО
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
        .map { WorkHistory(it.id, r.carId, it.name, r.date, r.mileage, r.notes.ifBlank { r.reason }) }
}

fun loadDefaultTemplate(context: Context): List<MaintenanceWork> = loadTemplates(context).map {
    MaintenanceWork(carId = "", templateId = it.id, name = it.name, category = it.category, intervalKm = it.intervalKm, intervalMonths = it.intervalMonths, warningKm = it.warningKm, warningMonths = it.warningMonths, preventive = it.preventive, notes = it.notes)
}

fun saveDefaultTemplate(context: Context, works: List<MaintenanceWork>) {
    saveTemplates(context, works.map { RegulationTemplate(it.templateId.ifBlank { it.id }, it.name, it.category, it.intervalKm, it.intervalMonths, it.warningKm, it.warningMonths, it.preventive, it.notes) })
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
                    warningKm: Int, warningMonths: Int, preventive: Boolean, notes: String) {
        if (name.isBlank()) return
        val existing = templates.firstOrNull { it.name.trim().equals(name.trim(), ignoreCase = true) && it.category == category }
        if (existing == null) templates.add(RegulationTemplate(
            id = id.ifBlank { UUID.randomUUID().toString() }, name = name.trim(), category = category,
            intervalKm = km, intervalMonths = months, warningKm = warningKm,
            warningMonths = warningMonths, preventive = preventive, notes = notes
        ))
    }

    for (i in 0 until oldDefault.length()) {
        val o = oldDefault.optJSONObject(i) ?: continue
        val name = o.optString("name")
        addTemplate(name, categoryFor(name, o), o.optString("id"), o.optInt("intervalKm"),
            o.optInt("intervalMonths"), o.optInt("warningKm"), o.optInt("warningMonths"),
            o.optBoolean("preventive"), o.optString("notes"))
    }
    for (i in 0 until oldWorks.length()) {
        val o = oldWorks.optJSONObject(i) ?: continue
        val name = o.optString("name")
        addTemplate(name, categoryFor(name, o), o.optString("templateId").ifBlank { o.optString("id") },
            o.optInt("intervalKm"), o.optInt("intervalMonths"), o.optInt("warningKm"),
            o.optInt("warningMonths"), o.optBoolean("preventive"), o.optString("notes"))
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
            workId = o.optString("workId").takeIf { it.isNotBlank() }, // <-- ДОБАВЛЕНО
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
