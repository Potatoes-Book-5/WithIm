package com.potatoes.book5.realestate.vehicle.gleo

import com.potatoes.book5.realestate.data.repository.*
import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.*
import com.potatoes.book5.realestate.domain.model.appaction.*
import com.potatoes.book5.realestate.domain.model.tour.*
import com.potatoes.book5.realestate.domain.port.*
import com.potatoes.book5.realestate.domain.usecase.*
import java.time.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal class TourClock(var at: Instant=Instant.parse("2026-11-15T03:00:00Z")) : Clock() {
    override fun instant()=at
    override fun getZone(): ZoneId=ZoneId.of("Asia/Seoul")
    override fun withZone(zone: ZoneId): Clock=Clock.fixed(at,zone)
}
/** Port fake only: domain/data business implementations under test remain production classes. */
internal class TourStoreFake : TourStore {
    val rows=MutableStateFlow<List<SavedTourArea>>(emptyList())
    val briefings=mutableListOf<TourBriefing>()
    val recorded=mutableListOf<AreaEntry>()
    var writes=0
    val notices=mutableListOf<Pair<String,StoredTourNotice>>()
    var onSourceStates: (suspend ()->Unit)?=null
    var onAcknowledge: (suspend ()->Unit)?=null
    val statuses=mutableMapOf<Pair<String,String>,TourSourceState>()
    override fun observeAreas(profile:String)=rows.map { it.filter { row -> row.profile==profile } }
    override suspend fun areas(profile:String): Outcome<List<SavedTourArea>> = Outcome.Success(rows.value.filter { it.profile==profile })
    override suspend fun ensureArea(profile:String,area:TourArea,at:Instant,allowed:()->Boolean): Outcome<AreaSaveResult> {
        if(!allowed()) return Outcome.Failure(DomainError.Invalid("invalidated"))
        val old=rows.value.find { it.profile==profile && it.area.id==area.id }
        if(old!=null) return Outcome.Success(AreaSaveResult(old,true))
        val saved=SavedTourArea(profile,area,at);rows.value=rows.value+saved;writes++
        return Outcome.Success(AreaSaveResult(saved,false))
    }
    override suspend fun deleteArea(profile:String,areaId:String): Outcome<Unit> {
        rows.value=rows.value.filterNot { it.profile==profile && it.area.id==areaId }
        recorded.removeAll { it.profile==profile && it.areaId==areaId }
        return Outcome.Success(Unit)
    }
    override suspend fun deleteAll() { rows.value=emptyList();briefings.clear();recorded.clear() }
    override suspend fun publishSummary(profile:String,areaId:String,prices:PreparedDistrictPrices,observedAt:Instant): Outcome<Boolean> = Outcome.Success(true)
    override suspend fun unread(profile:String,limit:Int)=Outcome.Success(briefings.filter { row -> rows.value.any { it.profile==profile && it.area.id==row.areaId } && row.readVersion!=row.version }.take(limit))
    override suspend fun acknowledge(profile:String,receipts:List<BriefingReceipt>,allowed:()->Boolean): Outcome<Int> {
        onAcknowledge?.invoke()
        if(!allowed()) return Outcome.Failure(DomainError.Invalid("invalidated"))
        var count=0
        receipts.forEach { r -> val index=briefings.indexOfFirst { it.areaId==r.itemId.removePrefix("price-summary:") && it.version==r.version && it.readVersion!=it.version }
            if(index>=0) { briefings[index]=briefings[index].copy(readVersion=r.version);count++ } }
        return Outcome.Success(count)
    }
    override suspend fun publishNotice(profile:String,areaId:String,notice:TourNotice,at:Instant,allowed:()->Boolean): Outcome<Boolean> {
        if(!allowed()) return Outcome.Failure(DomainError.Invalid("invalidated"))
        notices+=profile to StoredTourNotice(notice,"port-fixture-version",at,null)
        return Outcome.Success(true)
    }
    override suspend fun unreadNotices(profile:String): Outcome<List<StoredTourNotice>> = Outcome.Success(notices.filter { it.first==profile }.map { it.second }.distinctBy { it.notice.id })
    override suspend fun sourceStates(profile:String): Outcome<List<TourSourceState>> { onSourceStates?.invoke();return Outcome.Success(statuses.filterKeys { it.first==profile }.values.toList()) }
    override suspend fun setSourceState(profile:String,state:TourSourceState,allowed:()->Boolean): Outcome<Unit> {
        if(!allowed()) return Outcome.Failure(DomainError.Invalid("invalidated"))
        statuses[profile to state.source]=state;return Outcome.Success(Unit)
    }
    override suspend fun latestEntry(profile:String,areaId:String)=Outcome.Success(recorded.filter { it.profile==profile && it.areaId==areaId }.maxByOrNull { it.at })
    override suspend fun recordEntry(entry:AreaEntry,allowed:()->Boolean): Outcome<Unit> {
        if(!allowed() || rows.value.none { it.profile==entry.profile && it.area.id==entry.areaId && it.generation==entry.areaGeneration }) return Outcome.Failure(DomainError.Invalid("deleted"))
        recorded+=entry;return Outcome.Success(Unit)
    }
}
internal class TourFixture(drivingApproved:Boolean=false) {
    val clock=TourClock()
    val voices=InMemoryVoiceContext(clock).apply { setConsented(true);setForeground(true);setAppState(true,"a",visualDetailsAllowed=true) }
    val access=TourAccess(voices,clock,drivingApproved)
    val store=TourStoreFake()
    val market=InMemoryPreparedMarket(clock)
    val location=InMemoryPriceLocation(clock)
    val center=GeoPoint(37.25,127.05)
    val area=TourArea("poi:station","광교중앙역","41117",AreaBoundary.Circle(center),"vworld-place-fixture",clock.instant(),"경기도 수원시 영통구")
    val resolver by lazy { PreparedTourAreas(store,market,price::lastReferenced,access,{ query ->
        if(query==area.name) Outcome.Success(area) else Outcome.Failure(DomainError.Invalid("ambiguous_place"))
    },clock) }
    suspend fun prepare(scope: kotlinx.coroutines.test.TestScope) {
        scope.backgroundScope.launch { resolver.run() }
        resolver.resolve(area.name,context())
        scope.testScheduler.runCurrent()
    }
    var condition=InterestCondition(priceRanges=setOf(PriceRange.UNDER_5))
    var onInterestRead: (suspend ()->Unit)?=null
    val interests=object:InterestRepository {
        override suspend fun get(): InterestCondition { onInterestRead?.invoke();return condition }
        override fun observe()=flowOf<InterestCondition?>(condition)
        override suspend fun save(condition:InterestCondition) { this@TourFixture.condition=condition }
        override suspend fun markSkipped() {}
        override suspend fun clear() { condition=InterestCondition() }
        override suspend fun isDecided()=true
    }
    val navigator=FakeTourNavigator()
    val camera=FakeTourCamera()
    private val scrapRows=object:ScrapRepository {
        val rows=MutableStateFlow<List<Scrap>>(emptyList())
        override fun observeAll()=rows
        override fun observeIsScrapped(apartmentId:String)=rows.map { list -> list.any { it.apartmentId==apartmentId } }
        override suspend fun getAll()=rows.value
        override suspend fun get(apartmentId:String)=rows.value.find { it.apartmentId==apartmentId }
        override suspend fun upsert(scrap:Scrap) { rows.value=rows.value.filterNot { it.apartmentId==scrap.apartmentId }+scrap }
        override suspend fun delete(apartmentId:String) { error("Voice must not toggle off") }
        override suspend fun markChangeSeen(apartmentId:String) {}
        override suspend fun deleteAll() { rows.value=emptyList() }
    }
    val price=LiveAreaPriceSummaryRepository(market,location,voices,clock)
    val save=SaveTourAreaUseCase(resolver,store,access,clock)
    val entries=TrackTourEntriesUseCase(store,access,clock)
    val recommend=RecommendTourApartmentsUseCase(market,interests,clock)
    val check=CheckTourAreaUseCase(resolver,store,recommend,entries,access,clock)
    val briefing=LiveDailyBriefingRepository(store,market,access)
    val adapter=TourIntentAdapter(GetAreaPriceSummaryUseCase(price),save,ScrapVoiceTargetUseCase(scrapRows,market,voices),check,
        GetTourAnalyticsUseCase(market,access),NavigateTourApartmentUseCase(navigator,access),ScanTourAreaUseCase(camera,access),
        GetDailyBriefingUseCase(briefing),ConfirmTourBriefingUseCase(store,access),access)
    val entrance=VerifiedEntrance("a",center,"Fixture vehicle entrance","explicit-user-choice",EntranceEvidence.USER_CONFIRMED,"nav-poi","nav-sub")
    val detail=ApartmentDetail("a","Fixture A","district","dong",500,2001,25,25,0.1,
        Deal(LocalDate.of(2026,11,14),25,3,210_000),null,listOf(PricePoint(YearMonth.of(2026,11),210_000)),emptyList())
    init {
        val today=LocalDate.now(clock)
        market.publish(listOf(PreparedScrapCandidate("a","Fixture A","dong",25,PriceRecord(210_000,today),2001,clock.instant(),"41117")),
            PreparedDistrictPrices("41117","경기도 수원시 영통구",today.minusMonths(3),today,3,2_500_000_000L,3_100_000_000L,"a",clock.instant(),highestComplexName="Fixture A"))
        market.publishMarket(TourMarket("41117",listOf(
            TourCandidate("a","Fixture A","41117","dong",center,PriceRecord(210_000,today),25,500,2001,clock.instant(),PriceRecord(310_000,today)),
            TourCandidate("b","Fixture B","41117","dong",center,PriceRecord(40_000,today),25,500,2001,clock.instant(),PriceRecord(40_000,today))
        ),clock.instant(),priceLeaderId="a"),market.currentRevision())
        market.publishAnalytics(detail,clock.instant());fix()
    }
    fun context(profile:String="p",session:String="s")=AppCallContext("r",session,profile,voices.snapshot(),location.snapshot())
    fun fix(point:GeoPoint=center,accuracy:Double?=10.0) {
        val fix=VehicleLocationState(VehiclePose(point),accuracy,clock.instant(),LocationStatus.AVAILABLE)
        location.updateFix(fix);location.publish(fix,MarketDistrict("41117","경기도 수원시 영통구"),location.revision())
    }
    suspend fun saved() { save(area.name,context()) }
    fun seedBriefing(version:String="v1") {
        store.briefings+=TourBriefing(area.id,area.name,version,2_500_000_000L,3_100_000_000L,3,LocalDate.now(clock).minusMonths(3),LocalDate.now(clock),clock.instant(),clock.instant())
    }
}

internal class FakeTourNavigator : TourNavigator {
    var enabled=true;var active:Boolean?=false;var calls=0;var received:VerifiedEntrance?=null
    var result:Outcome<NavigationReceipt> = Outcome.Success(NavigationReceipt())
    override fun supported()=enabled
    override fun activeRoute()=active
    var isPending=false
    var onConnect: (suspend ()->Unit)?=null
    override fun pending()=isPending
    override suspend fun request(entrance:VerifiedEntrance,requestId:String,allowed:()->Boolean,eventAllowed:()->Boolean): Outcome<NavigationReceipt> {
        onConnect?.invoke()
        if(!allowed()) return Outcome.Failure(DomainError.Invalid("invalidated"))
        calls++;received=entrance;return when(val r=result) { is Outcome.Failure -> r;is Outcome.Success -> Outcome.Success(r.value.copy(requestId=requestId)) }
    }
}
internal class FakeTourCamera : TourCamera {
    var enabled=false;var calls=0
    override fun supported()=enabled
    override suspend fun scan(record:Boolean): Outcome<Unit> { calls++;return Outcome.Success(Unit) }
}
