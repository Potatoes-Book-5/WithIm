package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.model.InterestCondition
import com.potatoes.book5.realestate.domain.model.Scrap
import com.potatoes.book5.realestate.domain.policy.ConditionMatch
import com.potatoes.book5.realestate.domain.policy.ConditionMatchPolicy
import com.potatoes.book5.realestate.domain.port.InterestRepository
import com.potatoes.book5.realestate.domain.port.ScrapRepository
import java.time.Year
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveInterestConditionUseCase(private val interestRepository: InterestRepository) {
    operator fun invoke(): Flow<InterestCondition?> = interestRepository.observe()
}

data class ScrapMatch(val scrap: Scrap, val match: ConditionMatch)

/** 마이 페이지(SCR-010) 데이터. 조건이 없으면 비교하지 않는다. */
data class MyPageData(
    val condition: InterestCondition?,
    /** 일치율 높은 순 상위 3곳. 조건이 없으면 비어 있다. */
    val topMatches: List<ScrapMatch>,
    val scrapCount: Int,
    /** 확인하지 않은 변동이 있는 스크랩 수 */
    val changedCount: Int,
)

/** 관심 조건과 스크랩을 함께 관찰해 일치율 상위 3곳을 계산한다. 같은 일치율이면 최근 스크랩이 먼저다. */
class ObserveMyPageUseCase(
    private val interestRepository: InterestRepository,
    private val scrapRepository: ScrapRepository,
    private val currentYear: () -> Int = { Year.now().value },
) {
    operator fun invoke(): Flow<MyPageData> =
        combine(interestRepository.observe(), scrapRepository.observeAll()) { condition, scraps ->
            val usable = condition?.takeUnless { it.isEmpty }
            val year = currentYear()
            val top = if (usable == null) emptyList() else scraps
                .map { ScrapMatch(it, ConditionMatchPolicy.match(usable, it, year)) }
                .sortedWith(compareByDescending<ScrapMatch> { it.match.ratePercent }.thenByDescending { it.scrap.scrappedAt })
                .take(TOP_COUNT)
            MyPageData(
                condition = usable,
                topMatches = top,
                scrapCount = scraps.size,
                changedCount = scraps.count { it.hasUnseenChange },
            )
        }

    private companion object {
        const val TOP_COUNT = 3
    }
}
