package com.potatoes.book5.realestate.fakes

import com.potatoes.book5.realestate.domain.error.DomainError
import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.Apartment
import com.potatoes.book5.realestate.domain.model.ConsentSettings
import com.potatoes.book5.realestate.domain.model.InterestCondition
import com.potatoes.book5.realestate.domain.model.NearbyApartment
import com.potatoes.book5.realestate.domain.model.Neighborhood
import com.potatoes.book5.realestate.domain.model.RankingSnapshot
import com.potatoes.book5.realestate.domain.model.ApartmentDetail
import com.potatoes.book5.realestate.domain.model.Deal
import com.potatoes.book5.realestate.domain.model.PriceRecord
import com.potatoes.book5.realestate.domain.model.Scrap
import com.potatoes.book5.realestate.domain.port.ApartmentRepository
import com.potatoes.book5.realestate.domain.port.ConsentRepository
import com.potatoes.book5.realestate.domain.port.InterestRepository
import com.potatoes.book5.realestate.domain.port.NeighborhoodRepository
import com.potatoes.book5.realestate.domain.port.RankingRepository
import com.potatoes.book5.realestate.domain.port.ScrapRepository
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

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

/** 메모리에 스크랩을 보관한다. Room 구현과 같은 계약을 따른다. */
class FakeScrapRepository(initial: List<Scrap> = emptyList()) : ScrapRepository {
    val scraps = MutableStateFlow(initial.associateBy { it.apartmentId })
    override fun observeAll(): Flow<List<Scrap>> = scraps.map { it.values.toList() }
    override fun observeIsScrapped(apartmentId: String): Flow<Boolean> = scraps.map { apartmentId in it }
    override suspend fun getAll(): List<Scrap> = scraps.value.values.toList()
    override suspend fun get(apartmentId: String): Scrap? = scraps.value[apartmentId]
    override suspend fun upsert(scrap: Scrap) = scraps.update { it + (scrap.apartmentId to scrap) }
    override suspend fun delete(apartmentId: String) = scraps.update { it - apartmentId }
    override suspend fun markChangeSeen(apartmentId: String) = scraps.update { map ->
        val s = map[apartmentId]
        val change = s?.change ?: return@update map
        map + (apartmentId to s.copy(change = change.copy(seen = true)))
    }
}

class FakeApartmentRepository(
    var deals: MutableMap<String, Deal> = mutableMapOf(),
    var failing: Set<String> = emptySet(),
) : ApartmentRepository {
    override suspend fun detail(apartmentId: String): Outcome<ApartmentDetail> {
        if (apartmentId in failing) return Outcome.Failure(DomainError.Offline)
        val deal = deals[apartmentId] ?: return Outcome.Failure(DomainError.Invalid("unknown"))
        return Outcome.Success(detailOf(apartmentId, deal))
    }
    override suspend fun latestDeal(apartmentId: String): Outcome<Deal> {
        if (apartmentId in failing) return Outcome.Failure(DomainError.Offline)
        return deals[apartmentId]?.let { Outcome.Success(it) } ?: Outcome.Failure(DomainError.Invalid("unknown"))
    }
}

fun dealOf(price: Long, date: LocalDate = LocalDate.of(2026, 10, 1), pyeong: Int = 34) = Deal(date, pyeong, 10, price)

fun detailOf(id: String, latest: Deal, previous: Deal? = null) = ApartmentDetail(
    id = id, name = "단지-$id", district = "분당구", neighborhood = "정자동", households = 1_000, builtYear = 2010,
    minPyeong = 24, maxPyeong = 45, distanceKm = 1.0, latestDeal = latest, previousDeal = previous,
    trend = emptyList(), recentDeals = listOfNotNull(latest, previous),
)

fun scrapOf(
    id: String,
    price: Long = 150_000,
    date: LocalDate = LocalDate.of(2026, 10, 1),
    scrappedAt: LocalDateTime = LocalDateTime.of(2026, 10, 2, 9, 0),
    change: com.potatoes.book5.realestate.domain.model.PriceChange? = null,
): Scrap {
    val record = PriceRecord(price, date)
    return Scrap(id, "단지-$id", "정자동", 34, scrappedAt, record, record, scrappedAt, change)
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
