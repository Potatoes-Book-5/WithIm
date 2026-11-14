package com.potatoes.book5.realestate.ui.common

import com.potatoes.book5.realestate.R
import com.potatoes.book5.realestate.domain.model.AreaRange
import com.potatoes.book5.realestate.domain.model.PriceRange

/** 관심 조건 구간의 표시 문구. 조건 화면과 홈이 함께 쓴다. */
fun PriceRange.labelRes(): Int = when (this) {
    PriceRange.UNDER_5 -> R.string.price_under_5
    PriceRange.FROM_5_TO_10 -> R.string.price_5_10
    PriceRange.FROM_10_TO_15 -> R.string.price_10_15
    PriceRange.FROM_15_TO_20 -> R.string.price_15_20
    PriceRange.OVER_20 -> R.string.price_over_20
}

fun AreaRange.labelRes(): Int = when (this) {
    AreaRange.UNDER_20 -> R.string.area_under_20
    AreaRange.PY_20S -> R.string.area_20s
    AreaRange.PY_30S -> R.string.area_30s
    AreaRange.OVER_40 -> R.string.area_over_40
}
