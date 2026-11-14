package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.error.DomainError
import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.ApartmentTrade
import com.potatoes.book5.realestate.domain.model.RegionalTradeFeed
import com.potatoes.book5.realestate.domain.port.ApartmentTradeRepository
import java.time.YearMonth

/** Supplies reported transactions to F-002 without inventing a vehicle location or complex price. */
class GetYeongtongTradeFeedUseCase(private val repository: ApartmentTradeRepository) {
    suspend operator fun invoke(contractMonth: YearMonth, pageSize: Int = 100): Outcome<RegionalTradeFeed> {
        if (pageSize !in 1..1000) return Outcome.Failure(DomainError.Invalid("invalid-trade-query"))
        val trades = mutableListOf<ApartmentTrade>()
        var pageNumber = 1
        var totalCount: Int? = null
        do {
            val page = when (val result = repository.getTrades(DISTRICT_CODE, contractMonth, pageNumber, pageSize)) {
                is Outcome.Failure -> return result
                is Outcome.Success -> result.value
            }
            if (totalCount != null && page.totalCount != totalCount) {
                return Outcome.Failure(DomainError.InvalidResponse)
            }
            totalCount = page.totalCount
            trades.addAll(page.trades)
            val next = page.nextPage ?: break
            pageNumber = next
        } while (true)
        if (trades.size != totalCount) return Outcome.Failure(DomainError.InvalidResponse)
        val recentFirst = trades.sortedWith(compareByDescending<ApartmentTrade> { it.contractDate }
            .thenByDescending { it.priceManwon })
        return Outcome.Success(RegionalTradeFeed(DISTRICT_CODE, DISTRICT_NAME, contractMonth, recentFirst))
    }

    companion object {
        const val DISTRICT_CODE = "41117"
        const val DISTRICT_NAME = "수원시 영통구"
    }
}
