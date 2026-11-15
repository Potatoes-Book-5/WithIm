package com.potatoes.book5.realestate.ui.summary

import com.potatoes.book5.realestate.domain.model.DriveSummary
import com.potatoes.book5.realestate.ui.Screen

data class DriveSummaryState(
    val isLoading: Boolean = true,
    /** 요약이 없으면 null(빈 상태) */
    val summary: DriveSummary? = null,
    val scrappedIds: Set<String> = emptySet(),
    /** 스크랩을 바꾸는 중인 단지 */
    val togglingIds: Set<String> = emptySet(),
)

sealed interface DriveSummaryIntent {
    data object Close : DriveSummaryIntent
    data class ToggleScrap(val apartmentId: String) : DriveSummaryIntent
    data class OpenApartment(val apartmentId: String) : DriveSummaryIntent
    data class OpenRecommendation(val recommendation: com.potatoes.book5.realestate.domain.model.DriveRecommendation) : DriveSummaryIntent
    data object OpenScraps : DriveSummaryIntent
    /** SCR-008에서 같은 주행의 관심 매물 모아보기(SCR-011)를 연다. */
    data object OpenInterestCatchUp : DriveSummaryIntent
}

sealed interface DriveSummaryResult {
    data class SummaryLoaded(val summary: DriveSummary?) : DriveSummaryResult
    data class ScrappedLoaded(val ids: Set<String>) : DriveSummaryResult
    data class Toggling(val apartmentId: String, val inProgress: Boolean) : DriveSummaryResult
}

sealed interface DriveSummaryEffect {
    data object Close : DriveSummaryEffect
    data class Navigate(val screen: Screen) : DriveSummaryEffect
    data object ShowScrapFailed : DriveSummaryEffect
}
