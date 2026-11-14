package com.potatoes.book5.realestate.ui.home

import com.potatoes.book5.realestate.domain.model.AreaRange
import com.potatoes.book5.realestate.domain.model.PriceRange
import com.potatoes.book5.realestate.domain.model.ScrapChangeKind
import com.potatoes.book5.realestate.ui.Screen
import com.potatoes.book5.realestate.ui.common.RankItemUi
import java.time.LocalDateTime

data class ScrapCardUi(
    val totalCount: Int,
    val changedCount: Int,
    val firstChangedName: String?,
    val firstChangeKind: ScrapChangeKind?,
)

data class InterestCardUi(
    val neighborhoods: List<String>,
    val prices: Set<PriceRange>,
    val areas: Set<AreaRange>,
) {
    val isEmpty: Boolean get() = neighborhoods.isEmpty() && prices.isEmpty() && areas.isEmpty()
}

data class HomeState(
    val isLoading: Boolean = true,
    val neighborhoodName: String = "",
    val radiusKm: Int = 10,
    val dataAsOf: LocalDateTime? = null,
    /** 1~3위. 첫 항목이 대장 아파트 */
    val ranking: List<RankItemUi> = emptyList(),
    val scrap: ScrapCardUi? = null,
    val interest: InterestCardUi? = null,
    /** 오늘 주변에서 찾은 관심 매물 수 */
    val todayMatches: Int = 0,
) {
    val leader: RankItemUi? get() = ranking.firstOrNull()
    val followers: List<RankItemUi> get() = ranking.drop(1)
}

/** 아직 구현하지 않은 화면 */
enum class HomeDestination { MY_PAGE, SETTINGS, DRIVE_SUMMARY }

sealed interface HomeIntent {
    data object OpenMyPage : HomeIntent
    data object OpenSettings : HomeIntent
    data object OpenInterest : HomeIntent
    data object OpenScrapChanges : HomeIntent
    data object OpenScraps : HomeIntent
    data object OpenDriveSummary : HomeIntent
    data class OpenApartment(val id: String) : HomeIntent
}

sealed interface HomeResult {
    data class RankingLoaded(
        val neighborhoodName: String,
        val radiusKm: Int,
        val dataAsOf: LocalDateTime,
        val ranking: List<RankItemUi>,
        val todayMatches: Int,
    ) : HomeResult
    data class ScrapLoaded(val scrap: ScrapCardUi) : HomeResult
    data class InterestLoaded(val interest: InterestCardUi) : HomeResult
}

sealed interface HomeEffect {
    data class Navigate(val screen: Screen) : HomeEffect
    data class ShowComingSoon(val destination: HomeDestination) : HomeEffect
}
