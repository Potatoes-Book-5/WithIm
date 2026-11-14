package com.potatoes.book5.realestate.di

import android.content.Context
import com.potatoes.book5.realestate.data.local.PreferenceConsentRepository
import com.potatoes.book5.realestate.data.local.PreferenceInterestRepository
import com.potatoes.book5.realestate.data.local.SampleNeighborhoodRepository
import com.potatoes.book5.realestate.domain.port.ConsentRepository
import com.potatoes.book5.realestate.domain.port.InterestRepository
import com.potatoes.book5.realestate.domain.port.NeighborhoodRepository
import com.potatoes.book5.realestate.domain.usecase.GetInterestConditionUseCase
import com.potatoes.book5.realestate.domain.usecase.GetNeighborhoodSuggestionsUseCase
import com.potatoes.book5.realestate.domain.usecase.GetOnboardingStepUseCase
import com.potatoes.book5.realestate.domain.usecase.SaveConsentUseCase
import com.potatoes.book5.realestate.domain.usecase.SaveInterestConditionUseCase
import com.potatoes.book5.realestate.domain.usecase.SkipInterestConditionUseCase
import kotlinx.coroutines.Dispatchers

/** 구현체를 수동으로 조립한다(docs/ARCHITECTURE.md 의존성 주입). */
class AppContainer(context: Context) {
    private val prefs = context.getSharedPreferences("im_with_you", Context.MODE_PRIVATE)

    private val consentRepository: ConsentRepository = PreferenceConsentRepository(prefs, Dispatchers.IO)
    private val interestRepository: InterestRepository = PreferenceInterestRepository(prefs, Dispatchers.IO)
    private val neighborhoodRepository: NeighborhoodRepository = SampleNeighborhoodRepository()

    val getOnboardingStep = GetOnboardingStepUseCase(consentRepository, interestRepository)
    val saveConsent = SaveConsentUseCase(consentRepository)
    val getNeighborhoodSuggestions = GetNeighborhoodSuggestionsUseCase(neighborhoodRepository)
    val getInterestCondition = GetInterestConditionUseCase(interestRepository)
    val saveInterestCondition = SaveInterestConditionUseCase(interestRepository)
    val skipInterestCondition = SkipInterestConditionUseCase(interestRepository, consentRepository)
}
