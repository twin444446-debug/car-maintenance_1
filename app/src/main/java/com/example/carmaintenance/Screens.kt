package com.example.carmaintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppScreen() {
    val context = LocalContext.current
    var cars by remember { mutableStateOf(loadCars(context)) }
    var templates by remember { mutableStateOf(loadTemplates(context).ifEmpty { defaultTemplates().also { saveTemplates(context,it) } }) }
    var works by remember { mutableStateOf(loadWorks(context)) }
    var records by remember { mutableStateOf(loadServiceRecords(context)) }
    var parts by remember { mutableStateOf(loadPartCatalog(context)) }
    var selectedCarId by remember { mutableStateOf(context.getSharedPreferences("car_maintenance", android.content.Context.MODE_PRIVATE).getString("selected_car_id", null)?.takeIf { id -> cars.any { it.id == id } } ?: cars.firstOrNull()?.id) }
    var screen by remember { mutableStateOf("Главная") }
    var carDialog by remember { mutableStateOf(false) }; var editingCar by remember { mutableStateOf<Car?>(null) }
    var mileageDialog by remember { mutableStateOf(false) }
    var serviceDialog by remember { mutableStateOf(false) }; var serviceInitial by remember { mutableStateOf<ServiceRecord?>(null) }; var suggestedWork by remember { mutableStateOf("") }
    var actionWork by remember { mutableStateOf<MaintenanceWork?>(null) }; var regulationDialog by remember { mutableStateOf(false) }; var editingRegulation by remember { mutableStateOf<MaintenanceWork?>(null) }
    var templateEditor by remember { mutableStateOf(false) }
    var errorDecoder by remember { mutableStateOf(false) }
    var selectedRecord by remember { mutableStateOf<ServiceRecord?>(null) }; var partsScreen by remember { mutableStateOf(false) }

    val car = cars.firstOrNull { it.id == selectedCarId }
    fun refreshCars() { cars = loadCars(context) }
    fun saveAndRefreshWorks(newWorks: List<MaintenanceWork>) { works=newWorks; saveWorks(context,works) }
    fun createDefaultForCar(c: Car) {
        val source = templates.filter { it.category == WorkCategory.GENERAL || (it.category == WorkCategory.LPG && c.hasLpg) }
        val existingForCar = works.filter { it.carId == c.id }
        val existingTemplateIds = existingForCar.map { it.templateId }.filter { it.isNotBlank() }.toSet()
        val add = source.filter { template ->
            template.id !in existingTemplateIds && existingForCar.none { work ->
                work.name.equals(template.name, ignoreCase = true) && work.category == template.category
            }
        }.map { template -> MaintenanceWork(carId=c.id,templateId=template.id,name=template.name,category=template.category,intervalKm=template.intervalKm,intervalMonths=template.intervalMonths,warningKm=template.warningKm,warningMonths=template.warningMonths,preventive=template.preventive,notes=template.notes) }
        saveAndRefreshWorks(works + add)
    }
    fun completeWork(w: MaintenanceWork, km:Int, date:Long, note:String) {
        val line=ServiceLine(name=w.name,type=ServiceLineType.WORK,notes=note)
        val r=ServiceRecord(carId=w.carId,date=date,mileage=km,reason=note,lines=listOf(line),notes=note)
        records=records+r; saveServiceRecords(context,records)
        saveAndRefreshWorks(works.map{if(it.id==w.id)it.copy(lastMileage=km,lastDate=date,hasLastExecution=true,skipped=false)else it})
    }

    Scaffold(containerColor=BgColor, bottomBar={BottomNav(screen){screen=it}}) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when {
                templateEditor -> TemplateEditorScreen(templates=templates,onBack={templateEditor=false},onSave={templates=it;saveTemplates(context,templates)})
                errorDecoder -> ErrorDecoderScreen(onBack={errorDecoder=false})
                partsScreen -> PartsCatalogScreen(car,parts,onBack={partsScreen=false},onAdd={p->parts=parts+p;savePartCatalog(context,parts)})
                else -> when(screen) {
                    "Главная" -> HomeScreen(car,cars,works,records,onCarSelect={selectedCarId=it;context.getSharedPreferences("car_maintenance", android.content.Context.MODE_PRIVATE).edit().putString("selected_car_id",it).apply()},onAddCar={editingCar=null;carDialog=true},onAddWork={suggestedWork="";serviceInitial=null;serviceDialog=true},onWork={actionWork=it},onMileage={mileageDialog=true},onOpenCar={screen="Автомобиль"})
                    "Перечень работ" -> WorkListScreen(car,records,parts,onAdd={suggestedWork="";serviceInitial=null;serviceDialog=true},onEdit={selectedRecord=it;serviceInitial=it;serviceDialog=true},onPartSave={parts=parts+it;savePartCatalog(context,parts)})
                    "Регламент" -> RegulationScreen(car,works.filter{it.carId==car?.id},records,onWork={actionWork=it},onAdd={editingRegulation=null;regulationDialog=true},onFill={if(car!=null)createDefaultForCar(car)})
                    "Статистика" -> StatisticsScreen(car,records)
                    "Ещё" -> MoreScreen(onTemplate={templateEditor=true},onDecoder={errorDecoder=true},onParts={partsScreen=true})
                    "Автомобиль" -> CarDetailScreen(car,onBack={screen="Главная"},onEdit={editingCar=car;carDialog=true},onMileage={mileageDialog=true},onFill={if(car!=null)createDefaultForCar(car)},onDelete={if(car!=null){cars=cars.filter{it.id!=car.id};saveCars(context,cars);selectedCarId=cars.firstOrNull()?.id;context.getSharedPreferences("car_maintenance", android.content.Context.MODE_PRIVATE).edit().putString("selected_car_id",selectedCarId ?: "").apply();screen="Главная"}})
                }
            }
        }
    }

    if(carDialog) AddCarDialog(editingCar,{carDialog=false}){newCar->cars=if(cars.any{it.id==newCar.id})cars.map{if(it.id==newCar.id)newCar else it}else cars+newCar;saveCars(context,cars);selectedCarId=newCar.id;context.getSharedPreferences("car_maintenance", android.content.Context.MODE_PRIVATE).edit().putString("selected_car_id",newCar.id).apply();carDialog=false;editingCar=null;if(works.none{it.carId==newCar.id})createDefaultForCar(newCar)}
    if(mileageDialog && car!=null) MileageDialog(car.mileage,{mileageDialog=false}){v->cars=cars.map{if(it.id==car.id)it.copy(mileage=v)else it};saveCars(context,cars);mileageDialog=false}
    if(serviceDialog && car!=null) ServiceRecordDialog(car,serviceInitial,suggestedWork,parts,{serviceDialog=false;serviceInitial=null}){r->records=if(records.any{it.id==r.id})records.map{if(it.id==r.id)r else it}else records+r;saveServiceRecords(context,records);serviceDialog=false;serviceInitial=null}
    if(actionWork!=null && car!=null) WorkActionsDialog(actionWork!!,car,records,{actionWork=null},{km,date,note->completeWork(actionWork!!,km,date,note);actionWork=null},{saveAndRefreshWorks(works.map{if(it.id==actionWork!!.id)it.copy(skipped=true)else it});actionWork=null},{editingRegulation=actionWork;actionWork=null;regulationDialog=true},{saveAndRefreshWorks(works.filter{it.id!=actionWork!!.id});actionWork=null},{screen="Регламент";actionWork=null})
    if(regulationDialog && car!=null) RegulationWorkDialog(car.id,editingRegulation,templates,{regulationDialog=false;editingRegulation=null}){w->saveAndRefreshWorks(if(works.any{it.id==w.id})works.map{if(it.id==w.id)w else it}else works+w);regulationDialog=false;editingRegulation=null}
}

