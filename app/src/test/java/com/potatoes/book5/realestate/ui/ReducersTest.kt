package com.potatoes.book5.realestate.ui

import com.potatoes.book5.realestate.domain.model.AreaRange
import com.potatoes.book5.realestate.domain.model.OnboardingStep
import com.potatoes.book5.realestate.domain.model.PriceRange
import com.potatoes.book5.realestate.ui.consent.ConsentResult
import com.potatoes.book5.realestate.ui.consent.ConsentState
import com.potatoes.book5.realestate.ui.consent.reduceConsent
import com.potatoes.book5.realestate.ui.interest.InterestResult
import com.potatoes.book5.realestate.ui.interest.InterestSelection
import com.potatoes.book5.realestate.ui.interest.InterestState
import com.potatoes.book5.realestate.ui.interest.NeighborhoodUi
import com.potatoes.book5.realestate.ui.interest.reduceInterest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReducersTest {

    private val jeongja = NeighborhoodUi("code-정자동", "정자동")
    private val sunae = NeighborhoodUi("code-수내동", "수내동")

    @Test
    fun `동의 화면 토글은 알림 설정만 바꾼다`() {
        val state = reduceConsent(ConsentState(), ConsentResult.InterestAlertChanged(false))
        assertFalse(state.interestAlertEnabled)
        assertTrue(state.leaderAlertEnabled)
    }

    @Test
    fun `아무것도 고르지 않으면 저장할 수 없다`() {
        val state = reduceInterest(InterestState(), InterestResult.Loaded(listOf(jeongja), InterestSelection()))
        assertFalse(state.canSave)
    }

    @Test
    fun `칩을 한 번 누르면 선택, 다시 누르면 해제한다`() {
        val loaded = reduceInterest(InterestState(), InterestResult.Loaded(listOf(jeongja), InterestSelection()))
        val selected = reduceInterest(loaded, InterestResult.PriceToggled(PriceRange.FROM_10_TO_15))
        assertTrue(selected.canSave)
        val cleared = reduceInterest(selected, InterestResult.PriceToggled(PriceRange.FROM_10_TO_15))
        assertTrue(cleared.selectedPrices.isEmpty())
        assertFalse(cleared.canSave)
    }

    @Test
    fun `저장 중에는 다시 저장할 수 없다`() {
        val state = InterestState(isLoading = false, selectedAreas = setOf(AreaRange.PY_30S))
        assertFalse(reduceInterest(state, InterestResult.Saving).canSave)
        assertTrue(reduceInterest(state, InterestResult.SaveFailed).canSave)
    }

    @Test
    fun `이전에 고른 동네가 추천에 없어도 칩으로 보여 준다`() {
        val state = reduceInterest(
            InterestState(),
            InterestResult.Loaded(listOf(jeongja), InterestSelection(neighborhoods = setOf(sunae))),
        )
        assertEquals(listOf(jeongja, sunae), state.neighborhoodChips)
    }

    @Test
    fun `추천 동네를 못 불러와도 저장된 선택은 유지한다`() {
        val state = reduceInterest(InterestState(), InterestResult.LoadFailed(InterestSelection(neighborhoods = setOf(sunae))))
        assertTrue(state.suggestionsFailed)
        assertEquals(setOf(sunae), state.selectedNeighborhoods)
    }

    @Test
    fun `앱 시작 단계에 맞는 화면으로 간다`() {
        assertEquals(Destination.CONSENT, reduceApp(AppState(), AppResult.StartAt(OnboardingStep.CONSENT)).destination)
        assertEquals(Destination.INTEREST, reduceApp(AppState(), AppResult.StartAt(OnboardingStep.INTEREST)).destination)
        assertEquals(Destination.HOME, reduceApp(AppState(), AppResult.StartAt(OnboardingStep.DONE)).destination)
    }
}
