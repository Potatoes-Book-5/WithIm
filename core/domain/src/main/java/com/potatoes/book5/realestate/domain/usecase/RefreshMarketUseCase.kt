package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.port.MarketRepository

class RefreshMarketUseCase(private val repository: MarketRepository) {
    suspend operator fun invoke() = repository.refresh()
}
