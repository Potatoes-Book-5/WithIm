package com.potatoes.book5.realestate.data.sample

import com.potatoes.book5.realestate.domain.model.Apartment
import com.potatoes.book5.realestate.domain.model.GeoPoint
import com.potatoes.book5.realestate.domain.model.NearbyApartment
import com.potatoes.book5.realestate.domain.model.RankingSnapshot
import com.potatoes.book5.realestate.domain.model.VehiclePose
import com.potatoes.book5.realestate.domain.policy.GeoMath
import com.potatoes.book5.realestate.domain.port.RankingRepository
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * 와이어프레임과 같은 샘플 순위. 실거래가 API(T-005)와 차량 위치(F-001)가 연결되면 교체한다.
 * 단지명은 실제 단지와 무관한 예시다.
 */
class SampleRankingRepository : RankingRepository {

    private val areaIndex = MutableStateFlow(0)

    override fun observeNearbyRanking(): Flow<RankingSnapshot> = areaIndex.map { AREAS[it % AREAS.size] }

    /** 이동한 것처럼 다음 샘플 지역으로 바꾼다. 개발용 시뮬레이션에서 호출한다. */
    fun moveToNextArea() {
        areaIndex.value = areaIndex.value + 1
    }

    private companion object {
        val SAESOL = Apartment("sample-saesol", "새솔 리버뷰", 245_000, 34, LocalDate.of(2026, 10, 21))
        val EUNHAENG = Apartment("sample-eunhaeng", "은행나무 파크", 221_000, 38, LocalDate.of(2026, 9, 30))
        val CHEONGSOL = Apartment("sample-cheongsol", "청솔 하이츠", 198_000, 33, LocalDate.of(2026, 10, 12))
        val HANBIT = Apartment("sample-hanbit", "한빛마을 1단지", 158_000, 34, LocalDate.of(2026, 10, 30))
        val MOATTEUL = Apartment("sample-moatteul", "모아뜰 2단지", 134_000, 32, LocalDate.of(2026, 10, 21))
        val NEUTI = Apartment("sample-neuti", "느티마을 3단지", 129_000, 31, LocalDate.of(2026, 8, 14))

        /** 샘플 차량 위치. 동네 중심 근처 좌표이며 실제 주행 기록이 아니다. */
        private val JEONGJA = VehiclePose(GeoPoint(37.3670, 127.1080), headingDeg = 0.0)
        private val SUNAE = VehiclePose(GeoPoint(37.3785, 127.1150), headingDeg = 20.0)

        /** 차량 기준 거리·상대 방위에 맞는 좌표를 단지에 붙인다. */
        private fun VehiclePose.place(apartment: Apartment, km: Double, deg: Double) = NearbyApartment(
            apartment = apartment.copy(location = GeoMath.destination(location, km, requireNotNull(headingDeg) + deg)),
            distanceKm = km,
            bearingDeg = deg,
        )

        private fun VehiclePose.dot(id: String, km: Double, deg: Double) =
            place(Apartment(id, id, 0, 0, LocalDate.of(2026, 1, 1)), km, deg)

        val AREAS = listOf(
            with(JEONGJA) {
                RankingSnapshot(
                    neighborhoodName = "정자동",
                    radiusKm = 10,
                    top = listOf(
                        place(SAESOL, 2.4, 25.0),
                        place(EUNHAENG, 4.1, -40.0),
                        place(CHEONGSOL, 6.8, -160.0),
                    ),
                    interestMatches = listOf(place(HANBIT, 1.2, -70.0)),
                    others = listOf(
                        dot("o1", 3.2, 60.0), dot("o2", 2.8, -120.0), dot("o3", 4.5, 110.0),
                        dot("o4", 3.6, -15.0), dot("o5", 1.9, 150.0),
                    ),
                    dataAsOf = LocalDateTime.of(2026, 11, 13, 9, 0),
                    vehicle = this,
                )
            },
            with(SUNAE) {
                RankingSnapshot(
                    neighborhoodName = "수내동",
                    radiusKm = 10,
                    top = listOf(
                        place(EUNHAENG, 3.0, 10.0),
                        place(CHEONGSOL, 5.5, -30.0),
                        place(MOATTEUL, 2.0, 60.0),
                    ),
                    interestMatches = listOf(place(NEUTI, 1.6, -100.0)),
                    others = listOf(
                        dot("o6", 2.6, 40.0), dot("o7", 3.9, -70.0), dot("o8", 4.2, 140.0), dot("o9", 1.4, -150.0),
                    ),
                    dataAsOf = LocalDateTime.of(2026, 11, 13, 9, 0),
                    vehicle = this,
                )
            },
        )
    }
}
