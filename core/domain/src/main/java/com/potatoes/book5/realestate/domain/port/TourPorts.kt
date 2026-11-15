package com.potatoes.book5.realestate.domain.port

import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.*
import com.potatoes.book5.realestate.domain.model.appaction.*
import com.potatoes.book5.realestate.domain.model.tour.*
import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface TourStore {
    fun observeAreas(profile: String): Flow<List<SavedTourArea>>
    suspend fun areas(profile: String): Outcome<List<SavedTourArea>>
    suspend fun ensureArea(profile: String, area: TourArea, at: Instant, allowed: () -> Boolean): Outcome<AreaSaveResult>
    suspend fun deleteArea(profile: String, areaId: String): Outcome<Unit>
    suspend fun deleteAll()
    suspend fun publishSummary(profile: String, areaId: String, prices: PreparedDistrictPrices, observedAt: Instant): Outcome<Boolean>
    suspend fun unread(profile: String, limit: Int = 3): Outcome<List<TourBriefing>>
    suspend fun acknowledge(profile: String, receipts: List<BriefingReceipt>, allowed: () -> Boolean = { true }): Outcome<Int>
    suspend fun publishNotice(profile: String,areaId: String,notice: TourNotice,at: Instant, allowed: () -> Boolean = { true }): Outcome<Boolean>
    suspend fun unreadNotices(profile: String): Outcome<List<StoredTourNotice>>
    suspend fun sourceStates(profile: String): Outcome<List<TourSourceState>>
    suspend fun setSourceState(profile: String,state: TourSourceState, allowed: () -> Boolean = { true }): Outcome<Unit>
    suspend fun latestEntry(profile: String, areaId: String): Outcome<AreaEntry?>
    suspend fun recordEntry(entry: AreaEntry, allowed: () -> Boolean = { true }): Outcome<Unit>
}
interface TourAreaResolver {
    /** Cache/Room only; a miss may enqueue preparation, but never waits for network. */
    suspend fun resolve(query: String?, context: AppCallContext, preferCurrent: Boolean = false): Outcome<ActionResult<TourArea>>
}
interface TourMarketReader {
    fun readMarket(districtCode: String): Outcome<TourMarket>
    fun readAnalytics(apartmentId: String): Outcome<ApartmentDetail>
}
interface TourEntryEvidence { fun current(profile: String, areaId: String, session: String): AreaEntry? }
interface TourNavigator {
    val events: kotlinx.coroutines.flow.Flow<NavigationEvent> get() = kotlinx.coroutines.flow.emptyFlow()
    fun supported(): Boolean
    /** null means unknown; replacement then needs explicit confirmation. */
    fun activeRoute(): Boolean?
    fun pending(): Boolean = false
    suspend fun request(entrance: VerifiedEntrance, requestId: String, allowed: () -> Boolean, eventAllowed: () -> Boolean = allowed): Outcome<NavigationReceipt>
}
interface TourCamera {
    fun supported(): Boolean
    suspend fun scan(record: Boolean): Outcome<Unit>
}

interface TourNoticeSource {
    val id: String
    val configured: Boolean
    suspend fun fetch(): Outcome<List<com.potatoes.book5.realestate.domain.model.tour.TourNotice>>
}
