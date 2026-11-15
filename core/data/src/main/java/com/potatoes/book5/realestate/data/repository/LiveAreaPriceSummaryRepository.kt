package com.potatoes.book5.realestate.data.repository

import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.appaction.*
import com.potatoes.book5.realestate.domain.port.*
import com.potatoes.book5.realestate.domain.policy.MarketPeriod
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicReference

/** Real MOLIT completed batches. No resolver, trade provider, geocoder or collector lock. */
class LiveAreaPriceSummaryRepository(
    private val prices: PreparedPriceReader,
    private val locations: InMemoryPriceLocation,
    private val voices: VoiceContextPort,
    private val clock: Clock = Clock.systemUTC(),
) : AreaPriceSummaryRepository {
    private data class Reference(val area: ResolvedArea, val session: String, val profile: String,
                                 val revision: Long, val createdAt: Instant)
    private val last = AtomicReference<Reference?>(null)
    fun captureLocation() = locations.snapshot()

    override suspend fun get(target: AreaTarget, context: AppCallContext): Outcome<ActionResult<PriceSummaryPayload>> {
        val voice = context.voice ?: return unavailableContext()
        if (!voices.isCurrent(voice)) return unavailableContext()
        val now = clock.instant()
        val location = context.priceLocation
        val reference = last.get()
        val area = when (target.kind) {
            AreaTargetKind.CURRENT_LOCATION -> {
                val currentArea = location?.area
                val age = location?.observedAt?.let { Duration.between(it, now) }
                val accuracy = location?.accuracyMeters
                if (location == null || !locations.isCurrent(location) || currentArea == null || age == null ||
                    age.isNegative || age > Duration.ofSeconds(10) || accuracy == null || !accuracy.isFinite() || accuracy !in 0.0..50.0)
                    return Outcome.Success(ActionResult.NeedsInput("areaQuery"))
                currentArea
            }
            AreaTargetKind.LAST_REFERENCED_AREA -> {
                val age = reference?.let { Duration.between(it.createdAt, now) }
                if (reference == null || reference.session != context.appSessionId || reference.profile != context.profileScope ||
                    reference.revision != voice.revision || age == null || age.isNegative || age > Duration.ofSeconds(60))
                    return Outcome.Success(ActionResult.NeedsInput("areaQuery"))
                reference.area
            }
            AreaTargetKind.NAMED_AREA -> {
                val query = target.query?.trim()?.replace(Regex("\\s+"), " ")
                val matches = prices.knownDistricts().filter {
                    it.name == query || it.name.substringAfterLast(' ') == query
                }.distinctBy { it.id }
                if (query.isNullOrBlank() || matches.size != 1)
                    return Outcome.Success(ActionResult.NeedsInput("areaQuery"))
                matches.single()
            }
        }
        val prepared = when (val result = prices.readDistrictPrices(area.id)) {
            is Outcome.Failure -> return result
            is Outcome.Success -> result.value
        }
        // Capture all fields from one immutable batch, even if the collector replaces it now.
        val today = now.atZone(ZoneId.of("Asia/Seoul")).toLocalDate()
        val average = prepared.averagePriceKrw
        val highest = prepared.highestPriceKrw
        if (prepared.districtCode != area.id || prepared.scope != "DISTRICT" || prepared.sampleCount < 0 ||
            prepared.districtName.isBlank() || prepared.periodEndInclusive != today || prepared.periodStart != today.minusMonths(MarketPeriod.MONTHS) ||
            prepared.areaMinM2 != 80 || prepared.areaMaxExclusiveM2 != 90 ||
            (prepared.sampleCount == 0 && (prepared.averagePriceKrw != null || prepared.highestPriceKrw != null)) ||
            (prepared.sampleCount > 0 && (average == null || highest == null || average <= 0 || highest < average)))
            return Outcome.Failure(DomainError.InvalidResponse)
        val age = Duration.between(prepared.sourceAsOf, now)
        if (age.isNegative || age >= Duration.ofMinutes(5))
            return Outcome.Failure(DomainError.DataNotReady(PreparedDataFailure.STALE))
        if (!voices.isCurrent(voice) || (target.kind == AreaTargetKind.CURRENT_LOCATION && !locations.isCurrent(location!!)))
            return unavailableContext()
        val payload = PriceSummaryPayload(ResolvedArea(prepared.districtCode, prepared.districtName),
            prepared.averagePriceKrw, prepared.highestPriceKrw, prepared.highestComplexId, prepared.highestComplexName,
            prepared.sampleCount, prepared.areaMinM2, prepared.areaMaxExclusiveM2,
            prepared.periodStart.toString(), prepared.periodEndInclusive.plusDays(1).toString(),
            ResultMetadata(ExecutionMode.LIVE, false, "molit-apartment-sale", prepared.sourceAsOf.toString()),
            scope = prepared.scope,
            locationObservedAt = location?.observedAt?.toString().takeIf { target.kind == AreaTargetKind.CURRENT_LOCATION },
            locationAccuracyMeters = location?.accuracyMeters.takeIf { target.kind == AreaTargetKind.CURRENT_LOCATION },
            locationSource = location?.source?.name.takeIf { target.kind == AreaTargetKind.CURRENT_LOCATION })
        if (target.kind != AreaTargetKind.LAST_REFERENCED_AREA)
            last.set(Reference(payload.area, context.appSessionId, context.profileScope, voice.revision, now))
        return Outcome.Success(ActionResult.Completed(payload))
    }

    private fun unavailableContext(): Outcome.Failure = Outcome.Failure(DomainError.Invalid("context_invalidated"))
}

/** Never return invented announcements or pretend that a region was persisted. */
class UnavailableInterestAreaRepository : InterestAreaRepository {
    override suspend fun save(target: AreaTarget, context: AppCallContext): Outcome<ActionResult<InterestAreaSavePayload>> =
        Outcome.Failure(DomainError.Unsupported("interest_area_persistence"))
}
class UnavailableDailyBriefingRepository : DailyBriefingRepository {
    override suspend fun get(context: AppCallContext): Outcome<ActionResult<DailyBriefingPayload>> =
        Outcome.Failure(DomainError.Unsupported("interest_area_briefing_sources"))
}
