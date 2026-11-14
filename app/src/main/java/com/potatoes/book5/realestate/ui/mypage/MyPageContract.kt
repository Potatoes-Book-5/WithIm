package com.potatoes.book5.realestate.ui.mypage

import com.potatoes.book5.realestate.domain.model.InterestCondition
import com.potatoes.book5.realestate.domain.model.ScrapChangeKind
import com.potatoes.book5.realestate.ui.Screen

data class MatchCardUi(
    val id: String,
    val name: String,
    /** 오각형 축 순서(가격대, 평수, 동네, 세대수, 신축)의 0~5. 모르는 축은 0 */
    val scores: List<Float>,
    val ratePercent: Int,
    /** 확인하지 않은 변동 */
    val change: ScrapChangeKind?,
    val deltaManwon: Long,
)

data class MyPageState(
    val isLoading: Boolean = true,
    /** 저장된 관심 조건. 없거나 건너뛰었으면 null */
    val condition: InterestCondition? = null,
    val cards: List<MatchCardUi> = emptyList(),
    val scrapCount: Int = 0,
    val changedCount: Int = 0,
)

sealed interface MyPageIntent {
    data object Back : MyPageIntent
    data object EditConditions : MyPageIntent
    data class OpenApartment(val id: String) : MyPageIntent
    data object OpenScraps : MyPageIntent
}

sealed interface MyPageResult {
    data class Loaded(
        val condition: InterestCondition?,
        val cards: List<MatchCardUi>,
        val scrapCount: Int,
        val changedCount: Int,
    ) : MyPageResult
}

sealed interface MyPageEffect {
    data class Navigate(val screen: Screen) : MyPageEffect
    data object Close : MyPageEffect
}
