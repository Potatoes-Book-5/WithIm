package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.*
import com.potatoes.book5.realestate.domain.model.appaction.*
import com.potatoes.book5.realestate.domain.model.tour.*
import com.potatoes.book5.realestate.domain.policy.*
import com.potatoes.book5.realestate.domain.port.*
import java.time.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TourAccess(private val voices: VoiceContextPort, private val clock: Clock = Clock.systemUTC(), private val drivingVoiceApproved: Boolean = false) {
    fun valid(context: AppCallContext, visual: Boolean = false): Boolean {
        val v=context.voice ?: return false
        return observationValid(context) && (!visual || v.visualDetailsAllowed) &&
            (v.visualDetailsAllowed || (!visual && drivingVoiceApproved)) &&
            Duration.between(v.capturedAt,clock.instant())<=Duration.ofSeconds(if(visual) 2 else 60)
    }
    fun observationValid(context: AppCallContext): Boolean {
        val v = context.voice ?: return false
        val age = Duration.between(v.capturedAt, clock.instant())
        return context.trusted && context.profileScope.isNotBlank() && context.appSessionId.isNotBlank() && context.requestId.isNotBlank() &&
            voices.isCurrent(v) && !age.isNegative && age <= Duration.ofSeconds(60)
    }
    fun failure() = Outcome.Failure(DomainError.Invalid("context_invalidated_or_restricted"))
}

class SaveTourAreaUseCase(private val resolver: TourAreaResolver, private val store: TourStore,
                          private val access: TourAccess, private val clock: Clock = Clock.systemUTC()) {
    suspend operator fun invoke(query: String?, context: AppCallContext, preferCurrent: Boolean = false): Outcome<ActionResult<AreaSaveResult>> {
        if (!access.valid(context)) return access.failure()
        return when(val resolved = resolver.resolve(query,context,preferCurrent)) {
            is Outcome.Failure -> resolved
            is Outcome.Success -> when(val result=resolved.value) {
                is ActionResult.NeedsInput -> Outcome.Success(result)
                is ActionResult.Completed -> when(val saved=store.ensureArea(context.profileScope,result.payload,clock.instant()) { access.valid(context) }) {
                    is Outcome.Failure -> saved
                    is Outcome.Success -> if(access.valid(context)) Outcome.Success(ActionResult.Completed(saved.value)) else access.failure()
                }
            }
        }
    }
}

object AreaMembershipPolicy {
    fun evaluate(area: TourArea, location: PriceLocationSnapshot?, now: Instant): Outcome<AreaMembership> {
        val point=location?.point; val at=location?.observedAt
        if(point==null || at==null || Duration.between(at,now).let { it.isNegative || it>Duration.ofSeconds(10) })
            return Outcome.Failure(DomainError.DataNotReady(PreparedDataFailure.STALE))
        val accuracy=location.accuracyMeters
        if(accuracy==null || !accuracy.isFinite() || accuracy !in 0.0..50.0)
            return Outcome.Success(AreaMembership(Membership.UNCERTAIN,"ACCURACY_UNAVAILABLE"))
        val boundary=area.boundary
        if(boundary !is AreaBoundary.Circle)
            return Outcome.Success(AreaMembership(Membership.UNCERTAIN,"DISTRICT_GEOMETRY_UNAVAILABLE"))
        val d=GeoMath.distanceKm(point,boundary.center)*1000
        return Outcome.Success(AreaMembership(when {
            d+accuracy <= boundary.radiusMeters -> Membership.INSIDE
            d-accuracy > boundary.radiusMeters -> Membership.OUTSIDE
            else -> Membership.UNCERTAIN
        },"CIRCLE_WITH_ACCURACY",d))
    }
}

