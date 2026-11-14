package com.potatoes.book5.realestate.ui.home

internal fun reduceHome(state: HomeState, result: HomeResult): HomeState = when (result) {
    is HomeResult.RankingLoaded -> state.copy(
        isLoading = false,
        neighborhoodName = result.neighborhoodName,
        radiusKm = result.radiusKm,
        dataAsOf = result.dataAsOf,
        ranking = result.ranking,
        todayMatches = result.todayMatches,
    )
    is HomeResult.ScrapLoaded -> state.copy(scrap = result.scrap)
    is HomeResult.InterestLoaded -> state.copy(interest = result.interest)
}
