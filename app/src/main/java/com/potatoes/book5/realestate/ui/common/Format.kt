package com.potatoes.book5.realestate.ui.common

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 만원 단위 가격을 "24억 5,000" 형식으로. 1억 미만은 "9,800만". */
fun formatPrice(manwon: Long): String {
    val eok = manwon / 10_000
    val rest = manwon % 10_000
    return when {
        eok == 0L -> String.format(Locale.US, "%,d만", rest)
        rest == 0L -> "${eok}억"
        else -> String.format(Locale.US, "%d억 %,d", eok, rest)
    }
}

/** 지도·목록용 짧은 가격 "24.5억". */
fun formatPriceShort(manwon: Long): String {
    val value = manwon / 10_000.0
    val text = String.format(Locale.US, "%.1f", value).removeSuffix(".0")
    return "${text}억"
}

fun formatDistance(km: Double): String = String.format(Locale.US, "%.1fkm", km)

private val DEAL_DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd")
private val DATA_AS_OF = DateTimeFormatter.ofPattern("MM.dd HH:mm")

fun formatDealDate(date: LocalDate): String = date.format(DEAL_DATE)

fun formatDataAsOf(time: LocalDateTime): String = time.format(DATA_AS_OF)
