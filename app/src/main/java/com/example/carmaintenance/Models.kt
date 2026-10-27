package com.example.carmaintenance

import java.util.UUID

enum class WorkCategory { GENERAL, LPG }
enum class ServiceLineType { WORK, PART, CONSUMABLE, TIRE }

data class Car(
    val id: String = UUID.randomUUID().toString(),
    var brand: String = "", var model: String = "", var year: String = "",
    var vin: String = "", var mileage: String = "", var engine: String = "", var fuel: String = "",
    var hasLpg: Boolean = false, var notes: String = "", var imageUri: String = ""
)

data class RegulationTemplate(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var category: WorkCategory = WorkCategory.GENERAL,
    var intervalKm: Int = 0,
    var intervalMonths: Int = 0,
    var warningKm: Int = 0,
    var warningMonths: Int = 0,
    var preventive: Boolean = false,
    var notes: String = ""
)

data class MaintenanceWork(
    val id: String = UUID.randomUUID().toString(),
    val carId: String,
    var templateId: String = "",
    var name: String,
    var category: WorkCategory = WorkCategory.GENERAL,
    var intervalKm: Int = 0,
    var intervalMonths: Int = 0,
    var warningKm: Int = 0,
    var warningMonths: Int = 0,
    var lastMileage: Int = 0,
    var lastDate: Long = 0L,
    var hasLastExecution: Boolean = false,
    var skipped: Boolean = false,
    var preventive: Boolean = false,
    var notes: String = ""
)

data class ServiceLine(
    val id: String = UUID.randomUUID().toString(),
    var type: ServiceLineType = ServiceLineType.WORK,
    var name: String,
    var article: String = "",
    var cost: Double = 0.0,
    var currency: String = "RUB", // По умолчанию рубли
    var oilType: String = "",
    var oilName: String = "",
    var notes: String = ""
)

data class ServiceRecord(
    val id: String = UUID.randomUUID().toString(),
    val carId: String,
    val workId: String? = null, // <--- ДОБАВЛЕНО: привязка к MaintenanceWork
    var date: Long = System.currentTimeMillis(),
    var servicePlace: String = "",
    var reason: String = "",
    var mileage: Int = 0,
    var lines: List<ServiceLine> = emptyList(),
    var notes: String = ""
) {
    fun totalByCurrency(): Map<String, Double> = lines.groupBy { it.currency.ifBlank { "RUB" } }
        .mapValues { (_, rows) -> rows.sumOf { it.cost } }
}

data class PartCatalogItem(
    val id: String = UUID.randomUUID().toString(),
    val carId: String,
    var name: String,
    var article: String = "",
    var category: String = "Запчасть",
    var lastCost: Double = 0.0,
    var currency: String = "RUB"
)

data class WorkHistory(
    val id: String = UUID.randomUUID().toString(),
    val carId: String,
    val workName: String,
    val date: Long,
    val mileage: Int,
    val notes: String
)
