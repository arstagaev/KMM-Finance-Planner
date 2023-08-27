package com.example.common.models

import kotlinx.serialization.Serializable

@Serializable
data class SaveContainer(val data: ArrayList<ArrayList<SaldoCell>>)

@Serializable
data class SaldoConfiguration(
    var investmentsAmount: Int,
    var investmentsName: String,
    var startedDateMonth: Int,
    var startedDateYear: Int,
    var currentCurrency: String = "₽"
)

@Serializable
data class SaldoCell(var amount: Int, var name: String = "", var isConst: Boolean = false)