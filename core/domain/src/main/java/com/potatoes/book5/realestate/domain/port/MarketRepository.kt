package com.potatoes.book5.realestate.domain.port

import com.potatoes.book5.realestate.domain.error.Outcome

/** One real transaction source shared by ranking, details and neighborhood suggestions. */
interface MarketRepository : RankingRepository, ApartmentRepository, NeighborhoodRepository {
    suspend fun refresh(): Outcome<Unit>
    suspend fun run()
}