@Composable private fun BottomNav(screen:String,onSelect:(String)->Unit){NavigationBar(containerColor=NavColor){listOf("Главная" to Icons.Default.Home,"Перечень работ" to Icons.Default.List,"Регламент" to Icons.Default.Build,"Статистика" to Icons.Default.Info,"Ещё" to Icons.Default.MoreVert).forEach{(t,i)->NavigationBarItem(selected=screen==t,onClick={onSelect(t)},icon={Icon(i,t)},label={Text(t,fontSize=10.sp)},colors=NavigationBarItemDefaults.colors(selectedIconColor=ColorGreen,selectedTextColor=ColorGreen,indicatorColor=ColorGreen.copy(alpha=.1f),unselectedIconColor=TextSecondary,unselectedTextColor=TextSecondary))}}}

@Composable private fun Page(title:String,content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize().padding(16.dp)){Text(title,color=TextPrimary,fontSize=24.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(12.dp));content()}}

@Composable private fun HomeScreen(car:Car?,cars:List<Car>,works:List<MaintenanceWork>,records:List<ServiceRecord>,onCarSelect:(String)->Unit,onAddCar:()->Unit,onAddWork:()->Unit,onWork:(MaintenanceWork)->Unit,onMileage:()->Unit,onOpenCar:()->Unit){
    if(car==null){Page("Учёт автомобильного ТО"){Button(onClick=onAddCar){Text("Добавить автомобиль")}};return}
    val cw=works.filter{it.carId==car.id};val statuses=cw.map{getWorkStatus(it,car,records)}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Card(Modifier.fillMaxWidth().clickable{onOpenCar()},colors=CardDefaults.cardColors(containerColor=CardColor),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(18.dp)){Text("${car.brand} ${car.model}",color=TextPrimary,fontSize=22.sp,fontWeight=FontWeight.Bold);Text("${car.year.ifBlank{"—"}} • ${car.engine.ifBlank{"двигатель не указан"}}",color=TextSecondary);Spacer(Modifier.height(8.dp));Text("${car.mileage.ifBlank{"0"}} км",color=ColorBlue,fontSize=22.sp,fontWeight=FontWeight.Bold);Text("VIN: ${car.vin.ifBlank{"не указан"}}",color=TextSecondary);Text("Топливо: ${car.fuel.ifBlank{"не указано"}}${if(car.hasLpg)" • ГБО" else ""}",color=TextSecondary);TextButton(onClick=onMileage){Text("Изменить пробег",color=ColorYellow)}}}}
        item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){StatusBlock("Выполнено",statuses.count{it==WorkStatus.COMPLETED},ColorGreen,Modifier.weight(1f));StatusBlock("Необходимо",statuses.count{it==WorkStatus.REQUIRED},ColorRed,Modifier.weight(1f))}}
        item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){StatusBlock("Рекомендации",statuses.count{it==WorkStatus.PREVENTIVE},ColorYellow,Modifier.weight(1f));StatusBlock("Пропущено",statuses.count{it==WorkStatus.SKIPPED},ColorPurple,Modifier.weight(1f))}}
        item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){QuickAction("Добавить запись",{Icon(Icons.Default.Add,color=ColorGreen)},ColorGreen,onAddWork,Modifier.weight(1f));QuickAction("Автомобиль",{Icon(Icons.Default.Build,color=ColorBlue)},ColorBlue,onOpenCar,Modifier.weight(1f))}}
        item{SectionTitle("Ближайшие работы")}
        items(cw.sortedBy{when(getWorkStatus(it,car,records)){WorkStatus.REQUIRED->0;WorkStatus.SKIPPED->1;WorkStatus.PREVENTIVE->2;else->3}}.take(8)){w->WorkCard(w,car,records){onWork(w)}}
    }
}

