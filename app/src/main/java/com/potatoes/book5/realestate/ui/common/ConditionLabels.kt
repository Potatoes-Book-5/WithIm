package com.potatoes.book5.realestate.ui.common

import com.potatoes.book5.realestate.R
import com.potatoes.book5.realestate.domain.model.AreaRange
import com.potatoes.book5.realestate.domain.model.BuildingAge
import com.potatoes.book5.realestate.domain.model.HouseholdMin
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

fun HouseholdMin.labelRes(): Int = when (this) {
    HouseholdMin.OVER_300 -> R.string.households_300
    HouseholdMin.OVER_500 -> R.string.households_500
    HouseholdMin.OVER_1000 -> R.string.households_1000
}

/** 요약용 짧은 문구. 예: "500세대+" */
fun HouseholdMin.shortLabelRes(): Int = when (this) {
    HouseholdMin.OVER_300 -> R.string.households_300_short
    HouseholdMin.OVER_500 -> R.string.households_500_short
    HouseholdMin.OVER_1000 -> R.string.households_1000_short
}

fun BuildingAge.labelRes(): Int = when (this) {
    BuildingAge.WITHIN_5 -> R.string.building_age_5
    BuildingAge.WITHIN_10 -> R.string.building_age_10
    BuildingAge.WITHIN_15 -> R.string.building_age_15
}

/** 요약용. 예: "준공 10년 이내" */
fun BuildingAge.shortLabelRes(): Int = when (this) {
    BuildingAge.WITHIN_5 -> R.string.building_age_5_short
    BuildingAge.WITHIN_10 -> R.string.building_age_10_short
    BuildingAge.WITHIN_15 -> R.string.building_age_15_short
}