class RecommendTourApartmentsUseCase(private val markets: TourMarketReader, private val interests: InterestRepository,
                                     private val clock: Clock = Clock.systemUTC()) {
    suspend operator fun invoke(area: TourArea): Outcome<Triple<List<RecommendedApartment>,TourMarket,String>> {
        val market=when(val r=markets.readMarket(area.districtCode)) { is Outcome.Failure -> return r; is Outcome.Success -> r.value }
        val condition=interests.get()?.takeUnless { it.isEmpty }
            ?: return Outcome.Success(Triple(emptyList(),market,"PREFERENCES_REQUIRED"))
        val circle=area.boundary as? AreaBoundary.Circle
        val candidates=market.candidates.filter { c ->
            c.districtCode==area.districtCode && (circle==null || c.point?.let { GeoMath.distanceKm(it,circle.center)*1000<=circle.radiusMeters }==true)
        }
        val recommended=candidates.mapNotNull { c ->
            val t=clock.instant().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime()
            val scrap=Scrap(c.id,c.name,c.neighborhood,c.areaPyeong,t,c.price,c.price,t,null,c.households,c.builtYear)
            val scores=ConditionMatchPolicy.match(condition,scrap,t.year).scores
            val priceMatches=condition.priceRanges.isEmpty() || condition.priceRanges.any { range -> when(range) {
                PriceRange.UNDER_5 -> c.price.priceManwon in 1 until 50_000
                PriceRange.FROM_5_TO_10 -> c.price.priceManwon in 50_000 until 100_000
                PriceRange.FROM_10_TO_15 -> c.price.priceManwon in 100_000 until 150_000
                PriceRange.FROM_15_TO_20 -> c.price.priceManwon in 150_000 until 200_000
                PriceRange.OVER_20 -> c.price.priceManwon>=200_000
            } }
            if(!priceMatches || (condition.maxBuildingAge!=null && (c.builtYear ?: 0)>t.year) || scores.values.any { it==null || it < ConditionMatchPolicy.MAX_SCORE }) null
            else RecommendedApartment(c,scores.keys.map { it.name })
        }.sortedWith(compareBy<RecommendedApartment> { it.apartment.price.priceManwon }.thenBy { it.apartment.id }).take(3)
        val missing=candidates.any { (condition.minHouseholds!=null && it.households==null) || (condition.maxBuildingAge!=null && it.builtYear==null) }
        return Outcome.Success(Triple(recommended,market,if(missing) "PARTIAL_METADATA" else "READY"))
    }
}

class CheckTourAreaUseCase(private val resolver: TourAreaResolver, private val store: TourStore,
                           private val recommendations: RecommendTourApartmentsUseCase, private val entries: TourEntryEvidence,
                           private val access: TourAccess, private val clock: Clock = Clock.systemUTC()) {
    suspend operator fun invoke(query: String?, context: AppCallContext, requireSavedTarget: Boolean = true): Outcome<ActionResult<AreaOverview>> {
        if(!access.valid(context)) return access.failure()
        val savedAreas=when(val r=store.areas(context.profileScope)) { is Outcome.Failure -> return r; is Outcome.Success -> r.value }
        val area=if(query==null && requireSavedTarget) {
            if(savedAreas.isEmpty()) return Outcome.Success(ActionResult.NeedsInput("interestArea"))
            val possible=savedAreas.filter { saved ->
                (AreaMembershipPolicy.evaluate(saved.area,context.priceLocation,clock.instant()) as? Outcome.Success)?.value?.state!=Membership.OUTSIDE
            }
            when {
                possible.size==1 -> possible.single().area
                savedAreas.size==1 -> savedAreas.single().area
                else -> return Outcome.Success(ActionResult.NeedsInput("areaQuery"))
            }
        } else when(val r=resolver.resolve(query,context,preferCurrent=query==null)) {
            is Outcome.Failure -> return r
            is Outcome.Success -> when(val a=r.value) { is ActionResult.NeedsInput -> return Outcome.Success(a); is ActionResult.Completed -> a.payload }
        }
        val membership=when(val r=AreaMembershipPolicy.evaluate(area,context.priceLocation,clock.instant())) { is Outcome.Failure -> return r; is Outcome.Success -> r.value }
        val saved=savedAreas.any { it.area.id==area.id }
        val suggestions=recommendations(area)
        val pairs=(suggestions as? Outcome.Success)?.value
        val originalGeneration=savedAreas.find { it.area.id==area.id }?.generation
        val latest=when(val r=store.areas(context.profileScope)) { is Outcome.Failure -> return r; is Outcome.Success -> r.value }
        if(latest.find { it.area.id==area.id }?.generation!=originalGeneration) return Outcome.Failure(DomainError.Invalid("area_changed"))
        val entry=entries.current(context.profileScope,area.id,context.appSessionId)?.takeIf {
            saved && membership.state==Membership.INSIDE && it.at<=context.voice!!.capturedAt && it.contextRevision==context.voice.revision && it.areaGeneration==originalGeneration
        }
        if(!access.valid(context)) return access.failure()
        val leader=pairs?.second?.let { m -> m.candidates.find { it.id==m.priceLeaderId } }
        return Outcome.Success(ActionResult.Completed(AreaOverview(area,membership,saved,entry,pairs?.first.orEmpty(),
            if(pairs!=null) pairs.third else when(val e=(suggestions as Outcome.Failure).error) {
                is DomainError.Invalid -> "PREFERENCES_REQUIRED"
                is DomainError.DataNotReady -> "DATA_NOT_READY"
                else -> "UNAVAILABLE"
            },leader,pairs?.second?.coverage,originalGeneration)))
    }
}

