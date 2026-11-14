package com.potatoes.book5.realestate.data.sample

import com.potatoes.book5.realestate.domain.error.DomainError
import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.ApartmentDetail
import com.potatoes.book5.realestate.domain.model.Deal
import com.potatoes.book5.realestate.domain.model.PricePoint
import com.potatoes.book5.realestate.domain.port.ApartmentRepository
import com.potatoes.book5.realestate.domain.port.MarketSimulation
import java.time.LocalDate
import java.time.YearMonth

/**
 * 단지 상세와 실거래 샘플. 실거래가 API(T-005)가 연결되면 교체한다.
 * 단지명은 실제 단지와 무관한 예시이며 ID는 SampleRankingRepository와 같다.
 */
class SampleApartmentRepository : ApartmentRepository, MarketSimulation {

    private data class Profile(
        val name: String,
        val district: String,
        val neighborhood: String,
        val households: Int,
        val builtYear: Int,
        val minPyeong: Int,
        val maxPyeong: Int,
        val distanceKm: Double,
        val deals: List<Deal>,
    )

    private val profiles: MutableMap<String, Profile> = mutableMapOf(
        "sample-saesol" to Profile(
            "새솔 리버뷰", "분당구", "정자동", 1_520, 2018, 25, 45, 2.4,
            listOf(deal(2026, 10, 21, 34, 15, 245_000), deal(2026, 9, 2, 34, 8, 241_000), deal(2026, 7, 19, 45, 21, 298_000)),
        ),
        "sample-eunhaeng" to Profile(
            "은행나무 파크", "분당구", "이매동", 980, 2012, 24, 48, 4.1,
            listOf(deal(2026, 9, 30, 38, 11, 221_000), deal(2026, 8, 12, 38, 4, 216_000), deal(2026, 6, 3, 24, 9, 139_000)),
        ),
        "sample-cheongsol" to Profile(
            "청솔 하이츠", "분당구", "서현동", 1_102, 2006, 22, 41, 6.8,
            listOf(deal(2026, 10, 12, 33, 6, 198_000), deal(2026, 8, 28, 33, 13, 193_000), deal(2026, 5, 9, 41, 2, 230_000)),
        ),
        "sample-hanbit" to Profile(
            "한빛마을 1단지", "분당구", "정자동", 1_240, 2009, 24, 45, 1.2,
            listOf(deal(2026, 10, 30, 34, 12, 158_000), deal(2026, 9, 14, 34, 7, 155_000), deal(2026, 8, 2, 24, 3, 112_000)),
        ),
        "sample-moatteul" to Profile(
            "모아뜰 2단지", "분당구", "수내동", 860, 2003, 21, 38, 2.0,
            listOf(deal(2026, 10, 21, 32, 9, 134_000), deal(2026, 9, 5, 32, 15, 131_000), deal(2026, 7, 22, 21, 5, 88_000)),
        ),
        "sample-neuti" to Profile(
            "느티마을 3단지", "분당구", "정자동", 770, 1994, 18, 35, 1.6,
            listOf(deal(2026, 8, 14, 31, 10, 129_000), deal(2026, 6, 30, 31, 3, 126_000), deal(2026, 4, 11, 18, 7, 71_000)),
        ),
    )

    override suspend fun detail(apartmentId: String): Outcome<ApartmentDetail> {
        val p = profiles[apartmentId] ?: return Outcome.Failure(DomainError.Invalid("unknown-apartment"))
        val sorted = p.deals.sortedByDescending { it.date }
        val latest = sorted.first()
        return Outcome.Success(
            ApartmentDetail(
                id = apartmentId,
                name = p.name,
                district = p.district,
                neighborhood = p.neighborhood,
                households = p.households,
                builtYear = p.builtYear,
                minPyeong = p.minPyeong,
                maxPyeong = p.maxPyeong,
                distanceKm = p.distanceKm,
                latestDeal = latest,
                previousDeal = sorted.getOrNull(1),
                trend = trendEndingWith(latest),
                recentDeals = sorted.take(3),
            )
        )
    }

    override suspend fun latestDeal(apartmentId: String): Outcome<Deal> {
        val p = profiles[apartmentId] ?: return Outcome.Failure(DomainError.Invalid("unknown-apartment"))
        return Outcome.Success(p.deals.maxBy { it.date })
    }

    /**
     * 개발 빌드 전용. 모든 샘플 단지에 직전 최근 거래보다 3,000만 원 높은 새 거래를 하나씩 추가한다.
     * 스크랩 변동(새 실거래) 화면을 확인하는 데 쓴다.
     */
    @Synchronized
    override fun publishNewDeals() {
        profiles.replaceAll { _, p ->
            val latest = p.deals.maxBy { it.date }
            p.copy(deals = p.deals + latest.copy(date = latest.date.plusDays(7), floor = latest.floor % 20 + 1, priceManwon = latest.priceManwon + 3_000))
        }
    }

    private fun trendEndingWith(latest: Deal): List<PricePoint> {
        // 최근 1년을 완만하게 오르는 곡선으로 만든다. 실제 데이터 연결 전까지 차트 모양 확인용이다.
        val end = YearMonth.from(latest.date)
        val ratios = listOf(0.80, 0.82, 0.81, 0.85, 0.87, 0.86, 0.89, 0.92, 0.93, 0.96, 0.95, 1.0)
        return ratios.mapIndexed { i, r -> PricePoint(end.minusMonths((11 - i).toLong()), (latest.priceManwon * r).toLong()) }
    }

    private companion object {
        fun deal(y: Int, m: Int, d: Int, pyeong: Int, floor: Int, price: Long) = Deal(LocalDate.of(y, m, d), pyeong, floor, price)
    }
}
