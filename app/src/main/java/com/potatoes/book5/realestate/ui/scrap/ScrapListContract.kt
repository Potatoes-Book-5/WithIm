package com.potatoes.book5.realestate.ui.scrap

import com.potatoes.book5.realestate.domain.model.ScrapChangeKind
import java.time.LocalDate

enum class ScrapFilter { ALL, CHANGED }

data class ScrapItemUi(
    val id: String,
    val name: String,
    val neighborhood: String,
    val areaPyeong: Int,
    /** 마지막으로 확인한 실거래가(만원) */
    val priceManwon: Long,
    val scrappedOn: LocalDate,
    /** 확인하지 않은 변동. 확인했거나 없으면 null(F-008 규칙 5) */
    val change: ScrapChangeKind?,
    val deltaManwon: Long,
) {
    val hasChange: Boolean get() = change != null
}

data class ScrapListState(
    val isLoading: Boolean = true,
    val filter: ScrapFilter = ScrapFilter.ALL,
    /** 변동 단지가 먼저 오도록 정렬된 전체 목록 */
    val items: List<ScrapItemUi> = emptyList(),
) {
    val totalCount: Int get() = items.size
    val changedCount: Int get() = items.count { it.hasChange }
    val visibleItems: List<ScrapItemUi>
        get() = if (filter == ScrapFilter.CHANGED) items.filter { it.hasChange } else items
}

sealed interface ScrapListIntent {
    data class SelectFilter(val filter: ScrapFilter) : ScrapListIntent
    data class OpenItem(val id: String) : ScrapListIntent
    data object Back : ScrapListIntent
}

sealed interface ScrapListResult {
    data class Loaded(val items: List<ScrapItemUi>) : ScrapListResult
    data class FilterSelected(val filter: ScrapFilter) : ScrapListResult
}

sealed interface ScrapListEffect {
    data class OpenDetail(val id: String) : ScrapListEffect
    data object Close : ScrapListEffect
}
