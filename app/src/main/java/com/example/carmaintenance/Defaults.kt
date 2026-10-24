package com.example.carmaintenance

fun getDefaultWorks(carId: String): List<MaintenanceWork> = defaultTemplates().filter { it.category == WorkCategory.GENERAL }.map {
    MaintenanceWork(carId = carId, templateId = it.id, name = it.name, category = it.category, intervalKm = it.intervalKm, intervalMonths = it.intervalMonths, warningKm = it.warningKm, warningMonths = it.warningMonths, preventive = it.preventive, notes = it.notes)
}

fun getLpgDefaultWorks(carId: String): List<MaintenanceWork> = defaultTemplates().filter { it.category == WorkCategory.LPG }.map {
    MaintenanceWork(carId = carId, templateId = it.id, name = it.name, category = it.category, intervalKm = it.intervalKm, intervalMonths = it.intervalMonths, warningKm = it.warningKm, warningMonths = it.warningMonths, preventive = it.preventive, notes = it.notes)
}

fun defaultTemplates(): List<RegulationTemplate> = listOf(
    RegulationTemplate(name = "Масло в двигателе", intervalKm = 10000, intervalMonths = 12, warningKm = 1000, warningMonths = 1),
    RegulationTemplate(name = "Масло в механической коробке передач", intervalKm = 90000, intervalMonths = 60, warningKm = 5000, warningMonths = 3),
    RegulationTemplate(name = "Масляный фильтр", intervalKm = 10000, intervalMonths = 12, warningKm = 1000, warningMonths = 1),
    RegulationTemplate(name = "Топливный фильтр (тонкой очистки)", intervalKm = 20000, intervalMonths = 24, warningKm = 2000, warningMonths = 2),
    RegulationTemplate(name = "Топливный фильтр (сеточка)", intervalKm = 40000, intervalMonths = 48, warningKm = 4000, warningMonths = 3),
    RegulationTemplate(name = "Воздушный фильтр", intervalKm = 10000, intervalMonths = 12, warningKm = 1000, warningMonths = 1),
    RegulationTemplate(name = "Фильтр кондиционера (салона)", intervalKm = 10000, intervalMonths = 12, warningKm = 1000, warningMonths = 1),
    RegulationTemplate(name = "Свечи зажигания", intervalKm = 20000, intervalMonths = 24, warningKm = 2000, warningMonths = 2),
    RegulationTemplate(name = "Чистка инжектора (форсунок)", intervalKm = 20000, intervalMonths = 24, warningKm = 2000, warningMonths = 2),
    RegulationTemplate(name = "Высоковольтные провода", intervalKm = 40000, intervalMonths = 48, warningKm = 4000, warningMonths = 3),
    RegulationTemplate(name = "Датчик кислорода 4 контакта", intervalKm = 100000, intervalMonths = 120, warningKm = 5000, warningMonths = 6),
    RegulationTemplate(name = "Помпа (водяной насос)", intervalKm = 60000, intervalMonths = 60, warningKm = 5000, warningMonths = 3),
    RegulationTemplate(name = "Тормозная жидкость DOT4", intervalKm = 30000, intervalMonths = 36, warningKm = 3000, warningMonths = 3),
    RegulationTemplate(name = "Охлаждающая жидкость", intervalKm = 40000, intervalMonths = 24, warningKm = 4000, warningMonths = 2),
    RegulationTemplate(name = "Передние тормозные колодки", preventive = true),
    RegulationTemplate(name = "Задние тормозные колодки", preventive = true),
    RegulationTemplate(name = "Ремень ГРМ, кондиционера, генератора и ролики", intervalKm = 60000, intervalMonths = 60, warningKm = 5000, warningMonths = 3),
    RegulationTemplate(name = "ГБО: фильтр жидкой фракции (в редукторе)", category = WorkCategory.LPG, intervalKm = 15000, intervalMonths = 12, warningKm = 1500, warningMonths = 1),
    RegulationTemplate(name = "ГБО: фильтр паровой фракции (тонкой очистки)", category = WorkCategory.LPG, intervalKm = 15000, intervalMonths = 12, warningKm = 1500, warningMonths = 1),
    RegulationTemplate(name = "ГБО: BARRACUDA (замена)", category = WorkCategory.LPG, intervalKm = 30000, intervalMonths = 36, warningKm = 3000, warningMonths = 3),
    RegulationTemplate(name = "ГБО: переборка редуктора (ремкомплект)", category = WorkCategory.LPG, intervalKm = 50000, preventive = true, warningKm = 5000)
)
