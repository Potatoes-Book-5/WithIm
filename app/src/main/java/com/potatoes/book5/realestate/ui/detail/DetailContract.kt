package com.potatoes.book5.realestate.ui.detail

import com.potatoes.book5.realestate.domain.model.ApartmentDetail
import com.potatoes.book5.realestate.domain.model.ScrapChangeKind

data class DetailState(
    val isLoading: Boolean = true,
    val failed: Boolean = false,
    val detail: ApartmentDetail? = null,
    val isScrapped: Boolean = false,
    val isToggling: Boolean = false,
    /** 화면을 열 때 확인하지 않았던 변동. 열면서 확인 처리하지만 이번 방문 동안은 보여 준다. */
    val change: ScrapChangeKind? = null,
    /** 직전 대비 가격 차이(만원). 비교할 거래가 없으면 null */
    val deltaManwon: Long? = null,
)

sealed interface DetailIntent {
    data object ToggleScrap : DetailIntent
    data object Retry : DetailIntent
    data object Back : DetailIntent
}

sealed interface DetailResult {
    data object Loading : DetailResult
    data class Loaded(val detail: ApartmentDetail, val change: ScrapChangeKind?, val deltaManwon: Long?) : DetailResult
    data object Failed : DetailResult
    data class ScrapStateChanged(val scrapped: Boolean) : DetailResult
    data class Toggling(val inProgress: Boolean) : DetailResult
}

sealed interface DetailEffect {
    data object Close : DetailEffect
    data object ShowScrapFailed : DetailEffect
}
