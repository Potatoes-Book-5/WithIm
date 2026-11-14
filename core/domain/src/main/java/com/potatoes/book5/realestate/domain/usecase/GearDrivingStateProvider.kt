package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.model.DrivingState
import com.potatoes.book5.realestate.domain.model.GearPosition
import com.potatoes.book5.realestate.domain.model.GearReading
import com.potatoes.book5.realestate.domain.port.DrivingStateProvider
import com.potatoes.book5.realestate.domain.port.GearStateSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

/**
 * 기어로 주행 상태를 정한다(T-034, F-009 규칙 1, S-005, S-009).
 *
 * - P는 주차(주행 완료), R·N·D·수동 단수는 주행 중이다. UNKNOWN은 상태를 바꾸지 않는다.
 * - 기어를 한 번도 읽지 못했으면(미지원, 서비스 없음, 권한 없음) [fallback]을 따른다. 출시 빌드에서는 주차로 남고,
 *   개발 빌드에서는 수동 전환 버튼으로 바꾼다.
 * - 기어를 읽은 뒤 오류가 오면 마지막 상태를 유지한다. 일시 오류로 주행 세션이 끝나지 않게 한다.
 * - 개발 빌드의 수동 전환은 기어를 읽은 뒤에도 다음 기어 변경 전까지 우선한다.
 *
 * SDK 연결은 하나만 쓰도록 [scope]에서 공유한다. 주행 기록과 화면 전환이 같은 값을 받는다.
 */
class GearDrivingStateProvider(
    gearSource: GearStateSource,
    fallback: DrivingStateProvider,
    scope: CoroutineScope,
) : DrivingStateProvider {
    override val drivingState: Flow<DrivingState> =
        resolveDrivingState(gearSource.gear, fallback.drivingState)
            .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DrivingState.PARKED)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** P면 주차, 알 수 없으면 null, 그 밖의 기어는 주행 중이다. */
fun GearPosition.toDrivingState(): DrivingState? = when (this) {
    GearPosition.PARK -> DrivingState.PARKED
    GearPosition.REVERSE, GearPosition.NEUTRAL, GearPosition.DRIVE, GearPosition.MANUAL -> DrivingState.DRIVING
    GearPosition.UNKNOWN -> null
}

/** [GearDrivingStateProvider]의 판정 규칙. 처음 값은 주차다. */
fun resolveDrivingState(gear: Flow<GearReading>, fallback: Flow<DrivingState>): Flow<DrivingState> =
    merge(
        gear.catch { emit(GearReading.Unavailable) }.map(Signal::Gear),
        fallback.distinctUntilChanged().map(Signal::Fallback),
    )
        .scan(Resolution(), Resolution::next)
        .map { it.state }
        .distinctUntilChanged()

private sealed interface Signal {
    data class Gear(val reading: GearReading) : Signal
    data class Fallback(val state: DrivingState) : Signal
}

private data class Resolution(
    val state: DrivingState = DrivingState.PARKED,
    /** 유효한 기어(P, R, N, D, 수동)를 한 번이라도 받았는지 */
    val gearKnown: Boolean = false,
    val lastFallback: DrivingState? = null,
) {
    fun next(signal: Signal): Resolution = when (signal) {
        is Signal.Gear -> when (val reading = signal.reading) {
            is GearReading.Selected -> reading.gear.toDrivingState()
                ?.let { copy(state = it, gearKnown = true) }
                ?: this
            // 처음부터 읽지 못하면 대체 상태가 이미 반영되어 있고, 읽은 뒤라면 마지막 상태를 유지한다.
            GearReading.Unavailable -> this
        }
        is Signal.Fallback -> {
            // 대체 흐름의 첫 값(초기값)은 기어를 읽기 전에만 쓴다. 이후 바뀐 값은 개발용 수동 전환이므로 적용한다.
            val apply = !gearKnown || lastFallback != null
            copy(state = if (apply) signal.state else state, lastFallback = signal.state)
        }
    }
}
