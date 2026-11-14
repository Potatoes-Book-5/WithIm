package com.potatoes.book5.realestate.ui.scrap

import com.potatoes.book5.realestate.domain.model.Scrap

internal fun reduceScrapList(state: ScrapListState, result: ScrapListResult): ScrapListState = when (result) {
    is ScrapListResult.Loaded -> state.copy(isLoading = false, items = result.items)
    is ScrapListResult.FilterSelected -> state.copy(filter = result.filter)
}

internal fun Scrap.toItemUi(): ScrapItemUi {
    val unseen = change?.takeIf { !it.seen }
    return ScrapItemUi(
        id = apartmentId,
        name = name,
        neighborhood = neighborhood,
        areaPyeong = areaPyeong,
        priceManwon = last.priceManwon,
        scrappedOn = scrappedAt.toLocalDate(),
        change = unseen?.kind,
        deltaManwon = unseen?.deltaManwon ?: 0,
    )
}
