package com.example.common.ui.main_screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.common.colorGrayWindow2
import com.example.common.colorText
import com.example.common.decodeFromFile
import com.example.common.encodeForSave
import com.example.common.enums.SaldoMode
import com.example.common.models.FutureSaldo
import com.example.common.models.ResultSaldo
import com.example.common.models.SaldoCell
import com.example.common.models.SaldoConfiguration
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.LocalDateTime

val colorDebit = Color(0xFF57DE5d)//Color(68,220,96)
val colorCredit = Color(0xFFDE7171)//Color(220,63,96)

val colorDebitResult = Color(68,200,96)
val colorCreditResult = Color(200,63,96)

val colorDebitStroke = Color(204,255,229)
val colorCreditStroke = Color(255,204,204)

val colorTextDebitTitle = Color(12, 144, 63 )
val colorTextCreditTitle = Color(144, 12, 63 )

var configurationOfSaldo = mutableStateOf<SaldoConfiguration>(
    SaldoConfiguration(
        investmentsAmount = 0,
        investmentsName = "input here description",
        startedDateMonth = 2,
        startedDateYear = 1997
    )
)

private var startDate = LocalDate(
    configurationOfSaldo.value.startedDateYear,
    configurationOfSaldo.value.startedDateMonth,
    1
)//LocalDateTime.of(2023,1,1)

var ShowWithDescription = true
var stateFall = arrayListOf<ArrayList<SaldoCell>>(
//    arrayListOf(SaldoCell(amount = 1), SaldoCell(amount = 1), SaldoCell(1), SaldoCell(1)),
//    arrayListOf(SaldoCell(amount = 1), SaldoCell(amount = 1), SaldoCell(1), SaldoCell(-1)),
//    arrayListOf(SaldoCell(amount = 12), SaldoCell(amount = 1), SaldoCell(1, isConst = true), SaldoCell(111))
)


private val waterFall = MutableSharedFlow<ArrayList<ArrayList<SaldoCell>>>()
internal val resultFall = MutableSharedFlow<ArrayList<ResultSaldo>>()
val futureFall = mutableStateOf<FutureSaldo?>(null)
val paybackPeriod = mutableStateOf<String>("")
internal var resultArray = arrayListOf<ResultSaldo>()

var isEditMode = mutableStateOf(false)
var saldoMode = mutableStateOf<SaldoMode>(SaldoMode.LOADING)



fun initital() {

    CoroutineScope(CoroutineName("Init")).launch {
        decodeFromFile()
        //delay(1000L)
        //updateWhole()
    }
}
fun updateWhole() {
    val crtScp = CoroutineScope(CoroutineName("Update"))
    //saldoMode.value = SaldoMode.LOADING
    resultArray.clear()

    // make result
    var lastSum = configurationOfSaldo.value.investmentsAmount//-1000//investments

    // for result
    var deltaForFuture = 0
    var incConst = 0
    var expConst = 0
    var futureIncome = listOf<Int>()
    var futureExpense = listOf<Int>()
    var dt: LocalDate? = null
    startDate = LocalDate(
        configurationOfSaldo.value.startedDateYear,
        configurationOfSaldo.value.startedDateMonth,
        1
    )
    //LocalDateTime.of(2023,1,1)
    println("> stateFall ${stateFall.joinToString()}")


    // in initial launch of app , when empty
    if (stateFall.isEmpty()) {
        saldoMode.value = SaldoMode.SHOW
        resultArray.add(
            ResultSaldo(LocalDate(year = LocalDateTime.now().year,LocalDateTime.now().month,LocalDateTime.now().dayOfMonth),0,0,0)
        )
        configurationOfSaldo.value = configurationOfSaldo.value.copy(startedDateYear = LocalDateTime.now().year, startedDateMonth = LocalDateTime.now().monthValue)
        crtScp.launch {
            resultFall.emit(resultArray)
        }
        return
    }

    stateFall.forEachIndexed { index, month ->
        //stateFall[index] = month.sortedBy { it.amount } as ArrayList<SaldoCell>
        stateFall[index] = ArrayList( month.sortedBy { it.amount })

        var incList = ArrayList(month.filter { it != null && it.amount > 0  })
        var expList = ArrayList(month.filter { it != null && it.amount < 0  })

        var income = incList.sumOf { it.amount }
        var expense = expList.sumOf { it.amount }

        lastSum = income + expense + lastSum
        println("<stateFall.forEachIndexed>>>> lastSum:${lastSum}  income:${income} expense:${expense}  investmentsAmount VM:${configurationOfSaldo.value.investmentsAmount} || ${month.joinToString { it.amount.toString() }}")
        dt = startDate.plus(DatePeriod(months = index))
        resultArray.add(ResultSaldo(date = dt, income = income, sum = lastSum, expense = expense))

        // future generate
        if (index == stateFall.size-1) {
            futureIncome = incList.filter { it.isConst }.map { it.amount }
            futureExpense = expList.filter { it.isConst }.map { it.amount }

            incConst = futureIncome.sum()
            expConst = futureExpense.sum()

            deltaForFuture = incConst + expConst
        }
    }

    // future generate
    val sum1 = resultArray.last().sum + deltaForFuture //lastSum + delta + resultArray.last().sum
    val sum2 = sum1 + deltaForFuture
    val sum3 = sum2 + deltaForFuture

    var cumulative = sum3
    var sumHalfYear: Int? = null
    var sumYear: Int?  = null
    var sumSecondYear: Int?  = null

    repeat(25) {
        cumulative += deltaForFuture

        when(it) {
            6 -> sumHalfYear = cumulative
            12 -> sumYear = cumulative
            24 -> sumSecondYear = cumulative
            else -> {}
        }
    }

    var forecast = FutureSaldo(
        income = incConst, expense = expConst,
        startForecastDate = dt,
        sum1 = sum1,
        sum2 = sum2,
        sum3 = sum3,
        incomes = futureIncome, expenses = futureExpense,
        periodHalfYear = sumHalfYear,
        periodFirstYear = sumYear,
        periodSecondYear = sumSecondYear
    )

    crtScp.async {
        resultFall.emit(arrayListOf())
        resultFall.emit(resultArray)

        waterFall.emit(arrayListOf())
        waterFall.emit(stateFall)

        //futureFall.emit(null)
        futureFall.value = forecast
        println("===========================>")
        println("~refresh stateFall-> ${stateFall.joinToString()}")
        println("~refresh resultArray-> ${resultArray.joinToString()}")
        println("~refresh forecast-> ${forecast.toString()}")
        println("<===========================")
        saldoMode.value = SaldoMode.SHOW
    }

}

