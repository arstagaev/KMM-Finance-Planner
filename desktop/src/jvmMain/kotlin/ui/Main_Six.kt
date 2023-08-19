package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

private val waterFall = MutableSharedFlow<ArrayList<ArrayList<SaldoCell>>>()
private val resultFall = MutableSharedFlow<ArrayList<ResultSaldo>>()
val futureFall = mutableStateOf<FutureSaldo?>(null)
private var stateFall = arrayListOf<ArrayList<SaldoCell>>(
    arrayListOf(SaldoCell(amount = 1),SaldoCell(amount = 1),SaldoCell(1),SaldoCell(1)),
    arrayListOf(SaldoCell(amount = 1),SaldoCell(amount = 1),SaldoCell(1),SaldoCell(-1)),
    arrayListOf(SaldoCell(amount = 12),SaldoCell(amount = 1),SaldoCell(1, isConst = true),SaldoCell(111))
)
private var resultArray = arrayListOf<ResultSaldo>()

private var isEditMode = mutableStateOf(false)

data class ResultSaldo(
    val income: Int, val sum: Int, val expense: Int//, var isForecast: Boolean = false//, val arrayIncome: ArrayList<Int>, val arrayExpense: ArrayList<Int>
)
data class FutureSaldo(
    val investments: Int,
    val income: Int,
    val sum1: Int,
    val sum2: Int,
    val sum3: Int,
    val expense: Int, var incomes: List<Int>, var expenses: List<Int>,
    var periodHalfYear: Int? = null,
    var periodFirstYear: Int? = null,
    var periodSecondYear: Int? = null,
)

data class SaldoCell(var amount: Int, var name: String? = null, var isConst: Boolean = false)

fun updateXXX() {
    var investments = 100
    resultArray.clear()
    var lastSum = investments
    stateFall.forEach { month ->
        var incList = ArrayList(month.filter { it != null && it.amount > 0  })
        var expList = ArrayList(month.filter { it != null && it.amount < 0  })

        var income = incList.sumOf { it.amount }
        var expense = expList.sumOf { it.amount }

        lastSum += income + expense

        resultArray.add(ResultSaldo(income = income, sum = lastSum, expense = expense))
    }
    // make forecast
    val lastMonthConsts = stateFall.last().filter { it.isConst }
    val incomeConst = lastMonthConsts.filter { it.amount > 0 }.map { it.amount }
    val expenseConst = lastMonthConsts.filter { it.amount < 0 }.map { it.amount }

    val sumIncConst = incomeConst.sum()
    val sumExpConst = expenseConst.sum()

    val delta = sumIncConst - sumExpConst

    val sum1 = lastSum + delta
    val sum2 = sum1 + delta
    val sum3 = sum2 + delta

    var cumulative = sum3
    var sumHalfYear: Int? = null
    var sumYear: Int?  = null
    var sumSecondYear: Int?  = null

    repeat(25) {
        cumulative += delta

        when(it) {
            6 -> {
                sumHalfYear = cumulative
            }
            12 -> {
                sumYear = cumulative
            }
            24 -> {
                sumSecondYear = cumulative
            }
            else -> {}
        }
    }

    var forecast = FutureSaldo(investments = investments,
        income = sumIncConst, expense = sumExpConst,
        sum1 = sum1,
        sum2 = sum2,
        sum3 = sum3,
        incomes = incomeConst, expenses = expenseConst,
        periodHalfYear = sumHalfYear,
        periodFirstYear = sumYear,
        periodSecondYear = sumSecondYear
    )

    GlobalScope.async {
//        stateFall.forEachIndexed { index, ints ->
//            stateFall[index].sortDescending()
//        }
        resultFall.emit(arrayListOf())
        resultFall.emit(resultArray)

        waterFall.emit(arrayListOf())
        waterFall.emit(stateFall)

        //futureFall.emit(null)
        futureFall.value = forecast



        println("refresh-> ${stateFall.joinToString()}")
    }
}

private fun updateStroke(oldValue: Int, newSaldoCell: SaldoCell, parentIndex: Int) {
    if (newSaldoCell.amount == null) return
    stateFall[parentIndex].forEachIndexed { index, i ->
        if (i.amount == oldValue) {
            stateFall[parentIndex][index] = newSaldoCell
            println("updateStroke:> ${stateFall[parentIndex].map { it.amount }.joinToString()}")
            updateXXX()
            return
        }
    }


}

private fun addNewSaldo() {
    var toFuture = stateFall.last().filter { it.isConst }

    stateFall.add(toFuture as ArrayList<SaldoCell>)

    updateXXX()
}