@Composable private fun WorkListScreen(car:Car?,records:List<ServiceRecord>,parts:List<PartCatalogItem>,onAdd:()->Unit,onEdit:(ServiceRecord)->Unit,onPartSave:(PartCatalogItem)->Unit){if(car==null){Page("Перечень работ"){Text("Сначала добавьте автомобиль",color=TextSecondary)};return};Page("Перечень работ"){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick=onAdd,modifier=Modifier.weight(1f)){Text("Добавить запись")};Text("${records.count{it.carId==car.id}} записей",color=TextSecondary,modifier=Modifier.padding(12.dp))};Spacer(Modifier.height(8.dp));LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(records.filter{it.carId==car.id}.sortedByDescending{it.date}){r->Card(Modifier.fillMaxWidth().clickable{onEdit(r)},colors=CardDefaults.cardColors(containerColor=CardColor)){Column(Modifier.padding(14.dp)){Text(formatDate(r.date),color=ColorBlue);Text(r.servicePlace.ifBlank{"Самостоятельно"},color=TextSecondary);Text(r.reason.ifBlank{"Обслуживание"},color=TextPrimary,fontWeight=FontWeight.Bold);Text("Пробег: ${r.mileage} км",color=TextSecondary);r.lines.forEach{l->Text("• ${l.name}${if(l.article.isNotBlank())" [${l.article}]" else ""} — ${formatMoney(l.cost)} ${l.currency}",color=TextPrimary,fontSize=13.sp)};Text("Итого: ${formatTotals(r)}",color=ColorGreen,fontWeight=FontWeight.Bold)}}}}}}}

