package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.model.DrivingState
import com.potatoes.book5.realestate.domain.model.DrivingState.DRIVING
import com.potatoes.book5.realestate.domain.model.DrivingState.PARKED
import com.potatoes.book5.realestate.domain.model.GearPosition
import com.potatoes.book5.realestate.domain.model.GearReading
import com.potatoes.book5.realestate.domain.port.DrivingStateProvider
import com.potatoes.book5.realestate.domain.port.GearStateSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GearDrivingStateProviderTest {

    private val gear = MutableSharedFlow<GearReading>()
    private val fallback = MutableStateFlow(PARKED)

    private fun TestScope.collect(flow: Flow<DrivingState>): List<DrivingState> {
        val values = mutableListOf<DrivingState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect { values += it } }
        return values
    }

    private suspend fun select(position: GearPosition) = gear.emit(GearReading.Selected(position))

    @Test
    fun `P는 주차, 그 밖의 기어는 주행 중이다`() {
        assertEquals(PARKED, GearPosition.PARK.toDrivingState())
        listOf(GearPosition.DRIVE, GearPosition.REVERSE, GearPosition.NEUTRAL, GearPosition.MANUAL).forEach {
            assertEquals(DRIVING, it.toDrivingState())
        }
        assertEquals(null, GearPosition.UNKNOWN.toDrivingState())
    }

    @Test
    fun `기어가 D로 바뀌면 주행, P로 돌아오면 주행 완료다`() = runTest {
        val values = collect(resolveDrivingState(gear, fallback))
        select(GearPosition.PARK)
        select(GearPosition.DRIVE)
        select(GearPosition.NEUTRAL)
        select(GearPosition.REVERSE)
        select(GearPosition.PARK)
        assertEquals(listOf(PARKED, DRIVING, PARKED), values)
    }

    @Test
    fun `UNKNOWN은 마지막 상태를 유지한다`() = runTest {
        val values = collect(resolveDrivingState(gear, fallback))
        select(GearPosition.DRIVE)
        select(GearPosition.UNKNOWN)
        assertEquals(listOf(PARKED, DRIVING), values)
    }

    @Test
    fun `기어를 읽은 뒤 오류가 와도 주행 상태를 유지한다`() = runTest {
        val values = collect(resolveDrivingState(gear, fallback))
        select(GearPosition.DRIVE)
        gear.emit(GearReading.Unavailable)
        assertEquals(listOf(PARKED, DRIVING), values)
    }

    @Test
    fun `기어를 읽지 못하면 대체 상태를 따른다`() = runTest {
        val values = collect(resolveDrivingState(gear, fallback))
        gear.emit(GearReading.Unavailable)
        fallback.value = DRIVING
        fallback.value = PARKED
        assertEquals(listOf(PARKED, DRIVING, PARKED), values)
    }

    @Test
    fun `기어 흐름이 예외로 끝나도 대체 상태를 따른다`() = runTest {
        val broken = flow<GearReading> { throw IllegalStateException("service") }
        val values = collect(resolveDrivingState(broken, fallback))
        fallback.value = DRIVING
        assertEquals(listOf(PARKED, DRIVING), values)
    }

    @Test
    fun `대체 흐름의 초기값은 먼저 받은 기어를 덮지 않는다`() = runTest {
        // 기어가 대체 흐름보다 먼저 도착하는 경우
        val lateFallback = MutableStateFlow(PARKED)
        val gearFirst = MutableSharedFlow<GearReading>()
        val values = mutableListOf<DrivingState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            resolveDrivingState(
                gearFirst.onSubscription { emit(GearReading.Selected(GearPosition.DRIVE)) },
                lateFallback,
            ).collect { values += it }
        }
        assertEquals(DRIVING, values.last())
    }

    @Test
    fun `개발용 수동 전환은 기어를 읽은 뒤에도 다음 기어 변경 전까지 우선한다`() = runTest {
        val values = collect(resolveDrivingState(gear, fallback))
        select(GearPosition.PARK)
        fallback.value = DRIVING
        assertEquals(DRIVING, values.last())
        select(GearPosition.PARK)
        assertEquals(PARKED, values.last())
    }

    @Test
    fun `공유 제공자는 수집자가 여럿이어도 기어 연결을 하나만 연다`() = runTest {
        var subscriptions = 0
        val source = object : GearStateSource {
            override val gear: Flow<GearReading> = this@GearDrivingStateProviderTest.gear.onSubscription { subscriptions++ }
        }
        val manual = object : DrivingStateProvider {
            override val drivingState: Flow<DrivingState> = fallback
        }
        val provider = GearDrivingStateProvider(source, manual, backgroundScope)
        val first = collect(provider.drivingState)
        val second = collect(provider.drivingState)
        runCurrent()
        select(GearPosition.DRIVE)
        runCurrent()
        assertEquals(1, subscriptions)
        assertEquals(DRIVING, first.last())
        assertEquals(DRIVING, second.last())
    }
}
