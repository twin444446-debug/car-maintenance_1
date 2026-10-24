package com.example.carmaintenance

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class WorkStatus { COMPLETED, REQUIRED, SKIPPED, PREVENTIVE }

fun statusLabel(status: WorkStatus): String = when (status) {
    WorkStatus.COMPLETED -> "Выполнено"
    WorkStatus.REQUIRED -> "Необходимо выполнить"
    WorkStatus.SKIPPED -> "Пропущена"
    WorkStatus.PREVENTIVE -> "Рекомендуется"
}

fun statusColor(status: WorkStatus) = when (status) {
    WorkStatus.COMPLETED -> ColorGreen
    WorkStatus.REQUIRED -> ColorRed
    WorkStatus.SKIPPED -> ColorPurple
    WorkStatus.PREVENTIVE -> ColorYellow
}

private fun normalizedWorkName(value: String): String = value.trim()
    .removePrefix("ГБО:").trim()
    .replace(Regex("\\s+"), " ")

fun latestRecord(work: MaintenanceWork, records: List<ServiceRecord>): ServiceRecord? = records
    .asSequence()
    .filter { it.carId == work.carId }
    // Parts and consumables with the same name must not reset a regulation interval.
    .filter { record -> record.lines.any { line ->
        line.type == ServiceLineType.WORK &&
            normalizedWorkName(line.name).equals(normalizedWorkName(work.name), ignoreCase = true)
    } }
    .maxWithOrNull(compareBy<ServiceRecord> { it.date }.thenBy { it.mileage })

fun effectiveBaselineMileage(work: MaintenanceWork, records: List<ServiceRecord>): Int? = latestRecord(work, records)?.mileage ?: work.takeIf { it.hasLastExecution }?.lastMileage
fun effectiveBaselineDate(work: MaintenanceWork, records: List<ServiceRecord>): Long? = latestRecord(work, records)?.date ?: work.takeIf { it.hasLastExecution }?.lastDate

fun getWorkStatus(work: MaintenanceWork, car: Car, records: List<ServiceRecord> = emptyList()): WorkStatus {
    if (work.skipped) return WorkStatus.SKIPPED
    val currentKm = car.mileage.toIntOrNull() ?: 0
    val baseKm = effectiveBaselineMileage(work, records)
    val baseDate = effectiveBaselineDate(work, records)
    if (baseKm == null && baseDate == null) return if (work.preventive) WorkStatus.PREVENTIVE else WorkStatus.REQUIRED
    val dueKm = work.intervalKm > 0 && baseKm != null && currentKm >= baseKm + work.intervalKm
    val dueDate = work.intervalMonths > 0 && baseDate != null && System.currentTimeMillis() >= addMonths(baseDate, work.intervalMonths)
    if (dueKm || dueDate) return WorkStatus.REQUIRED
    val warnKm = work.warningKm > 0 && baseKm != null && currentKm >= (baseKm + work.intervalKm - work.warningKm).coerceAtLeast(0)
    val warnDate = work.warningMonths > 0 && baseDate != null && System.currentTimeMillis() >= addMonths(baseDate, (work.intervalMonths - work.warningMonths).coerceAtLeast(0))
    if (work.preventive || warnKm || warnDate) return WorkStatus.PREVENTIVE
    return WorkStatus.COMPLETED
}

fun nextDueMileage(work: MaintenanceWork, records: List<ServiceRecord>): Int? {
    val base = effectiveBaselineMileage(work, records) ?: return null
    return if (work.intervalKm > 0) base + work.intervalKm else null
}
fun nextDueDate(work: MaintenanceWork, records: List<ServiceRecord>): Long? {
    val base = effectiveBaselineDate(work, records) ?: return null
    return if (work.intervalMonths > 0) addMonths(base, work.intervalMonths) else null
}

fun addMonths(time: Long, months: Int): Long = Calendar.getInstance().apply { timeInMillis = time; add(Calendar.MONTH, months) }.timeInMillis
fun formatDate(time: Long): String = if (time <= 0L) "—" else SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(time))
fun formatMoney(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.getDefault(), "%.2f", value)
fun formatTotals(record: ServiceRecord): String = record.totalByCurrency().entries.joinToString(" + ") { "${formatMoney(it.value)} ${it.key}" }