@Composable private fun RegulationScreen(car:Car?,works:List<MaintenanceWork>,records:List<ServiceRecord>,onWork:(MaintenanceWork)->Unit,onAdd:()->Unit,onFill:()->Unit){if(car==null){Page("Регламент"){Text("Сначала добавьте автомобиль",color=TextSecondary)};return};val normal=works.filter{it.category==WorkCategory.GENERAL};val lpg=works.filter{it.category==WorkCategory.LPG};Page("Регламент"){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick=onFill,modifier=Modifier.weight(1f)){Text("Заполнить из шаблона")};Button(onClick=onAdd,modifier=Modifier.weight(1f)){Text("Добавить")}};Spacer(Modifier.height(8.dp));LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){item{SectionTitle("Обслуживание автомобиля")};items(normal){w->WorkCard(w,car,records){onWork(w)}};if(car.hasLpg){item{SectionTitle("Регламент ГБО")};items(lpg){w->WorkCard(w,car,records){onWork(w)}}}}}}

@Composable private fun StatisticsScreen(car:Car?,records:List<ServiceRecord>){if(car==null){Page("Статистика"){Text("Сначала добавьте автомобиль",color=TextSecondary)};return};val rs=records.filter{it.carId==car.id};val totals=rs.flatMap{it.lines}.groupBy{it.currency}.mapValues{(_,v)->v.sumOf{it.cost}};Page("Статистика"){StatusBlock("Записей обслуживания",rs.size,ColorBlue,Modifier.fillMaxWidth());Spacer(Modifier.height(12.dp));SectionTitle("Расходы по валютам");totals.forEach{(c,v)->InfoRow(c,formatMoney(v))};Spacer(Modifier.height(12.dp));SectionTitle("Работы");InfoRow("Всего строк",rs.sumOf{it.lines.size}.toString());InfoRow("Последнее обслуживание",formatDate(rs.maxOfOrNull{it.date}?:0L));Text("Расход топлива не рассчитывается: для автомобиля с бензином и ГБО такой показатель без дополнительных данных может быть некорректным.",color=TextSecondary,fontSize=12.sp)}}

@Composable private fun MoreScreen(onTemplate:()->Unit,onDecoder:()->Unit,onParts:()->Unit){Page("Ещё"){Button(onClick=onTemplate,modifier=Modifier.fillMaxWidth()){Text("Шаблоны регламента")};Button(onClick=onDecoder,modifier=Modifier.fillMaxWidth()){Text("Расшифровка кодов ошибок")};Button(onClick=onParts,modifier=Modifier.fillMaxWidth()){Text("Запчасти и расходники")};Text("Шаблоны общие, а интервалы, статусы и история — отдельно для каждого автомобиля.",color=TextSecondary)}}

@Composable private fun CarDetailScreen(car:Car?,onBack:()->Unit,onEdit:()->Unit,onMileage:()->Unit,onFill:()->Unit,onDelete:()->Unit){if(car==null){onBack();return};Page("Автомобиль"){TextButton(onClick=onBack){Text("← Назад",color=ColorBlue)};Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=CardColor)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("${car.brand} ${car.model}",color=TextPrimary,fontSize=22.sp,fontWeight=FontWeight.Bold);InfoRow("Год",car.year);InfoRow("Пробег","${car.mileage} км");InfoRow("VIN",car.vin);InfoRow("Двигатель",car.engine);InfoRow("Топливо",car.fuel);InfoRow("ГБО",if(car.hasLpg)"Установлено" else "Нет")}};Spacer(Modifier.height(12.dp));Button(onClick=onMileage,modifier=Modifier.fillMaxWidth()){Text("Изменить пробег")};Button(onClick=onFill,modifier=Modifier.fillMaxWidth()){Text("Заполнить регламент")};Button(onClick=onEdit,modifier=Modifier.fillMaxWidth()){Text("Редактировать")};TextButton(onClick=onDelete){Text("Удалить автомобиль",color=ColorRed)}}}