internal fun updateStroke(oldSaldo: SaldoCell, newSaldoCell: SaldoCell, parentIndex: Int) {
    if (newSaldoCell.amount == null) return

    stateFall[parentIndex].forEachIndexed { index, i ->

        if (i == oldSaldo) {
            stateFall[parentIndex][index] = newSaldoCell
            println("updateStroke:> ${stateFall[parentIndex].map { it.amount }.joinToString()}")
            updateWhole()
            return
        }
    }
    stateFall[parentIndex].sortedBy { it.amount }
}

private fun addNewSaldo() {
    if (stateFall.isEmpty()) {
        return
    }
    var toFuture = stateFall.last().filter { it.isConst }

    stateFall.add(toFuture as ArrayList<SaldoCell>)

    updateWhole()
}

internal fun addNewCell(newValue: SaldoCell?, parentIndex: Int) {
    if (newValue == null) return

    if (parentIndex >= stateFall.size) {
        var newArrayList = arrayListOf<SaldoCell>(newValue)


        stateFall.add(newArrayList)
    } else {
        stateFall[parentIndex].add(newValue)
        stateFall[parentIndex].sortedBy { it.amount }

        if (newValue.isConst) {
            var afterMatchCurrentMonth = false
            // add in another saldo`s
            stateFall.forEachIndexed { index, ints ->

                if (index == parentIndex) {
                    afterMatchCurrentMonth = true
                }
                if (index != parentIndex && afterMatchCurrentMonth) {

                    stateFall[index].add(newValue)
                }
            }
        }
    }
    println("const:${newValue.isConst} addNewStroke[ ${stateFall.joinToString()} ] ")
    updateWhole()
}
// TODO need check:
internal fun deleteCell(monthIndex: Int, saldoCell: SaldoCell, andFuture: Boolean = false) {
    if (monthIndex < stateFall.size) {
        //stateFall[monthIndex] = ArrayList(stateFall[monthIndex].minus(element = value))
        val indexRemovedElement = stateFall[monthIndex].indexOf(saldoCell)
        stateFall[monthIndex].removeAt(indexRemovedElement)

        if (andFuture) {
            stateFall.forEachIndexed { index, saldoCells ->
                if (index >= monthIndex && stateFall.size > index) {
                    val ire = stateFall[index].indexOf(saldoCell)
                    println(">>> ${ire} ${saldoCells.joinToString()}")
                    if (ire >= 0) {
                        stateFall[index].removeAt(ire)
                    }
                }
            }
        }
        println("safeDelete: [$saldoCell] ${stateFall.joinToString()}")
    } else {
        println("ERROR Y >")
    }
    updateWhole()
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun MainDashboard() {
    val crtcxt = rememberCoroutineScope()
    LaunchedEffect(true) {
        delay(1000L)
        updateWhole()
    }
    val iem = remember { isEditMode }
    val idm = remember { saldoMode }
    val col = waterFall.collectAsState(
        stateFall
    )
    val paybackPeriod_Internal = remember { paybackPeriod }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            Modifier.fillMaxSize() , verticalArrangement = Arrangement.SpaceAround//.fillMaxSize().background(colorBackgroundDark)//.fillMaxWidth()
        ) {
            AnimatedVisibility(
                idm.value == SaldoMode.SETUP_SETTINGS
            ) {
                EditorOfDate()
            }
            AnimatedVisibility(
                iem.value
            ) {
                Row(
                    Modifier.fillMaxWidth().shimmerEffectBlue().height(50.dp).clickable {
                        actionToSaveChanges()
                    }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                ) {

                    Text("Save Changes", color = colorGrayWindow2, fontSize = 30.sp)
                }
            }
            LazyRow(Modifier.fillMaxSize().background(
                colorGrayWindow2
            )) {

                item {
                    InitialInvestments()
                }

                if (col.value.isNotEmpty()) {
                    col.value.forEachIndexed { parentIndex, parentItem ->
                        item {
                            PlateOfMonth(parentIndex, parentItem)
                        }
                    }
                } else {
                    item {
                        PlateOfMonth(0, arrayListOf<SaldoCell>())
                    }
                }


                item {
                    Column(Modifier.fillMaxHeight().padding(top = 15.dp), verticalArrangement = Arrangement.Top) {
                        Box(modifier = Modifier.clickable {
                            addNewSaldo()
                        }
                            //.padding(4.dp)
                            .size(30.dp)
                            .aspectRatio(1f)
                            .background(colorText, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(modifier = Modifier, text = "+", color= colorGrayWindow2,   textAlign = TextAlign.Center)
                        }
                    }
                }
                item {
                    forecastGhostMonth(1)
                }
                item {
                    forecastGhostMonth(2)
                }
                item {
                    forecastGhostMonth(3)
                }
                item {
                    longForecast()
                }
            }

            Box(Modifier.fillMaxWidth().height(100.dp).background(Color.Red))
        }

        Card(modifier = Modifier.size(60.dp).padding(10.dp).align(Alignment.BottomStart), elevation = 15.dp, shape = RoundedCornerShape(14.dp)) {
            Box(modifier = Modifier.fillMaxSize().clickable {
                if (saldoMode.value == SaldoMode.SHOW) {
                    saldoMode.value = SaldoMode.SETUP_SETTINGS
                } else {
                    updateWhole()
                    saldoMode.value = SaldoMode.SHOW
                }
                //inputDateMode.value = !inputDateMode.value
            }) {
                Icon(modifier = Modifier.align(Alignment.Center),imageVector = Icons.Filled.Settings, contentDescription = "Settings")
            }
        }

        Card(modifier = Modifier.width(250.dp)
            .height(65.dp).padding(10.dp).align(Alignment.BottomCenter), elevation = 15.dp, shape = RoundedCornerShape(14.dp)) {
            Row(modifier = Modifier.fillMaxSize().background(Color.White).clickable {
                if (saldoMode.value == SaldoMode.SHOW) {
                    saldoMode.value = SaldoMode.SETUP_SETTINGS
                } else {
                    saldoMode.value = SaldoMode.SHOW
                }
            }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
                Text(modifier = Modifier.padding(4.dp), text = "Payback period:", color= Color.Black, textAlign = TextAlign.Center)
                Text(modifier = Modifier.padding(4.dp), text = paybackPeriod_Internal.value, color= Color.Black, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,style = MaterialTheme.typography.body1)
            }
        }
    }
}

fun actionToSaveChanges() {

    CoroutineScope(CoroutineName("Action to save")).launch {
        isEditMode.value = false
        updateWhole()
        encodeForSave()
    }

}
private fun tester1() {
    GlobalScope.launch {
        repeat(100) {
            stateFall = arrayListOf(
                arrayListOf(SaldoCell(1), SaldoCell(amount = (-10..20).random())),
                arrayListOf(SaldoCell(1), SaldoCell(amount = (-20..30).random())),
                arrayListOf(SaldoCell(1), SaldoCell(amount = (-30..40).random())),
            )
            updateWhole()
            delay(10)
        }
    }
}