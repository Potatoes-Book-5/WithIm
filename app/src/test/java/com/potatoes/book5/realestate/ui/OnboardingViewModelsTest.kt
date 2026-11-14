package com.potatoes.book5.realestate.ui

import androidx.lifecycle.SavedStateHandle
import com.potatoes.book5.realestate.domain.model.AreaRange
import com.potatoes.book5.realestate.domain.model.ConsentSettings
import com.potatoes.book5.realestate.domain.model.InterestCondition
import com.potatoes.book5.realestate.domain.model.Neighborhood
import com.potatoes.book5.realestate.domain.model.PriceRange
import com.potatoes.book5.realestate.domain.usecase.GetInterestConditionUseCase
import com.potatoes.book5.realestate.domain.usecase.GetNeighborhoodSuggestionsUseCase
import com.potatoes.book5.realestate.domain.usecase.SaveConsentUseCase
import com.potatoes.book5.realestate.domain.usecase.SaveInterestConditionUseCase
import com.potatoes.book5.realestate.domain.usecase.SkipInterestConditionUseCase
import com.potatoes.book5.realestate.fakes.FakeConsentRepository
import com.potatoes.book5.realestate.fakes.FakeInterestRepository
import com.potatoes.book5.realestate.fakes.FakeNeighborhoodRepository
import com.potatoes.book5.realestate.ui.consent.ConsentEffect
import com.potatoes.book5.realestate.ui.consent.ConsentIntent
import com.potatoes.book5.realestate.ui.consent.ConsentViewModel
import com.potatoes.book5.realestate.ui.interest.InterestEffect
import com.potatoes.book5.realestate.ui.interest.InterestIntent
import com.potatoes.book5.realestate.ui.interest.InterestMessage
import com.potatoes.book5.realestate.ui.interest.InterestViewModel
import com.potatoes.book5.realestate.ui.interest.NeighborhoodUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelsTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val consent = FakeConsentRepository(
        ConsentSettings(locationConsented = true, interestAlertEnabled = true, leaderAlertEnabled = true)
    )

    private fun interestViewModel(
        interest: FakeInterestRepository = FakeInterestRepository(),
        handle: SavedStateHandle = SavedStateHandle(),
        failSuggestions: Boolean = false,
    ) = InterestViewModel(
        savedStateHandle = handle,
        getSuggestions = GetNeighborhoodSuggestionsUseCase(FakeNeighborhoodRepository(fail = failSuggestions)),
        getInterestCondition = GetInterestConditionUseCase(interest),
        saveInterestCondition = SaveInterestConditionUseCase(interest),
        skipInterestCondition = SkipInterestConditionUseCase(interest, consent),
    )

    @Test
    fun `동의하면 알림 설정과 함께 저장하고 관심 조건 화면으로 간다`() = runTest {
        val repo = FakeConsentRepository()
        val vm = ConsentViewModel(SaveConsentUseCase(repo))
        vm.onIntent(ConsentIntent.SetLeaderAlert(false))
        vm.onIntent(ConsentIntent.Agree)
        assertEquals(ConsentEffect.OpenInterestConditions, vm.effect.first())
        assertTrue(repo.stored!!.locationConsented)
        assertFalse(repo.stored!!.leaderAlertEnabled)
    }

    @Test
    fun `나중에를 누르면 위치 동의 없이 홈으로 간다`() = runTest {
        val repo = FakeConsentRepository()
        val vm = ConsentViewModel(SaveConsentUseCase(repo))
        vm.onIntent(ConsentIntent.Decline)
        assertEquals(ConsentEffect.OpenHome, vm.effect.first())
        assertFalse(repo.stored!!.locationConsented)
    }

    @Test
    fun `추천 동네를 불러오고 고른 조건을 저장한 뒤 홈으로 간다`() = runTest {
        val interest = FakeInterestRepository()
        val vm = interestViewModel(interest)
        val jeongja = vm.state.value.suggestions.first()
        vm.onIntent(InterestIntent.ToggleNeighborhood(jeongja))
        vm.onIntent(InterestIntent.TogglePrice(PriceRange.FROM_10_TO_15))
        vm.onIntent(InterestIntent.Save)
        assertEquals(InterestEffect.OpenHome, vm.effect.first())
        assertEquals(setOf(Neighborhood("code-정자동", "정자동")), interest.stored!!.neighborhoods)
        assertEquals(setOf(PriceRange.FROM_10_TO_15), interest.stored!!.priceRanges)
    }

    @Test
    fun `아무것도 고르지 않으면 저장하지 않는다`() = runTest {
        val interest = FakeInterestRepository()
        val vm = interestViewModel(interest)
        vm.onIntent(InterestIntent.Save)
        assertEquals(null, interest.stored)
        assertFalse(interest.decided)
    }

    @Test
    fun `건너뛰면 관심 매물 알림을 끄고 홈으로 간다`() = runTest {
        val vm = interestViewModel()
        vm.onIntent(InterestIntent.Skip)
        assertEquals(InterestEffect.OpenHome, vm.effect.first())
        assertFalse(consent.stored!!.interestAlertEnabled)
    }

    @Test
    fun `음성 검색은 준비 중 안내를 보여 준다`() = runTest {
        val vm = interestViewModel()
        vm.onIntent(InterestIntent.VoiceSearch)
        assertEquals(InterestEffect.ShowMessage(InterestMessage.VOICE_UNAVAILABLE), vm.effect.first())
    }

    @Test
    fun `저장된 조건을 미리 선택해 보여 준다`() = runTest {
        val stored = InterestCondition(areaRanges = setOf(AreaRange.PY_30S))
        val vm = interestViewModel(FakeInterestRepository(stored))
        assertEquals(setOf(AreaRange.PY_30S), vm.state.value.selectedAreas)
        assertTrue(vm.state.value.canSave)
    }

    @Test
    fun `프로세스가 다시 만들어져도 고르던 값을 복원한다`() = runTest {
        val handle = SavedStateHandle()
        val first = interestViewModel(handle = handle)
        first.onIntent(InterestIntent.ToggleNeighborhood(NeighborhoodUi("code-수내동", "수내동")))
        first.onIntent(InterestIntent.ToggleArea(AreaRange.OVER_40))

        val restored = interestViewModel(handle = handle)
        assertEquals(setOf(NeighborhoodUi("code-수내동", "수내동")), restored.state.value.selectedNeighborhoods)
        assertEquals(setOf(AreaRange.OVER_40), restored.state.value.selectedAreas)
    }

    @Test
    fun `추천 동네를 못 불러오면 실패 상태를 보여 준다`() = runTest {
        val vm = interestViewModel(failSuggestions = true)
        assertTrue(vm.state.value.suggestionsFailed)
        assertFalse(vm.state.value.isLoading)
    }
}
