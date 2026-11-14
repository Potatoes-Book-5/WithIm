package com.potatoes.book5.realestate.ui.common

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun `만원 단위 가격을 억과 만으로 표시한다`() {
        assertEquals("24억 5,000", formatPrice(245_000))
        assertEquals("15억", formatPrice(150_000))
        assertEquals("9,800만", formatPrice(9_800))
        assertEquals("1억 200", formatPrice(10_200))
    }

    @Test
    fun `짧은 가격은 소수 한 자리 억으로 표시한다`() {
        assertEquals("24.5억", formatPriceShort(245_000))
        assertEquals("15억", formatPriceShort(150_000))
        assertEquals("22.1억", formatPriceShort(221_000))
    }

    @Test
    fun `거리와 날짜 형식`() {
        assertEquals("2.4km", formatDistance(2.4))
        assertEquals("2026.10.21", formatDealDate(LocalDate.of(2026, 10, 21)))
        assertEquals("11.13 09:00", formatDataAsOf(LocalDateTime.of(2026, 11, 13, 9, 0)))
    }
}
