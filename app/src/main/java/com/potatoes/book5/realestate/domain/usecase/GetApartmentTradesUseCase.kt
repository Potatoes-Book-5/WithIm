package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.TradePage
import com.potatoes.book5.realestate.domain.port.ApartmentTradeRepository
import java.time.YearMonth

class GetApartmentTradesUseCase(private val repository: ApartmentTradeRepository) {
    suspend operator fun invoke(
        districtCode: String,
        contractMonth: YearMonth,
        pageNumber: Int = 1,
        pageSize: Int = 100,
    ): Outcome<TradePage> = repository.getTrades(districtCode, contractMonth, pageNumber, pageSize)
}