private fun addNewCell(newValue: SaldoCell?, parentIndex: Int) {
    if (newValue == null) return

    if (parentIndex >= stateFall.size) {
        var newArrayList = arrayListOf<SaldoCell>(newValue)


        stateFall.add(newArrayList)
    } else {
        stateFall[parentIndex].add(newValue)

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
    updateXXX()
}
// TODO need check:
private fun deleteCell(monthIndex: Int, value: SaldoCell, andFuture: Boolean = false) {
    if (monthIndex < stateFall.size) {
        //stateFall[monthIndex] = ArrayList(stateFall[monthIndex].minus(element = value))
        stateFall[monthIndex].remove(element = value)
//        stateFall[monthIndex].forEachIndexed { index, saldoCell ->
//            if ()
//            stateFall[monthIndex].removeAt(index)
//        }
        if (andFuture) {
            // remove in another saldo`s
            stateFall[monthIndex].forEachIndexed { indexY, ints ->
                if (indexY != monthIndex && stateFall.size > indexY) {
                    stateFall[indexY] = ArrayList(stateFall[indexY].minus(value))
                }
            }
        }
        //return this
        println("safeDelete: [$value] ${stateFall.joinToString()}")
    }else {
        //return arrayListOf()
        println("ERROR Y >")
    }
    updateXXX()
}

@Composable
fun AppX2() {

    LaunchedEffect(true) {
        println("At start: ${stateFall.joinToString()}")
        updateXXX()
        //tester1()
    }
    val iem = remember { isEditMode }
    val col = waterFall.collectAsState(
        stateFall
    )


    Column(
        Modifier//.fillMaxWidth()
    ) {
        AnimatedVisibility(
            iem.value
        ) {
            Row(
                Modifier.fillMaxWidth().height(50.dp).background(Color.Red).clickable {
                    isEditMode.value = false
                    //actionSave.value = true
                    println("refresh-> ${stateFall.joinToString()}")
                    updateXXX()
                }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recalculate", fontSize = 30.sp)
            }
        }
        LazyRow {
//            itemsIndexed(col.value, itemContent = {parentIndex, parentItem ->
//                monthZero(parentItem, parentIndex)
//            })
            item {
                var futureSaldo = remember { futureFall }
                val textInvest = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.Blue)) {
                        append("Initial investments:")
                    }
                    append("\n" + "${futureSaldo.value?.investments}")
                }
                Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                    Card(
                        modifier = Modifier
                            .width(150.dp)
                            .height(100.dp)
                            .padding(5.dp),
                        elevation = 10.dp
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            Text(textInvest,
                                modifier = Modifier.padding(4.dp).align(Alignment.Center)
                                    .clickable {},
                                fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                                //color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                        }

                    }
                }
            }
            col.value.forEachIndexed { parentIndex, parentItem ->
                item {
                    //monthZero(parentItem, parentIndex)
//                    var incList = parentItem.filter { it != null && it.amount > 0  }
//                    var expList = parentItem.filter { it != null && it.amount < 0  }
//
//                    var income = remember { mutableStateOf(incList.sum()) }
//                    var expense = remember { mutableStateOf(expList.sum()) }
//
//                    LaunchedEffect(incList, expList) {
//                        income.value = incList.sum()
//                        expense.value = expList.sum()
//                    }
                    val res = resultFall.collectAsState(resultArray)

                    Card(
                        modifier = Modifier
                            .width(150.dp)
                            .padding(5.dp),
                        //elevation = 10.dp
                    ) {
                        Box(Modifier.fillMaxSize().background(Color.LightGray).clickable {  }) {
                            Text("${parentItem.size} ${parentIndex}", modifier = Modifier.fillMaxSize().padding(top = (1).dp,start = 0.dp).align(Alignment.TopCenter),
                                fontFamily = FontFamily.Default, fontSize = 10.sp, fontWeight = FontWeight.Light,
                                color = Color.LightGray
                            )

                            Column(
                                modifier = Modifier.fillMaxSize().padding(top = 15.dp), horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    Modifier.weight(3f).background(Color.Green))
                                {
                                    LazyColumn {
                                        itemsIndexed(
                                            parentItem.filter { it.amount > 0 },
                                            itemContent = { index, item ->
                                                strokeAgregator(item, parentIndex, index)
                                                //Text(">${item}")
                                            }
                                        )
                                        // circle "plus" for add new stroke of Saldo
                                        item {
                                            plusik(isIncome = true, parentIndex = parentIndex)
                                        }
                                    }
                                }

                                // SUMMA:
                                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.padding(end = 10.dp)) {
                                        Text("Σ Income: ${if (res.value.size > parentIndex) res.value[parentIndex].income else 0}", modifier = Modifier.padding(vertical = 2.dp),
                                            fontFamily = FontFamily.Default, fontSize = 10.sp, fontWeight = FontWeight.Bold,textAlign = TextAlign.Center,
                                            //color = Color.Green
                                        )
                                        Text("Σ Expense: ${if (res.value.size > parentIndex) res.value[parentIndex].expense else 0}", modifier = Modifier.padding(vertical = 2.dp),
                                            fontFamily = FontFamily.Default, fontSize = 10.sp, fontWeight = FontWeight.Bold,textAlign = TextAlign.Center,
                                            //color = Color.Red
                                        )
                                    }

                                    Text("${ if (res.value.size > parentIndex) res.value[parentIndex].sum else 0}", modifier = Modifier.padding(vertical = 5.dp).clickable {
                                        GlobalScope.async {
                                            updateXXX()
                                        }
                                    },
                                        fontFamily = FontFamily.Default, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold,textAlign = TextAlign.Center,
                                        color = Color.DarkGray
                                    )


                                }

                                Row(
                                    Modifier.weight(3f).background(Color.Red)
                                ) {
                                    LazyColumn {
                                        itemsIndexed(parentItem.filter { it.amount < 0 }, itemContent = { index, itemStroke ->
                                            //Text(">${item}")
                                            strokeAgregator(itemStroke, parentIndex, index, isIncome = true)

                                        })
                                        // circle "plus" for add new stroke of Saldo
                                        item {
                                            plusik(isIncome = false, parentIndex)
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
            }
            item {
                Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                    Box(modifier = Modifier.clickable {
                        addNewSaldo()
                    }
                        //.padding(4.dp)
                        .size(30.dp)
                        .aspectRatio(1f)
                        .background(Color.White, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(modifier = Modifier, text = "+", color= Color.Black,   textAlign = TextAlign.Center)
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
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun strokeAgregator(saldoCell: SaldoCell, parentIndex: Int, index: Int, isIncome: Boolean = true) {
    val oldvalue = saldoCell.amount
    var isEdit = remember { mutableStateOf(false) }
    var detailShow = remember { mutableStateOf(false) }
    var saldoStrokeAmount = remember { mutableStateOf("${saldoCell.amount}") }
    var saldoStrokeName = remember { mutableStateOf("${saldoCell.name}") }
    var isShowRemoveIcon = remember { mutableStateOf(false) }

    LaunchedEffect(isEditMode.value) {
        if (!isEditMode.value) {
            if (isEdit.value) {
                updateStroke(oldValue = oldvalue, newSaldoCell = SaldoCell(amount = saldoStrokeAmount.value.toInt(), name = saldoStrokeName.value), parentIndex,)
                isEdit.value = false
            }
        }
    }
    Card (Modifier.fillMaxWidth().padding(1.dp), shape = RoundedCornerShape(5.dp),
        elevation = 10.dp) {

        Column(
            Modifier.fillMaxSize()//.width(100.dp)
                .background(Color.White)

//        .onPointerEvent(PointerEventType.Enter) {
//            val position = it.changes.first().position
//            //println("posss ${position.toString()}")
//            isShowRemoveIcon.value = true
//        }.onPointerEvent(PointerEventType.Exit) {
//            val position = it.changes.first().position
//            //println("posss ${position.toString()}")
//            isShowRemoveIcon.value = false
//        }
        ) {
            if (isEdit.value) {
                TextField(
                    modifier = Modifier.fillMaxWidth()//.height(40.dp)
                        .background(Color.Magenta),
                    value = saldoStrokeAmount.value.toString(),
                    onValueChange = {
                        val newNum = it.filter { it.isDigit() }
                        if (newNum.isNotEmpty()) {
                            saldoStrokeAmount.value = it
                        }
                        //println("->>${currentBudgetX.value.joinToString()}")
                        //println("stroke ${saldoStrokeAmount.value} ${CalcModule2.currentBudgetX.value.joinToString()}")
                    },
                    textStyle = TextStyle.Default.copy(fontSize = 15.sp)
                )
                Row(Modifier.clickable { deleteCell(monthIndex = parentIndex, value = saldoCell) }) {
                    Text("Delete", fontSize = 10.sp)
                }
                Row(Modifier.clickable { deleteCell(monthIndex = parentIndex, value = saldoCell, true) }) {
                    Text("Delete and future", fontSize = 10.sp)
                }
            } else {
                val text1 = buildAnnotatedString {
                    append("${saldoCell.amount} ${if (saldoCell.isConst) "✅" else ""} ")
                    withStyle(SpanStyle(color = Color.LightGray)) {
                        append("\n" + "${saldoCell.name}")
                    }
                }
                Row(Modifier.fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {},
                            onDoubleTap = {
                            },
                            onLongPress = {
                                detailShow.value = !detailShow.value
                            },
                            onTap = {
                                GlobalScope.launch {
                                    isEditMode.value = true
                                    isEdit.value = true
                                    //delay(100)
                                }
                            }
                        )
                    }
                ) {
                    if (!detailShow.value) {
                        Text(modifier = Modifier.padding(start = 5.dp), text = "${saldoCell.amount} ${if (saldoCell.isConst) "✅" else ""}")
                    } else {
                        Text(modifier = Modifier.padding(start = 5.dp), text =text1)
                    }

                }


            }
//        if (isShowRemoveIcon.value) {
//            Box(Modifier.fillMaxSize().align(Alignment.CenterEnd).background(Color.Red)
//                .clickable {
//                    deleteCell(monthIndex = parentIndex,value = saldoStrokeAmount.value)
//                })
//        }
        }
    }
}

@Composable
private fun plusik(isIncome: Boolean = true, parentIndex: Int) {
    var saldoStrokeAmount = remember { mutableStateOf("") }
    var saldoStrokeName = remember { mutableStateOf("") }
    val checkedState = remember { mutableStateOf(false) }
    //var saldoStrokeName = remember { mutableStateOf("") }
    var isEdit = remember { mutableStateOf(false) }
    var newCellSaldo = remember { mutableStateOf<SaldoCell>(SaldoCell(amount = 0,name = "")) }
    //var isEditByHuman = remember { mutableStateOf(false) }

    LaunchedEffect(isEditMode.value) {
        if (!isEditMode.value && saldoStrokeAmount.value.toString().isNotEmpty() && saldoStrokeAmount.value.toString().isNotBlank()) {
            if (isEdit.value) {
                val newValue = saldoStrokeAmount.value.toInt() ?: 0// newCellSaldo.value.amount.toInt()
                println("Prep1 ${newValue} ${checkedState.value}")

                //addNewCell(SaldoCell(amount = newValue * if(isIncome) 1 else -1), parentIndex)
                addNewCell(newCellSaldo.value.copy(amount = newValue * if (isIncome) 1 else -1, isConst = checkedState.value, name = saldoStrokeName.value), parentIndex)

                newCellSaldo.value.amount = 0
                isEdit.value = false
            }
        }
        if (!isEditMode.value) {
            if (isEdit.value) {
                //updateStroke(oldValue = oldvalue, newValue = saldoStrokeAmount.value.toInt(), parentIndex,)
                isEdit.value = false
            }
        }
    }

    Row(Modifier.fillMaxWidth()//.height(40.dp)
        .clickable {
        isEditMode.value = true
        isEdit.value = true
    }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Top) {
        if (isEdit.value) {
            Column {
                TextField(
                    modifier = Modifier.fillMaxWidth()//.height(40.dp)
                        .background(Color.Magenta),
                    //value = newCellSaldo.value.amount.toString(),
                    value = saldoStrokeAmount.value,
                    onValueChange = {
                        if (it.isNotEmpty()) {
                            //newCellSaldo.value.amount = it.toInt()
                            saldoStrokeAmount.value = it
                            //isEditByHuman.value = true
                        }


                    },
                    label = { Text("Enter new amount", fontSize = 15.sp) },
                    textStyle = TextStyle.Default.copy(fontSize = 12.sp)
                )
                TextField(
                    modifier = Modifier.fillMaxWidth()//.height(40.dp)
                        .background(Color.Magenta),
                    value = saldoStrokeName.value?:"",

                    onValueChange = {
                        if (it.isNotEmpty()) {
                            saldoStrokeName.value = it
                            //newCellSaldo.value.name = it
                            //isEditByHuman.value = true
                        }
                    },
                    label = { Text("Enter name for source of amount", fontSize = 15.sp) },
                    textStyle = TextStyle.Default.copy(fontSize = 10.sp),

                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = checkedState.value,
                        onCheckedChange = {
                            checkedState.value = it
                            //isEditByHuman.value = true
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Text(modifier = Modifier.padding(start = 5.dp), text = "is is permanent ${if(isIncome) "income" else "expense"}", fontSize = 12.sp)
                }
            }

        } else {
            Text(text = "+", style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

private fun tester1() {
    GlobalScope.launch {
        repeat(100) {
            stateFall = arrayListOf(
                arrayListOf(SaldoCell(1),SaldoCell(amount = (-10..20).random())),
                arrayListOf(SaldoCell(1),SaldoCell(amount = (-20..30).random())),
                arrayListOf(SaldoCell(1),SaldoCell(amount = (-30..40).random())),
            )
            updateXXX()
            delay(10)
        }
    }
}
