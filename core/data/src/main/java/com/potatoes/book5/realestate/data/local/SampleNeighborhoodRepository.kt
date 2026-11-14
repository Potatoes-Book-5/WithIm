package com.potatoes.book5.realestate.data.local

import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.Neighborhood
import com.potatoes.book5.realestate.domain.port.NeighborhoodRepository

/**
 * 임시 추천 동네 목록이다. 차량 위치(F-001)와 동네 데이터 출처(T-005)가 정해지면
 * 현재 위치 주변 추천으로 교체한다.
 */
class SampleNeighborhoodRepository : NeighborhoodRepository {
    override suspend fun suggestions(): Outcome<List<Neighborhood>> = Outcome.Success(SAMPLE)

    private companion object {
        val SAMPLE = listOf("정자동", "수내동", "서현동", "판교동", "이매동")
            .map { Neighborhood(code = "sample-$it", name = it) }
    }
}
