package com.example.carmaintenance

import android.content.Context

// ==================== ШАБЛОНЫ РЕГЛАМЕНТА (ДАННЫЕ) ====================

fun defaultTemplates(): List<RegulationTemplate> = listOf(
    // ================== 1. МАСЛА ==================
    RegulationTemplate(
        name = "Масло в двигателе", groupName = "1. МАСЛА",
        category = WorkCategory.GENERAL, intervalKm = 10000, intervalMonths = 12, warningKm = 1000, warningMonths = 1, notes = "Объем ~3.3 л. Проверять уровень каждые 1000 км."
    ),
    RegulationTemplate(
        name = "Масло в механической коробке передач", groupName = "1. МАСЛА",
        category = WorkCategory.GENERAL, intervalKm = 90000, intervalMonths = 60, warningKm = 5000, warningMonths = 3
    ),

    // ================== 2. ЖИДКОСТИ ==================
    RegulationTemplate(
        name = "Жидкость охлаждающая (антифриз)", groupName = "2. ЖИДКОСТИ",
        category = WorkCategory.GENERAL, intervalKm = 40000, intervalMonths = 24, warningKm = 3000, warningMonths = 2
    ),
    RegulationTemplate(
        name = "Тормозная жидкость DOT4", groupName = "2. ЖИДКОСТИ",
        category = WorkCategory.GENERAL, intervalKm = 30000, intervalMonths = 24, warningKm = 3000, warningMonths = 2
    ),

    // ================== 3. ФИЛЬТРЫ ==================
    RegulationTemplate(
        name = "Фильтр масляный", groupName = "3. ФИЛЬТРЫ",
        category = WorkCategory.GENERAL, intervalKm = 10000, intervalMonths = 12, warningKm = 1000, warningMonths = 1
    ),
    RegulationTemplate(
        name = "Фильтр воздушный", groupName = "3. ФИЛЬТРЫ",
        category = WorkCategory.GENERAL, intervalKm = 10000, intervalMonths = 12, warningKm = 1000, warningMonths = 1
    ),
    RegulationTemplate(
        name = "Фильтр кондиционера (салона)", groupName = "3. ФИЛЬТРЫ",
        category = WorkCategory.GENERAL, intervalKm = 10000, intervalMonths = 12, warningKm = 1000, warningMonths = 1
    ),
    RegulationTemplate(
        name = "Фильтр топливный (сеточка в баке)", groupName = "3. ФИЛЬТРЫ",
        category = WorkCategory.GENERAL, intervalKm = 40000, intervalMonths = 48, warningKm = 3000, warningMonths = 2, notes = "Артикул: 31090-07000"
    ),
    RegulationTemplate(
        name = "Фильтр топливный (тонкой очистки)", groupName = "3. ФИЛЬТРЫ",
        category = WorkCategory.GENERAL, intervalKm = 40000, intervalMonths = 48, warningKm = 3000, warningMonths = 2, notes = "Артикул: 31112-1G500"
    ),

    // ================== 4. РЕМНИ ==================
    RegulationTemplate(
        name = "Ремень ГРМ + ролики", groupName = "4. РЕМНИ",
        category = WorkCategory.GENERAL, intervalKm = 60000, intervalMonths = 60, warningKm = 5000, warningMonths = 3
    ),
    RegulationTemplate(
        name = "Ремень генератора", groupName = "4. РЕМНИ",
        category = WorkCategory.GENERAL, intervalKm = 60000, intervalMonths = 60, warningKm = 5000, warningMonths = 3
    ),
    RegulationTemplate(
        name = "Ремень кондиционера", groupName = "4. РЕМНИ",
        category = WorkCategory.GENERAL, intervalKm = 60000, intervalMonths = 60, warningKm = 5000, warningMonths = 3
    ),

    // ================== 5. ЗАЖИГАНИЕ ==================
    RegulationTemplate(
        name = "Свечи зажигания", groupName = "5. ЗАЖИГАНИЕ",
        category = WorkCategory.GENERAL, intervalKm = 20000, intervalMonths = 24, warningKm = 2000, warningMonths = 1
    ),
    RegulationTemplate(
        name = "Высоковольтные провода", groupName = "5. ЗАЖИГАНИЕ",
        category = WorkCategory.GENERAL, intervalKm = 40000, intervalMonths = 48, warningKm = 3000, warningMonths = 2
    ),
    RegulationTemplate(
        name = "Катушки зажигания", groupName = "5. ЗАЖИГАНИЕ",
        category = WorkCategory.GENERAL, intervalKm = 140000, intervalMonths = 84, warningKm = 10000, warningMonths = 6
    ),

    // ================== 6. ТОРМОЗА ==================
    RegulationTemplate(
        name = "Колодки тормозные передние", groupName = "6. ТОРМОЗА",
        category = WorkCategory.GENERAL, intervalKm = 30000, intervalMonths = 24, warningKm = 3000, warningMonths = 2
    ),
    RegulationTemplate(
        name = "Колодки тормозные задние", groupName = "6. ТОРМОЗА",
        category = WorkCategory.GENERAL, intervalKm = 60000, intervalMonths = 36, warningKm = 5000, warningMonths = 3
    ),

    // ================== 7. ДАТЧИКИ И ПРОЧЕЕ ==================
    RegulationTemplate(
        name = "Датчик кислорода (Лямбда-зонд)", groupName = "7. ДАТЧИКИ И ПРОЧЕЕ",
        category = WorkCategory.GENERAL, intervalKm = 100000, intervalMonths = 72, warningKm = 10000, warningMonths = 6
    ),
    RegulationTemplate(
        name = "Помпа (водяной насос) + прокладка", groupName = "7. ДАТЧИКИ И ПРОЧЕЕ",
        category = WorkCategory.GENERAL, intervalKm = 120000, intervalMonths = 72, warningKm = 10000, warningMonths = 6
    ),
    RegulationTemplate(
        name = "Чистка инжектора (форсунок)", groupName = "7. ДАТЧИКИ И ПРОЧЕЕ",
        category = WorkCategory.GENERAL, intervalKm = 20000, intervalMonths = 24, warningKm = 2000, warningMonths = 1
    ),

    // ================== 8. ГБО ==================
    RegulationTemplate(
        name = "ГБО: фильтр жидкой фракции (в редукторе)", groupName = "8. ГБО",
        category = WorkCategory.LPG, intervalKm = 15000, intervalMonths = 12, warningKm = 1500, warningMonths = 1
    ),
    RegulationTemplate(
        name = "ГБО: фильтр паровой фракции (тонкой очистки)", groupName = "8. ГБО",
        category = WorkCategory.LPG, intervalKm = 15000, intervalMonths = 12, warningKm = 1500, warningMonths = 1
    ),
    RegulationTemplate(
        name = "ГБО: Форсунки Barracuda (замена/чистка)", groupName = "8. ГБО",
        category = WorkCategory.LPG, intervalKm = 150000, intervalMonths = 120, warningKm = 10000, warningMonths = 6, notes = "Ресурс 150-200 тыс. км."
    ),
    RegulationTemplate(
        name = "ГБО: переборка редуктора (ремкомплект)", groupName = "8. ГБО",
        category = WorkCategory.LPG, intervalKm = 50000, intervalMonths = 60, warningKm = 5000, preventive = true
    )
).map { it.copy(isSystem = true) } // <-- Помечаем как системные

fun resetTemplatesToDefault(context: Context): List<RegulationTemplate> {
    val defaults = defaultTemplates()
    saveTemplates(context, defaults)
    return defaults
}