class GetTourAnalyticsUseCase(private val markets: TourMarketReader, private val access: TourAccess) {
    operator fun invoke(apartmentId: String?, context: AppCallContext): Outcome<ActionResult<ApartmentDetail>> {
        if(!access.valid(context,visual=true)) return access.failure()
        val id=apartmentId ?: context.voice?.apartmentId() ?: return Outcome.Success(ActionResult.NeedsInput("apartment"))
        return when(val r=markets.readAnalytics(id)) {
            is Outcome.Failure -> r
            is Outcome.Success -> if(access.valid(context,true)) Outcome.Success(ActionResult.Completed(r.value)) else access.failure()
        }
    }
}

class NavigateTourApartmentUseCase(private val navigator: TourNavigator, private val access: TourAccess) {
    private val gate=Mutex()
    suspend operator fun invoke(entrance: VerifiedEntrance?, replaceConfirmed: Boolean, context: AppCallContext): Outcome<ActionResult<NavigationReceipt>> {
        if(!gate.tryLock()) return Outcome.Failure(DomainError.Invalid("navigation_busy"))
        try {
            if(!access.valid(context,true)) return access.failure()
            if(entrance==null) return Outcome.Success(ActionResult.NeedsInput("entrance"))
            val p=entrance.point
            if(entrance.evidence==EntranceEvidence.UNVERIFIED || entrance.apartmentId.isBlank() || entrance.sourceId.isBlank() || entrance.address.isBlank() ||
                !p.latitude.isFinite() || !p.longitude.isFinite() || p.latitude !in -90.0..90.0 || p.longitude !in -180.0..180.0)
                return Outcome.Failure(DomainError.Invalid("unverified_entrance"))
            if(context.voice?.selectedApartmentId!=entrance.apartmentId) return Outcome.Success(ActionResult.NeedsInput("apartment"))
            if(entrance.navigationPoiId.isNullOrBlank() || entrance.navigationPoiSubId.isNullOrBlank()) return Outcome.Success(ActionResult.NeedsInput("navigationPoi"))
            if(navigator.pending()) return Outcome.Failure(DomainError.Invalid("navigation_busy"))
            if(!navigator.supported()) return Outcome.Failure(DomainError.Unsupported("navigation"))
            if(navigator.activeRoute()!=false && !replaceConfirmed) return Outcome.Success(ActionResult.NeedsInput("replaceRoute"))
            if(!access.valid(context,true)) return access.failure()
            return when(val r=navigator.request(entrance,context.requestId,allowed={ access.valid(context,true) },eventAllowed={ access.observationValid(context) && context.voice?.visualDetailsAllowed==true })) {
                is Outcome.Failure -> r
                is Outcome.Success -> if(access.valid(context,true)) Outcome.Success(ActionResult.Completed(r.value)) else access.failure()
            }
        } finally { gate.unlock() }
    }
}

class ScanTourAreaUseCase(private val camera: TourCamera, private val access: TourAccess) {
    suspend operator fun invoke(record: Boolean, context: AppCallContext): Outcome<ActionResult<Unit>> {
        if(!access.valid(context,true)) return access.failure()
        if(!camera.supported()) return Outcome.Failure(DomainError.Unsupported("camera_access_unverified"))
        if(record) return Outcome.Failure(DomainError.Unsupported("recording_consent_unverified"))
        return when(val r=camera.scan(false)) { is Outcome.Failure -> r; is Outcome.Success -> Outcome.Success(ActionResult.Completed(Unit)) }
    }
}

class ConfirmTourBriefingUseCase(private val store: TourStore, private val access: TourAccess) {
    suspend operator fun invoke(receipts: List<BriefingReceipt>, context: AppCallContext): Outcome<Int> {
        if(!access.valid(context)) return access.failure()
        if(receipts.size>3 || receipts.any { it.itemId.isBlank() || it.version.isBlank() }) return Outcome.Failure(DomainError.Invalid("invalid_receipts"))
        val result=store.acknowledge(context.profileScope,receipts) { access.valid(context) }
        return if(access.valid(context)) result else access.failure()
    }
}
