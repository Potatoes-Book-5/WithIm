package com.potatoes.book5.realestate.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.potatoes.book5.realestate.RealEstateApplication
import com.potatoes.book5.realestate.di.AppContainer
import com.potatoes.book5.realestate.ui.consent.ConsentViewModel
import com.potatoes.book5.realestate.ui.driving.DrivingViewModel
import com.potatoes.book5.realestate.ui.home.HomeViewModel
import com.potatoes.book5.realestate.ui.interest.InterestViewModel

/** AppContainer에서 ViewModel을 만든다. */
object AppViewModels {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val c = container()
            AppViewModel(c.getOnboardingStep, c.observeDrivingState, c.simulateDriving, c.simulateAreaChange)
        }
        initializer { ConsentViewModel(container().saveConsent) }
        initializer {
            val c = container()
            InterestViewModel(
                savedStateHandle = createSavedStateHandle(),
                getSuggestions = c.getNeighborhoodSuggestions,
                getInterestCondition = c.getInterestCondition,
                saveInterestCondition = c.saveInterestCondition,
                skipInterestCondition = c.skipInterestCondition,
            )
        }
        initializer {
            val c = container()
            HomeViewModel(c.observeNearbyRanking, c.getScrapSummary, c.getInterestCondition)
        }
        initializer {
            val c = container()
            DrivingViewModel(createSavedStateHandle(), c.observeNearbyRanking, c.observeLeaderChanges)
        }
    }

    private fun CreationExtras.container(): AppContainer =
        (checkNotNull(this[APPLICATION_KEY]) as RealEstateApplication).container
}
