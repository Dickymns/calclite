package com.nimsaya.calclite.models

data class HistoryModel(
    var id: String = "",        // Untuk menyimpan kunci unik Firebase
    val formula: String = "",
    val result: String = "",
    var isSelected: Boolean = false // Untuk status centang
)