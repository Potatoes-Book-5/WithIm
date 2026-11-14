package com.potatoes.book5.realestate.ui.detail

import com.potatoes.book5.realestate.domain.model.ApartmentDetail
import com.potatoes.book5.realestate.domain.model.PriceChange

internal fun reduceDetail(state: DetailState, result: DetailResult): DetailState = when (result) {
    DetailResult.Loading -> state.copy(isLoading = true, failed = false)
    is DetailResult.Loaded -> state.copy(
        isLoading = false,
        failed = false,
        detail = result.detail,
        change = result.change,
        deltaManwon = result.deltaManwon,
    )
    DetailResult.Failed -> state.copy(isLoading = false, failed = true)
    is DetailResult.ScrapStateChanged -> state.copy(isScrapped = result.scrapped)
    is DetailResult.Toggling -> state.copy(isToggling = result.inProgress)
}

/**
 * 직전 대비 차이. 스크랩 변동이 있으면 그 값(마지막 확인 값 대비)을,
 * 없으면 같은 평형의 직전 거래와 비교한다. 평형이 다르면 비교하지 않는다.
 */
internal fun deltaFor(detail: ApartmentDetail, unseenChange: PriceChange?): Long? {
    if (unseenChange != null) return unseenChange.deltaManwon
    val previous = detail.previousDeal ?: return null
    if (previous.areaPyeong != detail.latestDeal.areaPyeong) return null
    return detail.latestDeal.priceManwon - previous.priceManwon
}
