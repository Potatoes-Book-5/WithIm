package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.ApartmentDetail
import com.potatoes.book5.realestate.domain.model.PriceChange
import com.potatoes.book5.realestate.domain.model.PriceRecord
import com.potatoes.book5.realestate.domain.model.Scrap
import com.potatoes.book5.realestate.domain.model.ScrapChange
import com.potatoes.book5.realestate.domain.model.ScrapSummary
import com.potatoes.book5.realestate.domain.policy.ScrapChangePolicy
import com.potatoes.book5.realestate.domain.port.ApartmentRepository
import com.potatoes.book5.realestate.domain.port.MarketSimulation
import com.potatoes.book5.realestate.domain.port.ScrapRepository
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 확인하지 않은 변동이 있는 단지를 위로, 그다음 최근에 스크랩한 순서(SCR-007). */
internal val ScrapOrder: Comparator<Scrap> =
    compareByDescending<Scrap> { it.hasUnseenChange }.thenByDescending { it.scrappedAt }

class ObserveScrapsUseCase(private val scrapRepository: ScrapRepository) {
    operator fun invoke(): Flow<List<Scrap>> = scrapRepository.observeAll().map { it.sortedWith(ScrapOrder) }
}

/** 홈의 스크랩 카드 요약. 변동 건수는 확인하지 않은 변동만 센다(F-008 규칙 5). */
class ObserveScrapSummaryUseCase(private val scrapRepository: ScrapRepository) {
    operator fun invoke(): Flow<ScrapSummary> = scrapRepository.observeAll().map { scraps ->
        val unseen = scraps.filter { it.hasUnseenChange }.sortedByDescending { it.change?.detectedAt }
        ScrapSummary(
            totalCount = scraps.size,
            changedCount = unseen.size,
            firstChange = unseen.firstOrNull()?.let { ScrapChange(it.name, it.change!!.kind) },
        )
    }
}

class ObserveIsScrappedUseCase(private val scrapRepository: ScrapRepository) {
    operator fun invoke(apartmentId: String): Flow<Boolean> = scrapRepository.observeIsScrapped(apartmentId)
}

class GetScrapUseCase(private val scrapRepository: ScrapRepository) {
    suspend operator fun invoke(apartmentId: String): Scrap? = scrapRepository.get(apartmentId)
}

class GetApartmentDetailUseCase(private val apartmentRepository: ApartmentRepository) {
    suspend operator fun invoke(apartmentId: String): Outcome<ApartmentDetail> = apartmentRepository.detail(apartmentId)
}

/**
 * 스크랩을 켜고 끈다. 새로 스크랩하면 현재 최근 실거래를 기준 값과 마지막 확인 값으로 저장한다.
 * @return 바뀐 뒤의 스크랩 여부
 */
class ToggleScrapUseCase(
    private val scrapRepository: ScrapRepository,
    private val now: () -> LocalDateTime = LocalDateTime::now,
) {
    suspend operator fun invoke(detail: ApartmentDetail): Boolean {
        if (scrapRepository.get(detail.id) != null) {
            scrapRepository.delete(detail.id)
            return false
        }
        val at = now()
        val record = PriceRecord(detail.latestDeal.priceManwon, detail.latestDeal.date)
        scrapRepository.upsert(
            Scrap(
                apartmentId = detail.id,
                name = detail.name,
                neighborhood = detail.neighborhood,
                areaPyeong = detail.latestDeal.areaPyeong,
                scrappedAt = at,
                baseline = record,
                last = record,
                lastCheckedAt = at,
                change = null,
                households = detail.households,
                builtYear = detail.builtYear,
            )
        )
        return true
    }
}

/** 상세 화면을 열면 변동을 읽음으로 바꾼다(F-008 규칙 5). */
class MarkScrapChangeSeenUseCase(private val scrapRepository: ScrapRepository) {
    suspend operator fun invoke(apartmentId: String) = scrapRepository.markChangeSeen(apartmentId)
}

/**
 * 스크랩 단지마다 최신 실거래를 조회해 변동을 기록한다(F-008 규칙 4, 변동 판단 1~5).
 * 조회에 실패한 단지는 건너뛰고 기존 값을 유지한다(오프라인 동작).
 * @return 새로 기록한 변동 수
 */
class RefreshScrapChangesUseCase(
    private val scrapRepository: ScrapRepository,
    private val apartmentRepository: ApartmentRepository,
    private val now: () -> LocalDateTime = LocalDateTime::now,
) {
    /** v1에서 저장한 스크랩은 세대수·준공연도가 없으므로 상세에서 채운다(T-017). 실패하면 그대로 둔다. */
    private suspend fun withComplexInfo(scrap: Scrap): Scrap {
        if (scrap.households != null && scrap.builtYear != null) return scrap
        val detail = (apartmentRepository.detail(scrap.apartmentId) as? Outcome.Success)?.value ?: return scrap
        return scrap.copy(households = detail.households, builtYear = detail.builtYear)
    }

    suspend operator fun invoke(): Int {
        var changed = 0
        scrapRepository.getAll().forEach { stored ->
            val scrap = withComplexInfo(stored)
            val latest = (apartmentRepository.latestDeal(scrap.apartmentId) as? Outcome.Success)?.value ?: return@forEach
            val at = now()
            val detected = ScrapChangePolicy.detect(scrap.last, latest)
            val updated = if (detected == null) {
                scrap.copy(lastCheckedAt = at)
            } else {
                changed++
                scrap.copy(
                    last = PriceRecord(latest.priceManwon, latest.date),
                    lastCheckedAt = at,
                    change = PriceChange(detected.kind, detected.deltaManwon, at, seen = false),
                )
            }
            scrapRepository.upsert(updated)
        }
        return changed
    }
}

/** 개발 빌드 전용: 새 실거래를 흉내 낸 뒤 변동을 다시 확인한다. */
class SimulateNewDealsUseCase(
    private val simulation: MarketSimulation,
    private val refreshScrapChanges: RefreshScrapChangesUseCase,
) {
    suspend operator fun invoke(): Int {
        simulation.publishNewDeals()
        return refreshScrapChanges()
    }
}
