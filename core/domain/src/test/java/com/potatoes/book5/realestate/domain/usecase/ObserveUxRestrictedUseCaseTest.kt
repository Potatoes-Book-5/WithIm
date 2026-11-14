package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.port.UxRestrictionSource
import com.potatoes.book5.realestate.domain.port.UxRestrictionState
import com.potatoes.book5.realestate.domain.port.UxRestrictionState.RESTRICTED
import com.potatoes.book5.realestate.domain.port.UxRestrictionState.UNAVAILABLE
import com.potatoes.book5.realestate.domain.port.UxRestrictionState.UNRESTRICTED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveUxRestrictedUseCaseTest {

    private fun sourceOf(flow: Flow<UxRestrictionState>) = object : UxRestrictionSource {
        override val state = flow
    }

    @Test
    fun `DO 필요 상태만 규제로 본다`() = runTest {
        val values = ObserveUxRestrictedUseCase(sourceOf(flowOf(UNRESTRICTED, RESTRICTED, RESTRICTED, UNAVAILABLE, UNRESTRICTED)))().toList()
        assertEquals(listOf(false, true, false), values)
    }

    @Test
    fun `차량 서비스 오류는 규제 없음으로 본다`() = runTest {
        val values = ObserveUxRestrictedUseCase(sourceOf(flow { throw IllegalStateException("car service") }))().toList()
        assertEquals(listOf(false), values)
    }
}
