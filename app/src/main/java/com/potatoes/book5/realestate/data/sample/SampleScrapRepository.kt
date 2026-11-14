package com.potatoes.book5.realestate.data.sample

import com.potatoes.book5.realestate.domain.model.ScrapChange
import com.potatoes.book5.realestate.domain.model.ScrapChangeKind
import com.potatoes.book5.realestate.domain.model.ScrapSummary
import com.potatoes.book5.realestate.domain.port.ScrapRepository

/** 스크랩 기능(F-008) 구현 전까지 쓰는 샘플 요약. */
class SampleScrapRepository : ScrapRepository {
    override suspend fun summary(): ScrapSummary = ScrapSummary(
        totalCount = 8,
        changedCount = 2,
        firstChange = ScrapChange("한빛마을 1단지", ScrapChangeKind.NEW_DEAL),
    )
}
