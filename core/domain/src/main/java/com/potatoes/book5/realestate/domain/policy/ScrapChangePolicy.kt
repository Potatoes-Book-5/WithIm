package com.potatoes.book5.realestate.domain.policy

import com.potatoes.book5.realestate.domain.model.Deal
import com.potatoes.book5.realestate.domain.model.PriceRecord
import com.potatoes.book5.realestate.domain.model.ScrapChangeKind

data class DetectedChange(val kind: ScrapChangeKind, val deltaManwon: Long)

/**
 * 마지막 확인 값과 최신 실거래를 비교해 변동을 판단한다(F-008 변동 판단 1~3).
 * - 거래일이 더 늦으면 새 실거래
 * - 거래일이 같고 가격만 다르면 가격 상승·하락
 * - 같으면 변동 없음(null)
 */
object ScrapChangePolicy {
    fun detect(last: PriceRecord, latest: Deal): DetectedChange? {
        val delta = latest.priceManwon - last.priceManwon
        return when {
            latest.date.isAfter(last.dealDate) -> DetectedChange(ScrapChangeKind.NEW_DEAL, delta)
            latest.date.isEqual(last.dealDate) && delta > 0 -> DetectedChange(ScrapChangeKind.PRICE_UP, delta)
            latest.date.isEqual(last.dealDate) && delta < 0 -> DetectedChange(ScrapChangeKind.PRICE_DOWN, delta)
            else -> null
        }
    }
}
