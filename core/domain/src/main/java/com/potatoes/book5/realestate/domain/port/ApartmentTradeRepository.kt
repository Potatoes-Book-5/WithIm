package com.potatoes.book5.realestate.domain.port

import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.TradePage
import java.time.YearMonth

/** Region/month transaction feed, separate from per-apartment representative prices. */
interface ApartmentTradeRepository {
    suspend fun getTrades(
        districtCode: String,
        contractMonth: YearMonth,
        pageNumber: Int = 1,
        pageSize: Int = 100,
    ): Outcome<TradePage>
}
