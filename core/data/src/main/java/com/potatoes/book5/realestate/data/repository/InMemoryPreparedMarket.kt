package com.potatoes.book5.realestate.data.repository

import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.*
import com.potatoes.book5.realestate.domain.port.PreparedScrapReader
import com.potatoes.book5.realestate.domain.port.PreparedPriceReader
import com.potatoes.book5.realestate.domain.model.appaction.ResolvedArea
import com.potatoes.book5.realestate.domain.model.tour.*
import com.potatoes.book5.realestate.domain.port.TourMarketReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

/** Bounded process-local read model. Atomic immutable publication, independent of fetch locks. */
class InMemoryPreparedMarket(
    private val clock: Clock = Clock.systemUTC(),
    private val ttl: Duration = Duration.ofMinutes(5),
    private val maxCandidates: Int = 256,
    private val maxDistricts: Int = 8,
) : PreparedScrapReader, PreparedPriceReader, TourMarketReader {
    private data class State(
        val candidates: Map<String, PreparedScrapCandidate> = emptyMap(),
        val districts: Map<String, PreparedDistrictPrices> = emptyMap(),
        val revision: Long = 0,
        val tourMarkets: Map<String,TourMarket> = emptyMap(),
    )
    private val state = AtomicReference(State())
    private val publications = MutableStateFlow(0L)
    val changes = publications.asStateFlow()
    private val analytics = AtomicReference<Map<String,Pair<ApartmentDetail,Instant>>>(emptyMap())
    init { require(!ttl.isNegative && !ttl.isZero && maxCandidates > 0 && maxDistricts > 0) }

    fun publish(candidates: List<PreparedScrapCandidate>, district: PreparedDistrictPrices? = null, expectedRevision: Long? = null): Long {
        // Copy input before CAS; never retain caller-owned mutable collections.
        val today = clock.instant().atZone(java.time.ZoneId.of("Asia/Seoul")).toLocalDate()
        val batch = candidates.filter { it.apartmentId.isNotBlank() && it.name.isNotBlank() &&
            it.areaPyeong > 0 && it.latest.priceManwon in 1..(Long.MAX_VALUE / 10_000) &&
            it.latest.dealDate <= today }.toList()
        val now = clock.instant()
        fun accepts(incoming: Instant, old: Instant?) = old == null || old > now || incoming >= old
        val updated = state.updateAndGet { previous ->
            if (expectedRevision != null && expectedRevision != previous.revision) return@updateAndGet previous
            val rows = LinkedHashMap(previous.candidates)
            val regions = LinkedHashMap(previous.districts)
            val wholeAccepted = district != null && accepts(district.sourceAsOf, regions[district.districtCode]?.sourceAsOf)
            if (wholeAccepted) {
                val full = requireNotNull(district)
                // A complete district refresh is authoritative, including disappeared/cancelled targets.
                rows.entries.removeAll { it.value.districtCode == full.districtCode }
                regions.remove(full.districtCode)
                regions[full.districtCode] = full
            }
            if (district == null || wholeAccepted) batch.forEach { candidate ->
                val old = rows[candidate.apartmentId]
                val watermark = regions[candidate.districtCode]?.sourceAsOf
                val provenBatch = district != null || expectedRevision != null || old != null || watermark == null ||
                    watermark > now || candidate.sourceAsOf > watermark
                if (provenBatch && accepts(candidate.sourceAsOf, old?.sourceAsOf) && accepts(candidate.sourceAsOf, watermark)) {
                    rows.remove(candidate.apartmentId)
                    rows[candidate.apartmentId] = candidate
                }
            }
            while (rows.size > maxCandidates) rows.remove(rows.keys.first())
            while (regions.size > maxDistricts) regions.remove(regions.keys.first())
            State(rows.toMap(), regions.toMap(), previous.revision + if (wholeAccepted) 1 else 0,
                previous.tourMarkets.filterKeys { it in regions && (!wholeAccepted || it!=district?.districtCode) })
        }
        publications.value=updated.revision
        return updated.revision
    }

    /** Capture before a trusted partial preparation, not after it finishes. */
    fun currentRevision(): Long = state.get().revision

    private fun <T> read(value: T?, asOf: (T) -> Instant): Outcome<T> {
        value ?: return Outcome.Failure(DomainError.DataNotReady(PreparedDataFailure.NOT_READY))
        val age = Duration.between(asOf(value), clock.instant())
        return if (age.isNegative || age >= ttl)
            Outcome.Failure(DomainError.DataNotReady(PreparedDataFailure.STALE))
        else Outcome.Success(value)
    }
    override fun readScrapCandidate(apartmentId: String): Outcome<PreparedScrapCandidate> =
        read(state.get().candidates[apartmentId]) { it.sourceAsOf }

    override fun knownDistricts(): List<ResolvedArea> = state.get().districts.values.map { ResolvedArea(it.districtCode, it.districtName) }

    override fun readDistrictPrices(districtCode: String): Outcome<PreparedDistrictPrices> {
        val result = read(state.get().districts[districtCode]) { it.sourceAsOf }
        if (result is Outcome.Success && result.value.periodEndInclusive !=
            clock.instant().atZone(java.time.ZoneId.of("Asia/Seoul")).toLocalDate())
            return Outcome.Failure(DomainError.DataNotReady(PreparedDataFailure.STALE))
        return result
    }

    fun publishMarket(value: TourMarket, expectedRevision: Long) {
        val copy=value.copy(candidates=value.candidates.toList())
        state.updateAndGet { old ->
            if(old.revision!=expectedRevision || old.districts[value.districtCode]?.sourceAsOf!=value.sourceAsOf) old
            else old.copy(tourMarkets=old.tourMarkets+(value.districtCode to copy))
        }
    }
    override fun readMarket(districtCode: String): Outcome<TourMarket> {
        val snapshot=state.get()
        val today=clock.instant().atZone(java.time.ZoneId.of("Asia/Seoul")).toLocalDate()
        if(snapshot.districts[districtCode]?.periodEndInclusive?.let { it!=today }==true)
            return Outcome.Failure(DomainError.DataNotReady(PreparedDataFailure.STALE))
        return read(snapshot.tourMarkets[districtCode]) { it.sourceAsOf }
    }
    fun publishAnalytics(value: ApartmentDetail, at: Instant) {
        val age=Duration.between(at,clock.instant())
        if(age.isNegative || age>=ttl) return
        val copy=value.copy(trend=value.trend.toList(),recentDeals=value.recentDeals.toList())
        state.updateAndGet { old -> old.copy(tourMarkets=old.tourMarkets.mapValues { (_,market) -> market.copy(candidates=market.candidates.map { c ->
            if(c.id==value.id) c.copy(households=value.households ?: c.households,builtYear=value.builtYear ?: c.builtYear) else c
        }) }) }
        analytics.updateAndGet { old ->
            val rows=LinkedHashMap(old)
            if(old[value.id]?.second?.let { it>at }==true) return@updateAndGet old
            rows.remove(value.id);rows[value.id]=copy to at
            while(rows.size>32) rows.remove(rows.keys.first())
            rows.toMap()
        }
    }
    override fun readAnalytics(apartmentId: String): Outcome<ApartmentDetail> = when(val result=read(analytics.get()[apartmentId]) { it.second }) {
        is Outcome.Failure -> result
        is Outcome.Success -> Outcome.Success(result.value.first)
    }
}