@Composable private fun PartsCatalogScreen(car:Car?,parts:List<PartCatalogItem>,onBack:()->Unit,onAdd:(PartCatalogItem)->Unit){if(car==null){Page("Запчасти и расходники"){Text("Сначала добавьте автомобиль",color=TextSecondary)};return};var show by remember{mutableStateOf(false)};Page("Запчасти и расходники"){TextButton(onClick=onBack){Text("← Назад",color=ColorBlue)};Text("Каталог создаётся вручную для этого автомобиля. Встроенной базы рекомендаций нет.",color=TextSecondary);Spacer(Modifier.height(8.dp));Button(onClick={show=true},modifier=Modifier.fillMaxWidth()){Text("Добавить запчасть / расходник")};Spacer(Modifier.height(8.dp));LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(parts.filter{it.carId==car.id}){p->Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=CardColor)){Column(Modifier.padding(12.dp)){Text(p.name,color=TextPrimary,fontWeight=FontWeight.Bold);Text("Артикул: ${p.article.ifBlank{"—"}}",color=TextSecondary);Text("Последняя цена: ${formatMoney(p.lastCost)} ${p.currency}",color=TextSecondary)}}}}};if(show)PartCatalogDialog(car.id,{show=false}){onAdd(it);show=false}}}

@Composable private fun PartCatalogDialog(carId:String,onDismiss:()->Unit,onSave:(PartCatalogItem)->Unit){var name by remember{mutableStateOf("")};var article by remember{mutableStateOf("")};var category by remember{mutableStateOf("Запчасть")};var cost by remember{mutableStateOf("")};var currency by remember{mutableStateOf("EUR")};AlertDialog(onDismissRequest=onDismiss,containerColor=CardColor,title={Text("Новая запчасть",color=TextPrimary)},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(name,{name=it},label={Text("Название")});OutlinedTextField(article,{article=it},label={Text("Артикул")});OutlinedTextField(category,{category=it},label={Text("Категория")});Row{OutlinedTextField(cost,{cost=it},label={Text("Цена")},modifier=Modifier.weight(1f));OutlinedTextField(currency,{currency=it.uppercase()},label={Text("Валюта")},modifier=Modifier.weight(1f))}},confirmButton={TextButton(onClick={if(name.isNotBlank())onSave(PartCatalogItem(carId=carId,name=name,article=article,category=category,lastCost=cost.toDoubleOrNull()?:0.0,currency=currency.uppercase()))}){Text("Сохранить",color=ColorGreen)}},dismissButton={TextButton(onClick=onDismiss){Text("Отмена",color=TextSecondary)}})}

@Composable private fun ErrorDecoderScreen(onBack:()->Unit){Page("Расшифровка кодов ошибок"){TextButton(onClick=onBack){Text("← Назад",color=ColorBlue)};Text("Введите код OBD-II, например P0300, чтобы получить расшифровку.",color=TextSecondary);Spacer(Modifier.height(8.dp));var code by remember{mutableStateOf("")};OutlinedTextField(code,{code=it.uppercase()},label={Text("Код ошибки")},singleLine=true,modifier=Modifier.fillMaxWidth());Spacer(Modifier.height(8.dp));if(code.isNotBlank())Text(obdMeaning(code),color=TextPrimary)}}
private fun obdMeaning(code:String)=when(code.uppercase()){"P0300"->"P0300 — случайные/множественные пропуски воспламенения.";"P0301"->"P0301 — пропуски воспламенения в цилиндре 1.";"P0302"->"P0302 — пропуски воспламенения в цилиндре 2.";"P0171"->"P0171 — слишком бедная смесь.";"P0172"->"P0172 — слишком богатая смесь.";"P0420"->"P0420 — эффективность системы нейтрализации ниже порога.";else->"Код $code: расшифровка пока отсутствует в локальном справочнике. Можно добавить её позже."}
