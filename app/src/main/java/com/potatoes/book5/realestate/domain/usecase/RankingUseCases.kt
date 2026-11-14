package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.model.Apartment
import com.potatoes.book5.realestate.domain.model.DrivingState
import com.potatoes.book5.realestate.domain.model.LeaderChange
import com.potatoes.book5.realestate.domain.model.RankingSnapshot
import com.potatoes.book5.realestate.domain.port.ConsentRepository
import com.potatoes.book5.realestate.domain.port.DrivingStateProvider
import com.potatoes.book5.realestate.domain.port.RankingRepository
import com.potatoes.book5.realestate.domain.port.VehicleSimulation
import java.time.Duration
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

class ObserveNearbyRankingUseCase(private val rankingRepository: RankingRepository) {
    operator fun invoke(): Flow<RankingSnapshot> = rankingRepository.observeNearbyRanking()
}

class ObserveDrivingStateUseCase(private val provider: DrivingStateProvider) {
    operator fun invoke(): Flow<DrivingState> = provider.drivingState.distinctUntilChanged()
}

/**
 * 순위가 갱신될 때 1위 단지가 바뀌면 대장 교체를 알린다(F-003 규칙 4·5).
 * - 대장 교체 알림이 꺼져 있으면 알리지 않는다(F-006 규칙 7).
 * - 마지막 알림 후 [minInterval] 안에 다시 바뀌면 알리지 않고 기록만 한다(제안값 5분).
 */
class ObserveLeaderChangesUseCase(
    private val rankingRepository: RankingRepository,
    private val consentRepository: ConsentRepository,
    private val minInterval: Duration = Duration.ofMinutes(5),
    private val now: () -> LocalDateTime = LocalDateTime::now,
) {
    operator fun invoke(): Flow<LeaderChange> = flow {
        var previousLeader: Apartment? = null
        var lastAlertAt: LocalDateTime? = null
        rankingRepository.observeNearbyRanking().collect { snapshot ->
            val leader = snapshot.leader ?: return@collect
            val before = previousLeader
            previousLeader = leader.apartment
            if (before == null || before.id == leader.apartment.id) return@collect
            val at = now()
            val tooSoon = lastAlertAt?.let { Duration.between(it, at) < minInterval } ?: false
            val enabled = consentRepository.get()?.leaderAlertEnabled ?: false
            if (enabled && !tooSoon) {
                lastAlertAt = at
                emit(LeaderChange(previous = before, current = leader, at = at))
            }
        }
    }
}

class SimulateDrivingUseCase(private val simulation: VehicleSimulation) {
    operator fun invoke(driving: Boolean) = simulation.setDriving(driving)
}

class SimulateAreaChangeUseCase(private val simulation: VehicleSimulation) {
    operator fun invoke() = simulation.moveToNextArea()
}
