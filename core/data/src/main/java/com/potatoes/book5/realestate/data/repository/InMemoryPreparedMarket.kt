package com.potatoes.book5.realestate.data.repository

import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.*
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
) : PreparedScrapReader {
    private data class State(
        val candidates: Map<String, PreparedScrapCandidate> = emptyMap(),
        val districts: Map<String, PreparedDistrictPrices> = emptyMap(),
        val revision: Long = 0,
    )
    private val state = AtomicReference(State())
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
            State(rows.toMap(), regions.toMap(), previous.revision + if (wholeAccepted) 1 else 0)
        }
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

    fun readDistrictPrices(districtCode: String): Outcome<PreparedDistrictPrices> {
        val result = read(state.get().districts[districtCode]) { it.sourceAsOf }
        if (result is Outcome.Success && result.value.periodEndInclusive !=
            clock.instant().atZone(java.time.ZoneId.of("Asia/Seoul")).toLocalDate())
            return Outcome.Failure(DomainError.DataNotReady(PreparedDataFailure.STALE))
        return result
    }
}
