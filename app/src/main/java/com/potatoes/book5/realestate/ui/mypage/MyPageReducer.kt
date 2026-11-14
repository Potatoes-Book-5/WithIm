package com.potatoes.book5.realestate.ui.mypage

import com.potatoes.book5.realestate.domain.policy.MatchAxis
import com.potatoes.book5.realestate.domain.usecase.MyPageData
import com.potatoes.book5.realestate.domain.usecase.ScrapMatch

internal fun reduceMyPage(state: MyPageState, result: MyPageResult): MyPageState = when (result) {
    is MyPageResult.Loaded -> state.copy(
        isLoading = false,
        condition = result.condition,
        cards = result.cards,
        scrapCount = result.scrapCount,
        changedCount = result.changedCount,
    )
}

internal fun MyPageData.toResult() = MyPageResult.Loaded(
    condition = condition,
    cards = topMatches.map { it.toCard() },
    scrapCount = scrapCount,
    changedCount = changedCount,
)

private fun ScrapMatch.toCard(): MatchCardUi {
    val unseen = scrap.change?.takeIf { !it.seen }
    return MatchCardUi(
        id = scrap.apartmentId,
        name = scrap.name,
        scores = MatchAxis.entries.map { (match.scores[it] ?: 0.0).toFloat() },
        ratePercent = match.ratePercent,
        change = unseen?.kind,
        deltaManwon = unseen?.deltaManwon ?: 0,
    )
}
