package com.potatoes.book5.realestate.data.repository

import com.potatoes.book5.realestate.domain.error.DomainError
import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.AddressKind
import com.potatoes.book5.realestate.domain.model.Apartment
import com.potatoes.book5.realestate.domain.model.ApartmentDetail
import com.potatoes.book5.realestate.domain.model.ApartmentTrade
import com.potatoes.book5.realestate.domain.model.AreaRange
import com.potatoes.book5.realestate.domain.model.Deal
import com.potatoes.book5.realestate.domain.model.GeoPoint
import com.potatoes.book5.realestate.domain.model.InterestCondition
import com.potatoes.book5.realestate.domain.model.NearbyApartment
import com.potatoes.book5.realestate.domain.model.Neighborhood
import com.potatoes.book5.realestate.domain.model.PricePoint
import com.potatoes.book5.realestate.domain.model.PriceRange
import com.potatoes.book5.realestate.domain.model.RankingSnapshot
import com.potatoes.book5.realestate.domain.model.TradePage
import com.potatoes.book5.realestate.domain.model.VehiclePose
import com.potatoes.book5.realestate.domain.policy.GeoMath
import com.potatoes.book5.realestate.domain.port.AddressGeocoder
import com.potatoes.book5.realestate.domain.port.ApartmentTradeRepository
import com.potatoes.book5.realestate.domain.port.InterestRepository
import com.potatoes.book5.realestate.domain.port.MarketRepository
import com.potatoes.book5.realestate.domain.port.MarketDistrict
import com.potatoes.book5.realestate.domain.port.MarketDistrictResolver
import com.potatoes.book5.realestate.domain.port.LocationStatus
import com.potatoes.book5.realestate.domain.port.LocationSource
import com.potatoes.book5.realestate.domain.port.VehicleLocationProvider
import com.potatoes.book5.realestate.domain.port.VehicleLocationState
import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/** Real MOLIT transactions shared by ranking, neighborhood suggestions and apartment detail. */
class LiveMarketRepository(
    private val tradeRepository: ApartmentTradeRepository,
    private val geocoder: AddressGeocoder,
    private val interestRepository: InterestRepository,
    private val locationProvider: VehicleLocationProvider,
    private val districtResolver: MarketDistrictResolver,
    private val clock: Clock = Clock.system(ZoneId.of("Asia/Seoul")),
) : MarketRepository {
    private data class MarketState(val snapshot: RankingSnapshot, val located: List<NearbyApartment>)
    private data class VerifiedCandidate(val id: String, val trade: ApartmentTrade, val point: GeoPoint)
    private data class LoadedCoverage(
        val from: LocalDate,
        val to: LocalDate,
        val center: GeoPoint,
        val districts: List<MarketDistrict>,
    )

    private val mutex = Mutex()
    private val todayAtStart = LocalDate.now(clock)
    @Volatile private var currentPose: VehiclePose? = null
    @Volatile private var currentLocationSource: LocationSource = LocationSource.VEHICLE
    @Volatile private var locationIssue: DomainError? = DomainError.Invalid("location-searching")
    @Volatile private var locationIssueLabel: String = LOCATION_PENDING_LABEL
    @Volatile private var lastRankedPose: VehiclePose? = null
    @Volatile private var lastRequestedPose: VehiclePose? = null
    @Volatile private var verifiedCandidates: List<VerifiedCandidate> = emptyList()
    private val coordinateCache = mutableMapOf<String, GeoPoint?>()
    private val state = MutableStateFlow(
        MarketState(
            RankingSnapshot(
                neighborhoodName = RADIUS_AREA_LABEL,
                radiusKm = RADIUS_KM,
                top = emptyList(),
                interestMatches = emptyList(),
                others = emptyList(),
                dataAsOf = LocalDateTime.now(clock),
                vehicle = null,
                loading = true,
                periodStart = todayAtStart.minusMonths(1),
                periodEnd = todayAtStart,
                referenceLabel = LOCATION_PENDING_LABEL,
            ),
            emptyList(),
        )
    )

    // Memory only. In particular, geocoder coordinates are never stored with month records.
    private val monthRecords = mutableMapOf<Pair<String, YearMonth>, List<ApartmentTrade>>()
    private val monthFetchedAt = mutableMapOf<Pair<String, YearMonth>, Instant>()
    @Volatile private var districtNamesByCode: Map<String, String> = emptyMap()
    @Volatile private var recentRecords: List<ApartmentTrade> = emptyList()
    @Volatile private var representativeById: Map<String, ApartmentTrade> = emptyMap()
    @Volatile private var loadedCoverage: LoadedCoverage? = null
    private var lastAttempt: Instant? = null
    private var lastOutcome: Outcome<Unit>? = null

    override fun observeNearbyRanking(): Flow<RankingSnapshot> = combine(state, interestRepository.observe()) { market, interest ->
        val matches = if (interest == null || interest.isEmpty) emptyList() else market.located.filter {
            matchesInterest(interest, it.apartment, LocalDate.now(clock).year)
        }
        val topIds = market.snapshot.top.mapTo(mutableSetOf()) { it.apartment.id }
        val matchIds = matches.mapTo(mutableSetOf()) { it.apartment.id }
        market.snapshot.copy(
            interestMatches = matches,
            others = market.located.filter { it.apartment.id !in topIds && it.apartment.id !in matchIds },
        )
    }

    override suspend fun refresh(): Outcome<Unit> = mutex.withLock {
        val now = clock.instant()
        val today = LocalDate.now(clock)
        val from = today.minusMonths(1)
        val pose = currentPose ?: return@withLock fail(locationIssue ?: DomainError.Invalid("location-unavailable"))
        val previous = lastOutcome
        val last = lastAttempt
        val coverage = loadedCoverage
        if (previous != null && last != null && coverage?.from == from && coverage.to == today &&
            GeoMath.distanceKm(coverage.center, pose.location) < RERANK_DISTANCE_KM && Duration.between(last, now) <
            if (previous is Outcome.Success) SUCCESS_TTL else FAILURE_TTL
        ) return@withLock previous

        state.value = state.value.copy(snapshot = state.value.snapshot.copy(loading = true, error = null))
        try {
            val districts = when (val resolved = districtResolver.districts(pose.location, RADIUS_KM.toDouble())) {
                is Outcome.Failure -> return@withLock fail(resolved.error)
                is Outcome.Success -> resolved.value.distinctBy { it.code }
            }
            if (districts.isEmpty() || districts.any { !it.code.matches(Regex("[0-9]{5}")) || it.name.isBlank() }) {
                return@withLock fail(DomainError.InvalidResponse)
            }
            districtNamesByCode = districtNamesByCode + districts.associate { it.code to it.name }
            val months = buildList {
                var month = YearMonth.from(from)
                while (month <= YearMonth.from(today)) { add(month); month = month.plusMonths(1) }
            }
            val keys = districts.flatMap { district -> months.map { district.code to it } }
            val currentMonth = YearMonth.from(today)
            val toFetch = keys.filter { key ->
                val fetchedAt = monthFetchedAt[key]
                val ttl = if (key.second == currentMonth) SUCCESS_TTL else RECENT_MONTH_TTL
                monthRecords[key] == null || fetchedAt == null || Duration.between(fetchedAt, now) >= ttl
            }
            val semaphore = Semaphore(MAX_CONCURRENT_REGIONS)
            val responses = coroutineScope {
                toFetch.map { key -> async { key to semaphore.withPermit { fetchMonth(key.first, key.second) } } }.awaitAll()
            }
            val fetched = mutableMapOf<Pair<String, YearMonth>, List<ApartmentTrade>>()
            for ((key, response) in responses) when (response) {
                is Outcome.Failure -> return@withLock fail(response.error)
                is Outcome.Success -> fetched[key] = response.value
            }
            val window = keys.flatMap { fetched[it] ?: monthRecords.getValue(it) }
                .filter { !it.hasCancellationNotice && it.contractDate in from..today }
            val representatives = window.filter(::usable).groupBy { stableId(it) }.mapNotNull { (id, trades) ->
                if (id == null) null else trades.maxWithOrNull(compareBy<ApartmentTrade> { it.priceManwon }
                    .thenBy { it.contractDate })?.let { id to it }
            }.sortedWith(compareByDescending<Pair<String, ApartmentTrade>> { it.second.priceManwon }
                .thenByDescending { it.second.contractDate })

            monthRecords.putAll(fetched)
            fetched.keys.forEach { monthFetchedAt[it] = clock.instant() }
            recentRecords = window
            representativeById = representatives.toMap()
            loadedCoverage = LoadedCoverage(from, today, pose.location, districts)
            when (val ranked = rankAtPose(pose, representatives, from, today)) {
                is Outcome.Failure -> return@withLock fail(ranked.error)
                is Outcome.Success -> Unit
            }
            lastAttempt = clock.instant()
            lastOutcome = Outcome.Success(Unit)
            lastOutcome!!
        } catch (cancelled: CancellationException) {
            state.value = state.value.copy(snapshot = state.value.snapshot.copy(loading = false))
            throw cancelled
        }
    }

    private fun fail(error: DomainError): Outcome<Unit> {
        state.value = state.value.copy(snapshot = state.value.snapshot.copy(loading = false, error = error))
        lastAttempt = clock.instant()
        return Outcome.Failure(error).also { lastOutcome = it }
    }

    /** Recenter on every fix; spatially throttle and cancel only superseded geocoding scans. */
    override suspend fun run() = coroutineScope {
        var rankJob: Job? = null
        locationProvider.locations.collect { fix ->
            currentLocationSource = fix.source
            val pose = fix.usablePose()
            if (pose == null) {
                rankJob?.cancel()
                currentPose = null
                lastRankedPose = null
                lastRequestedPose = null
                locationIssue = DomainError.Invalid(if (fix.status == LocationStatus.AVAILABLE)
                    "location-inaccurate-or-stale" else "location-${fix.status.name.lowercase()}")
                locationIssueLabel = if (fix.source == LocationSource.EMULATOR_ROUTE)
                    "테스트 위치 확인 필요 · 실거래 데이터 보관 중"
                else "차량 위치 확인 필요 · 실거래 데이터 보관 중"
                state.value = MarketState(
                    state.value.snapshot.copy(
                        top = emptyList(), interestMatches = emptyList(), others = emptyList(),
                        vehicle = null, loading = false,
                        error = locationIssue,
                        referenceLabel = locationIssueLabel,
                    ),
                    emptyList(),
                )
                return@collect
            }
            currentPose = pose
            locationIssue = null
            locationIssueLabel = referenceLabel(fix.source)
            val known = verifiedCandidates.map { nearby(it.id, it.trade, it.point, pose) }
                .filter { it.distanceKm <= RADIUS_KM }
                .sortedWith(compareByDescending<NearbyApartment> { it.apartment.priceManwon }
                    .thenByDescending { it.apartment.dealDate })
            val previous = lastRequestedPose ?: lastRankedPose
            val moved = previous == null || GeoMath.distanceKm(previous.location, pose.location) >= RERANK_DISTANCE_KM
            val boundaryCrossed = previous != null && verifiedCandidates.any {
                (GeoMath.distanceKm(previous.location, it.point) <= RADIUS_KM) !=
                    (GeoMath.distanceKm(pose.location, it.point) <= RADIUS_KM)
            }
            val shouldRank = moved || boundaryCrossed
            val previousSnapshot = state.value.snapshot
            val previousError = previousSnapshot.error
            val locationError = previousError is DomainError.Invalid && previousError.reason.startsWith("location-")
            state.value = MarketState(
                previousSnapshot.copy(
                    top = known.take(TOP_LIMIT), interestMatches = emptyList(), others = emptyList(),
                    vehicle = pose,
                    loading = shouldRank || rankJob?.isActive == true || previousSnapshot.loading,
                    error = if (shouldRank || locationError) null else previousError,
                    referenceLabel = referenceLabel(fix.source),
                ),
                known.take(MAX_MAP_PINS),
            )
            if (shouldRank) {
                lastRequestedPose = pose
                rankJob?.cancel()
                rankJob = launch {
                    delay(RERANK_DEBOUNCE_MILLIS)
                    val today = LocalDate.now(clock)
                    val coverage = loadedCoverage
                    if (coverage == null || coverage.from != today.minusMonths(1) || coverage.to != today ||
                        GeoMath.distanceKm(coverage.center, pose.location) >= RERANK_DISTANCE_KM) {
                        when (refresh()) {
                            is Outcome.Failure -> return@launch
                            is Outcome.Success -> return@launch // refresh already ranked at the current pose.
                        }
                    }
                    mutex.withLock {
                        val from = today.minusMonths(1)
                        val representatives = representativeById.entries.map { it.key to it.value }
                            .sortedWith(compareByDescending<Pair<String, ApartmentTrade>> { it.second.priceManwon }
                                .thenByDescending { it.second.contractDate })
                        when (val ranked = rankAtPose(pose, representatives, from, today)) {
                            is Outcome.Failure -> fail(ranked.error)
                            is Outcome.Success -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun VehicleLocationState.usablePose(): VehiclePose? {
        if (status != LocationStatus.AVAILABLE) return null
        val current = pose ?: return null
        val accuracy = accuracyMeters ?: return null
        val observed = observedAt ?: return null
        val age = Duration.between(observed, clock.instant())
        if (!accuracy.isFinite() || accuracy !in 0.0..MAX_ACCURACY_METERS ||
            age > MAX_FIX_AGE || age < MAX_FUTURE_FIX_OFFSET) return null
        val point = current.location
        if (!point.latitude.isFinite() || !point.longitude.isFinite() ||
            point.latitude !in -90.0..90.0 || point.longitude !in -180.0..180.0) return null
        return current
    }

    /** Requires the caller to hold [mutex]; uses only in-memory transactions and coordinates. */
    private suspend fun rankAtPose(
        pose: VehiclePose,
        representatives: List<Pair<String, ApartmentTrade>>,
        from: LocalDate,
        to: LocalDate,
    ): Outcome<Unit> {
        val located = linkedMapOf<String, NearbyApartment>()
        val top = mutableListOf<NearbyApartment>()
        for ((id, trade) in representatives) {
            val found = when (val response = locate(id, trade, coordinateCache, pose)) {
                is Outcome.Failure -> return response
                is Outcome.Success -> response.value
            } ?: continue
            if (found.distanceKm > RADIUS_KM) continue
            located[id] = found
            top += found
            if (top.size == TOP_LIMIT) break
        }
        val condition = interestRepository.get()
        val topIds = top.mapTo(mutableSetOf()) { it.apartment.id }
        val remaining = representatives.filter { it.first !in topIds }
        val prioritized = if (condition == null || condition.isEmpty) remaining else
            remaining.filter { matchesInterest(condition, it.second, to.year) } +
                remaining.filterNot { matchesInterest(condition, it.second, to.year) }
        var extraAttempts = 0
        for ((id, trade) in prioritized) {
            if (located.size >= MAX_MAP_PINS || extraAttempts >= MAX_EXTRA_GEOCODES) break
            extraAttempts++
            val found = when (val response = locate(id, trade, coordinateCache, pose)) {
                is Outcome.Failure -> return response
                is Outcome.Success -> response.value
            } ?: continue
            if (found.distanceKm <= RADIUS_KM) located[id] = found
        }
        verifiedCandidates = representatives.mapNotNull { (id, trade) ->
            val address = trade.addressRequest() ?: return@mapNotNull null
            coordinateCache["${address.first}:${address.second}"]?.let { VerifiedCandidate(id, trade, it) }
        }
        val displayPose = currentPose ?: return Outcome.Success(Unit)
        if (GeoMath.distanceKm(pose.location, displayPose.location) >= RERANK_DISTANCE_KM) {
            return Outcome.Success(Unit) // A newer significant move has its own scan.
        }
        val displayed = verifiedCandidates.map { nearby(it.id, it.trade, it.point, displayPose) }
            .filter { it.distanceKm <= RADIUS_KM }
            .sortedWith(compareByDescending<NearbyApartment> { it.apartment.priceManwon }
                .thenByDescending { it.apartment.dealDate })
        lastRankedPose = displayPose
        lastRequestedPose = displayPose
        state.value = MarketState(
            state.value.snapshot.copy(
                neighborhoodName = RADIUS_AREA_LABEL, radiusKm = RADIUS_KM,
                top = displayed.take(TOP_LIMIT), interestMatches = emptyList(), others = emptyList(),
                vehicle = displayPose, loading = false, error = null,
                periodStart = from, periodEnd = to, referenceLabel = referenceLabel(currentLocationSource),
            ),
            displayed.take(MAX_MAP_PINS),
        )
        return Outcome.Success(Unit)
    }

    override suspend fun suggestions(): Outcome<List<Neighborhood>> {
        if (recentRecords.isEmpty()) when (val loaded = refresh()) {
            is Outcome.Failure -> return loaded
            is Outcome.Success -> Unit
        }
        val neighborhoods = recentRecords.asSequence().mapNotNull { trade ->
            trade.legalDong?.trim()?.takeIf(String::isNotBlank)?.let { trade.districtCode to it }
        }.groupingBy { it }.eachCount().entries
            .sortedWith(compareByDescending<Map.Entry<Pair<String, String>, Int>> { it.value }
                .thenBy { it.key.first }.thenBy { it.key.second })
            .map { (key, _) ->
                val (code, dong) = key
                Neighborhood("molit:$code:dong:$dong", "${districtNamesByCode[code] ?: code} $dong")
            }
        return Outcome.Success(neighborhoods)
    }

    override suspend fun detail(apartmentId: String): Outcome<ApartmentDetail> = mutex.withLock {
        val districtCode = Regex("^molit:([0-9]{5}):.+$").matchEntire(apartmentId)?.groupValues?.get(1)
            ?: return@withLock Outcome.Failure(DomainError.Invalid("unknown-apartment"))
        val end = YearMonth.from(LocalDate.now(clock))
        val records = mutableListOf<ApartmentTrade>()
        for (offset in 11 downTo 0) {
            val month = end.minusMonths(offset.toLong())
            val cacheKey = districtCode to month
            val cached = monthRecords[cacheKey]
            val monthTrades = if (cached != null) cached else when (val response = fetchMonth(districtCode, month)) {
                is Outcome.Failure -> return@withLock response
                is Outcome.Success -> response.value.also { monthRecords[cacheKey] = it }
            }
            records += monthTrades
        }
        val trades = records.filter { !it.hasCancellationNotice && stableId(it) == apartmentId && usable(it) }
            .sortedWith(compareByDescending<ApartmentTrade> { it.contractDate }.thenByDescending { it.priceManwon })
        val newest = trades.firstOrNull() ?: return@withLock Outcome.Failure(DomainError.Invalid("unknown-apartment"))
        val deals = trades.map { it.toDeal() }
        val located = state.value.located.find { it.apartment.id == apartmentId }
        Outcome.Success(
            ApartmentDetail(
                id = apartmentId,
                name = newest.apartmentName ?: "이름 미제공",
                district = districtNamesByCode[districtCode] ?: "법정동 코드 $districtCode",
                neighborhood = newest.legalDong ?: "동 정보 없음",
                households = null,
                builtYear = trades.firstNotNullOfOrNull { it.builtYear },
                minPyeong = deals.minOfOrNull { it.areaPyeong },
                maxPyeong = deals.maxOfOrNull { it.areaPyeong },
                distanceKm = located?.distanceKm,
                latestDeal = deals.first(),
                previousDeal = deals.getOrNull(1),
                trend = trades.groupBy { YearMonth.from(it.contractDate) }.map { (month, group) ->
                    PricePoint(month, group.maxOf { it.priceManwon })
                }.sortedBy { it.month },
                recentDeals = deals.take(3),
            )
        )
    }

    override suspend fun latestDeal(apartmentId: String): Outcome<Deal> = when (val result = detail(apartmentId)) {
        is Outcome.Failure -> result
        is Outcome.Success -> Outcome.Success(result.value.latestDeal)
    }

    private suspend fun fetchMonth(districtCode: String, month: YearMonth): Outcome<List<ApartmentTrade>> {
        var page = 1
        var count: Int? = null
        val records = mutableListOf<ApartmentTrade>()
        while (true) {
            val result = when (val response = tradeRepository.getTrades(districtCode, month, page, PAGE_SIZE)) {
                is Outcome.Failure -> return response
                is Outcome.Success -> response.value
            }
            if (result.pageNumber != page || result.pageSize != PAGE_SIZE ||
                (count != null && count != result.totalCount)) return Outcome.Failure(DomainError.InvalidResponse)
            count = result.totalCount
            records += result.trades
            page = result.nextPage ?: break
        }
        if (records.size != count) return Outcome.Failure(DomainError.InvalidResponse)
        return Outcome.Success(records)
    }

    private suspend fun locate(
        id: String,
        trade: ApartmentTrade,
        points: MutableMap<String, GeoPoint?>,
        pose: VehiclePose,
    ): Outcome<NearbyApartment?> {
        val address = trade.addressRequest() ?: return Outcome.Success(null)
        val key = "${address.first}:${address.second}"
        val point = (if (points.containsKey(key)) points[key] else when (val result = geocoder.geocode(address.second, address.first)) {
            is Outcome.Success -> GeoPoint(result.value.latitude, result.value.longitude).also { points[key] = it }
            is Outcome.Failure -> when (val error = result.error) {
                is DomainError.Invalid -> if (error.reason == "invalid-address") {
                    points[key] = null
                    null
                } else return result
                // ProviderRejected can also mean bad credentials; never turn it into an empty ranking.
                else -> return result
            }
        }) ?: return Outcome.Success(null)
        return Outcome.Success(nearby(id, trade, point, pose))
    }

    private fun nearby(id: String, trade: ApartmentTrade, point: GeoPoint, pose: VehiclePose): NearbyApartment =
        NearbyApartment(
            apartment = Apartment(
                id = id,
                name = trade.apartmentName ?: "이름 미제공",
                priceManwon = trade.priceManwon,
                areaPyeong = trade.areaPyeong(),
                dealDate = trade.contractDate,
                location = point,
            ),
            distanceKm = GeoMath.distanceKm(pose.location, point),
            bearingDeg = relativeBearing(pose, point),
            directionKnown = pose.headingDeg != null,
        )

    private fun ApartmentTrade.addressRequest(): Pair<AddressKind, String>? {
        val districtName = districtNamesByCode[districtCode] ?: return null
        roadAddress?.let { return AddressKind.ROAD to "$districtName ${it.addressLine}" }
        val dong = legalDong?.trim()?.takeIf(String::isNotBlank) ?: return null
        val lot = lotNumber?.trim()?.takeIf(String::isNotBlank) ?: return null
        return AddressKind.PARCEL to "$districtName $dong $lot"
    }

    private fun stableId(trade: ApartmentTrade): String? {
        val code = trade.districtCode
        if (!code.matches(Regex("[0-9]{5}"))) return null
        trade.apartmentId?.trim()?.takeIf(String::isNotBlank)?.let { return "molit:$code:$it" }
        val name = trade.apartmentName?.trim()?.takeIf(String::isNotBlank) ?: return null
        val address = trade.roadAddress?.addressLine?.let { "road:$it" }
            ?: listOfNotNull(trade.legalDong, trade.lotNumber).takeIf { it.size == 2 }
                ?.joinToString(" ", prefix = "parcel:") ?: return null
        val raw = "$name|$address"
        val hash = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }.take(24)
        return "molit:$code:address:$hash"
    }

    private fun usable(trade: ApartmentTrade): Boolean = trade.apartmentName?.isNotBlank() == true &&
        trade.exclusiveAreaSquareMeters != null && trade.floor != null && stableId(trade) != null

    private fun ApartmentTrade.areaPyeong(): Int = exclusiveAreaSquareMeters!!
        .divide(java.math.BigDecimal("3.305785"), 0, RoundingMode.HALF_UP).toInt().coerceAtLeast(1)

    private fun ApartmentTrade.toDeal() = Deal(contractDate, areaPyeong(), floor!!, priceManwon)

    private fun matchesInterest(condition: InterestCondition, apartment: Apartment, year: Int): Boolean {
        val trade = representativeById[apartment.id] ?: return false
        return matchesInterest(condition, trade, year)
    }

    private fun matchesInterest(condition: InterestCondition, trade: ApartmentTrade, year: Int): Boolean {
        if (condition.minHouseholds != null) return false // MOLIT trade feed has no household count.
        if (condition.neighborhoods.isNotEmpty() && condition.neighborhoods.none {
            it.code == "molit:${trade.districtCode}:dong:${trade.legalDong}"
        }) return false
        if (condition.priceRanges.isNotEmpty() && condition.priceRanges.none { it.includes(trade.priceManwon) }) return false
        val area = trade.exclusiveAreaSquareMeters?.let { trade.areaPyeong() }
        if (condition.areaRanges.isNotEmpty() && (area == null || condition.areaRanges.none { it.includes(area) })) return false
        val age = condition.maxBuildingAge
        val builtYear = trade.builtYear
        if (age != null && (builtYear == null || year - builtYear > age.maxYears)) return false
        return true
    }

    private fun PriceRange.includes(amount: Long): Boolean = when (this) {
        PriceRange.UNDER_5 -> amount < 50_000
        PriceRange.FROM_5_TO_10 -> amount in 50_000L until 100_000L
        PriceRange.FROM_10_TO_15 -> amount in 100_000L until 150_000L
        PriceRange.FROM_15_TO_20 -> amount in 150_000L until 200_000L
        PriceRange.OVER_20 -> amount >= 200_000
    }

    private fun AreaRange.includes(pyeong: Int): Boolean = when (this) {
        AreaRange.UNDER_20 -> pyeong < 20
        AreaRange.PY_20S -> pyeong in 20..29
        AreaRange.PY_30S -> pyeong in 30..39
        AreaRange.OVER_40 -> pyeong >= 40
    }

    private fun relativeBearing(pose: VehiclePose, b: GeoPoint): Double {
        val a = pose.location
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val delta = Math.toRadians(b.longitude - a.longitude)
        val degrees = Math.toDegrees(atan2(sin(delta) * cos(lat2),
            cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(delta)))
        return (degrees - (pose.headingDeg ?: 0.0) + 540.0) % 360.0 - 180.0
    }

    private fun referenceLabel(source: LocationSource): String = when (source) {
        LocationSource.VEHICLE -> VEHICLE_REFERENCE_LABEL
        LocationSource.EMULATOR_ROUTE -> EMULATOR_REFERENCE_LABEL
    }

    private companion object {
        const val RADIUS_AREA_LABEL = "현재 위치 반경 10km"
        const val LOCATION_PENDING_LABEL = "현재 위치 확인 중"
        const val VEHICLE_REFERENCE_LABEL = "현재 차량 위치 · 반경 10km 실거래"
        const val EMULATOR_REFERENCE_LABEL = "테스트 위치 · 반경 10km 실거래"
        const val RADIUS_KM = 10
        const val TOP_LIMIT = 3
        const val MAX_MAP_PINS = 12
        const val MAX_EXTRA_GEOCODES = 18
        const val PAGE_SIZE = 1000
        const val MAX_CONCURRENT_REGIONS = 4
        const val MAX_ACCURACY_METERS = 100.0
        const val RERANK_DISTANCE_KM = 0.5
        const val RERANK_DEBOUNCE_MILLIS = 400L
        val MAX_FIX_AGE: Duration = Duration.ofSeconds(30)
        val MAX_FUTURE_FIX_OFFSET: Duration = Duration.ofSeconds(-5)
        val SUCCESS_TTL: Duration = Duration.ofSeconds(60)
        val RECENT_MONTH_TTL: Duration = Duration.ofMinutes(5)
        val FAILURE_TTL: Duration = Duration.ofSeconds(10)
    }
}
