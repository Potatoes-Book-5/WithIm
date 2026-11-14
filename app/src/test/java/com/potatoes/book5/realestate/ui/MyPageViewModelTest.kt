package com.potatoes.book5.realestate.ui

import com.potatoes.book5.realestate.domain.model.InterestCondition
import com.potatoes.book5.realestate.domain.model.PriceRange
import com.potatoes.book5.realestate.domain.usecase.ObserveMyPageUseCase
import com.potatoes.book5.realestate.fakes.FakeInterestRepository
import com.potatoes.book5.realestate.fakes.FakeScrapRepository
import com.potatoes.book5.realestate.fakes.scrapOf
import com.potatoes.book5.realestate.ui.mypage.MyPageEffect
import com.potatoes.book5.realestate.ui.mypage.MyPageIntent
import com.potatoes.book5.realestate.ui.mypage.MyPageViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyPageViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `카드는 축 순서대로 점수를 담고 이동 의도는 화면으로 바뀐다`() = runTest(dispatcher) {
        val interest = FakeInterestRepository(InterestCondition(priceRanges = setOf(PriceRange.FROM_10_TO_15)))
        val scraps = FakeScrapRepository(listOf(scrapOf("a", price = 165_000)))
        val vm = MyPageViewModel(ObserveMyPageUseCase(interest, scraps) { 2026 })
        runCurrent()

        val state = vm.state.value
        assertFalse(state.isLoading)
        val card = state.cards.single()
        assertEquals(listOf(4f, 5f, 5f, 5f, 5f), card.scores)
        assertEquals(96, card.ratePercent)

        vm.onIntent(MyPageIntent.EditConditions)
        assertEquals(MyPageEffect.Navigate(Screen.Interest), vm.effect.first())
        vm.onIntent(MyPageIntent.OpenApartment("a"))
        assertEquals(MyPageEffect.Navigate(Screen.Detail("a")), vm.effect.first())
        vm.onIntent(MyPageIntent.OpenScraps)
        assertEquals(MyPageEffect.Navigate(Screen.Scraps()), vm.effect.first())
        vm.onIntent(MyPageIntent.Back)
        assertEquals(MyPageEffect.Close, vm.effect.first())
    }
}
