package com.potatoes.book5.realestate.fakes

import com.potatoes.book5.realestate.domain.error.DomainError
import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.Apartment
import com.potatoes.book5.realestate.domain.model.ConsentSettings
import com.potatoes.book5.realestate.domain.model.InterestCondition
import com.potatoes.book5.realestate.domain.model.NearbyApartment
import com.potatoes.book5.realestate.domain.model.Neighborhood
import com.potatoes.book5.realestate.domain.model.RankingSnapshot
import com.potatoes.book5.realestate.domain.model.ScrapSummary
import com.potatoes.book5.realestate.domain.port.ConsentRepository
import com.potatoes.book5.realestate.domain.port.InterestRepository
import com.potatoes.book5.realestate.domain.port.NeighborhoodRepository
import com.potatoes.book5.realestate.domain.port.RankingRepository
import com.potatoes.book5.realestate.domain.port.ScrapRepository
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeConsentRepository(var stored: ConsentSettings? = null) : ConsentRepository {
    override suspend fun get(): ConsentSettings? = stored
    override suspend fun save(settings: ConsentSettings) {
        stored = settings
    }
}

class FakeInterestRepository(
    var stored: InterestCondition? = null,
    var decided: Boolean = stored != null,
) : InterestRepository {
    override suspend fun get(): InterestCondition? = stored
    override suspend fun save(condition: InterestCondition) {
        stored = condition
        decided = true
    }
    override suspend fun markSkipped() {
        decided = true
    }
    override suspend fun isDecided(): Boolean = decided
}

class FakeRankingRepository(initial: RankingSnapshot) : RankingRepository {
    val snapshots = MutableStateFlow(initial)
    override fun observeNearbyRanking(): Flow<RankingSnapshot> = snapshots
}

class FakeScrapRepository(private val summary: ScrapSummary = ScrapSummary(0, 0, null)) : ScrapRepository {
    override suspend fun summary(): ScrapSummary = summary
}

/** 테스트용 순위 스냅샷. leaderIds 순서대로 1위부터 채운다. */
fun snapshotOf(vararg leaderIds: String, area: String = "정자동"): RankingSnapshot = RankingSnapshot(
    neighborhoodName = area,
    radiusKm = 10,
    top = leaderIds.mapIndexed { i, id ->
        NearbyApartment(Apartment(id, "단지-$id", 200_000L - i * 10_000, 34, LocalDate.of(2026, 10, 1)), 1.0 + i, 0.0)
    },
    interestMatches = emptyList(),
    others = emptyList(),
    dataAsOf = LocalDateTime.of(2026, 11, 13, 9, 0),
)

class FakeNeighborhoodRepository(
    private val names: List<String> = listOf("정자동", "수내동"),
    private val fail: Boolean = false,
) : NeighborhoodRepository {
    override suspend fun suggestions(): Outcome<List<Neighborhood>> =
        if (fail) Outcome.Failure(DomainError.Offline)
        else Outcome.Success(names.map { Neighborhood("code-$it", it) })
}
