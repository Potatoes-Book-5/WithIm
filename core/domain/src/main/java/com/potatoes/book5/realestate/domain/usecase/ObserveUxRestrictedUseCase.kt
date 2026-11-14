package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.port.UxRestrictionSource
import com.potatoes.book5.realestate.domain.port.UxRestrictionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * 플랫폼이 DO 화면만 허용하는지(T-035, DI-002). 규제 상태를 모르면 false로 두고 기어 기준 주행 상태(T-034)를 따른다.
 */
class ObserveUxRestrictedUseCase(private val source: UxRestrictionSource) {
    operator fun invoke(): Flow<Boolean> = source.state
        .catch { emit(UxRestrictionState.UNAVAILABLE) }
        .map { it == UxRestrictionState.RESTRICTED }
        .distinctUntilChanged()
}
