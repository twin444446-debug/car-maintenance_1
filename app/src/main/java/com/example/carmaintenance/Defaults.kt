package com.example.carmaintenance

fun getDefaultWorks(carId: String): List<MaintenanceWork> = defaultTemplates()
    .filter { it.category == WorkCategory.GENERAL }
    .map {
        MaintenanceWork(
            carId = carId, 
            templateId = it.id, 
            name = it.name, 
            groupName = it.groupName,
            category = it.category, 
            intervalKm = it.intervalKm, 
            intervalMonths = it.intervalMonths, 
            warningKm = it.warningKm, 
            warningMonths = it.warningMonths, 
            preventive = it.preventive, 
            notes = it.notes
        )
    }

fun getLpgDefaultWorks(carId: String): List<MaintenanceWork> = defaultTemplates()
    .filter { it.category == WorkCategory.LPG }
    .map {
        MaintenanceWork(
            carId = carId, 
            templateId = it.id, 
            name = it.name, 
            groupName = it.groupName,
            category = it.category, 
            intervalKm = it.intervalKm, 
            intervalMonths = it.intervalMonths, 
            warningKm = it.warningKm, 
            warningMonths = it.warningMonths, 
            preventive = it.preventive, 
            notes = it.notes
        )
    }

// Функция defaultTemplates() удалена отсюда, так как она теперь находится в Storage.kt
