package com.spendwise.app.util

import com.spendwise.app.model.TransactionType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formatters {

    private val vnLocale: Locale = Locale.forLanguageTag("vi-VN")

    private val moneyFormat = DecimalFormat("#,###", DecimalFormatSymbols(vnLocale).apply {
        groupingSeparator = '.'
    })

    fun money(amount: Long): String = "${moneyFormat.format(amount)} đ"

    fun signedMoney(amount: Long, type: TransactionType): String =
        if (type == TransactionType.INCOME) "+${money(amount)}" else "-${money(amount)}"

    /** Định dạng ngoại tệ, vd: 38,42 USD (JPY/KRW không có số lẻ). */
    fun foreign(value: Double, code: String): String {
        val decimals = if (code in setOf("JPY", "KRW", "TWD")) 0 else 2
        val fmt = DecimalFormat(if (decimals == 0) "#,##0" else "#,##0.00", DecimalFormatSymbols(vnLocale))
        return "${fmt.format(value)} $code"
    }

    fun dateTime(millis: Long): String = SimpleDateFormat("HH:mm dd/MM/yyyy", vnLocale).format(Date(millis))

    fun date(millis: Long): String = SimpleDateFormat("dd/MM/yyyy", vnLocale).format(Date(millis))

    fun month(year: Int, month: Int): String = "Tháng ${month + 1}/$year"

    fun isInMonth(millis: Long, year: Int, month: Int): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
    }
}
