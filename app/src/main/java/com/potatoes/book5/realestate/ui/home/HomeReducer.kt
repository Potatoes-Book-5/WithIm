package com.potatoes.book5.realestate.ui.home

internal fun reduceHome(state: HomeState, result: HomeResult): HomeState = when (result) {
    is HomeResult.RankingLoaded -> state.copy(
        isLoading = result.loading,
        error = result.error,
        neighborhoodName = result.neighborhoodName.ifBlank { state.neighborhoodName },
        radiusKm = result.radiusKm,
        dataAsOf = if (result.loading || result.error != null) state.dataAsOf else result.dataAsOf,
        periodStart = result.periodStart ?: state.periodStart,
        periodEnd = result.periodEnd ?: state.periodEnd,
        referenceLabel = result.referenceLabel ?: state.referenceLabel,
        ranking = if (result.loading || result.error != null) state.ranking else result.ranking,
        todayMatches = if (result.loading || result.error != null) state.todayMatches else result.todayMatches,
    )
    HomeResult.RefreshRequested -> state.copy(isLoading = true, error = null)
    is HomeResult.ScrapLoaded -> state.copy(scrap = result.scrap)
    is HomeResult.InterestLoaded -> state.copy(interest = result.interest)
}
