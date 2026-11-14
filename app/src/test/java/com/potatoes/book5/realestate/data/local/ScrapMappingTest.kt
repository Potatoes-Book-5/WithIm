package com.potatoes.book5.realestate.data.local

import com.potatoes.book5.realestate.domain.model.PriceChange
import com.potatoes.book5.realestate.domain.model.PriceRecord
import com.potatoes.book5.realestate.domain.model.ScrapChangeKind
import com.potatoes.book5.realestate.fakes.scrapOf
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ScrapMappingTest {
    private val zone = ZoneId.of("Asia/Seoul")

    @Test
    fun `스크랩은 변동이 있든 없든 그대로 저장하고 읽는다`() {
        val plain = scrapOf("a")
        assertEquals(plain, plain.toEntity(zone).toDomain(zone))

        val changed = plain.copy(
            last = PriceRecord(153_000, LocalDate.of(2026, 10, 8)),
            change = PriceChange(ScrapChangeKind.PRICE_DOWN, -2_000, LocalDateTime.of(2026, 11, 14, 10, 30), seen = false),
        )
        assertEquals(changed, changed.toEntity(zone).toDomain(zone))

        val withComplex = plain.copy(households = 1_240, builtYear = 2009)
        assertEquals(withComplex, withComplex.toEntity(zone).toDomain(zone))
    }
}
