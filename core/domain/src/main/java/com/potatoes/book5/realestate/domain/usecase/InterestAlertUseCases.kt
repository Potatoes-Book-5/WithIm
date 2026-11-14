package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.model.NearbyApartment
import com.potatoes.book5.realestate.domain.port.ConsentRepository
import com.potatoes.book5.realestate.domain.port.RankingRepository
import com.potatoes.book5.realestate.domain.port.ScrapRepository
import java.time.Duration
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** 관심 매물 발견 알림(F-005, F-006). */
data class InterestMatchAlert(val match: NearbyApartment, val at: LocalDateTime)

/**
 * 순위가 갱신될 때 관심 조건에 맞는 단지에 가까워지면 알린다(F-005 규칙 2~4, F-006 규칙 5·7).
 * - [alertDistanceKm](제안값 2km) 안의 관심 매물만 후보로 삼는다.
 * - 스크랩한 단지는 변동 알림(F-008) 대상이라 제외한다.
 * - 같은 단지는 알린 뒤 [suppress](제안값 24시간) 동안 다시 알리지 않는다. 기록은 앱 프로세스가 살아 있는 동안만 둔다(T-025 범위 밖).
 * - 관심 매물 알림이 꺼져 있으면 알리지 않는다.
 * - 후보가 여럿이면 가장 가까운 1건만 알린다. 나머지는 주행 요약(F-009)에 남는다.
 */
class ObserveInterestMatchAlertsUseCase(
    private val rankingRepository: RankingRepository,
    private val consentRepository: ConsentRepository,
    private val scrapRepository: ScrapRepository,
    private val alertDistanceKm: Double = 2.0,
    private val suppress: Duration = Duration.ofHours(24),
    private val now: () -> LocalDateTime = LocalDateTime::now,
) {
    private val alertedAt = mutableMapOf<String, LocalDateTime>()

    operator fun invoke(): Flow<InterestMatchAlert> = flow {
        rankingRepository.observeNearbyRanking().collect { snapshot ->
            if (snapshot.loading || snapshot.error != null || snapshot.vehicle == null) return@collect
            val near = snapshot.interestMatches.filter { it.distanceKm <= alertDistanceKm }
            if (near.isEmpty()) return@collect
            if (consentRepository.get()?.interestAlertEnabled != true) return@collect
            val at = now()
            val scrapped = scrapRepository.getAll().mapTo(HashSet()) { it.apartmentId }
            val target = near
                .filter { it.apartment.id !in scrapped }
                .filter { candidate -> alertedAt[candidate.apartment.id]?.let { Duration.between(it, at) >= suppress } ?: true }
                .minByOrNull { it.distanceKm }
                ?: return@collect
            alertedAt[target.apartment.id] = at
            emit(InterestMatchAlert(target, at))
        }
    }
}
