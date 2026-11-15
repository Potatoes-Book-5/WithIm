package com.potatoes.book5.realestate.data.local

import androidx.room.withTransaction
import com.potatoes.book5.realestate.data.local.db.*
import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.*
import com.potatoes.book5.realestate.domain.model.tour.*
import com.potatoes.book5.realestate.domain.port.TourStore
import java.security.MessageDigest
import java.time.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map

class RoomTourStore(private val database: AppDatabase) : TourStore {
    private val dao=database.tourDao()
    private class Invalidated : RuntimeException()
    private suspend fun <T> result(block: suspend ()->T): Outcome<T> = try { Outcome.Success(block()) }
        catch(e:CancellationException) { throw e }
        catch(_:Invalidated) { Outcome.Failure(DomainError.Invalid("context_invalidated")) }
        catch(_:IllegalArgumentException) { Outcome.Failure(DomainError.Invalid("invalid_area_or_summary")) }
        catch(_:Exception) { Outcome.Failure(DomainError.InvalidResponse) }
    override fun observeAreas(profile: String)=dao.observeAreas(profile).map { rows -> rows.map { it.domain() } }
    override suspend fun areas(profile: String)=result { dao.areas(profile).map { it.domain() } }
    override suspend fun ensureArea(profile: String,area: TourArea,at: Instant,allowed: ()->Boolean): Outcome<AreaSaveResult> = result {
        database.withTransaction {
            if(!allowed()) throw Invalidated()
            require(profile.isNotBlank() && area.id.isNotBlank() && area.name.isNotBlank() && area.sourceId.isNotBlank() && area.districtCode.matches(Regex("[0-9]{5}")))
            val old=dao.area(profile,area.id)
            if(old!=null) return@withTransaction AreaSaveResult(old.domain(),true)
            require(dao.areas(profile).size<100)
            val circle=area.boundary as? AreaBoundary.Circle
            if(circle!=null) require(circle.radiusMeters==500.0 && circle.center.latitude.isFinite() && circle.center.longitude.isFinite() &&
                circle.center.latitude in -90.0..90.0 && circle.center.longitude in -180.0..180.0)
            dao.insertArea(TourAreaEntity(profile,area.id,area.name,area.districtCode,if(circle==null) "DISTRICT" else "CIRCLE",
                circle?.center?.latitude,circle?.center?.longitude,circle?.radiusMeters,area.sourceId,area.sourceAsOf.toEpochMilli(),at.toEpochMilli(),java.util.UUID.randomUUID().toString(),area.districtName))
            if(!allowed()) throw Invalidated()
            AreaSaveResult(requireNotNull(dao.area(profile,area.id)).domain(),false)
        }
    }
    override suspend fun deleteArea(profile: String,areaId: String)=result { dao.deleteArea(profile,areaId) }
    override suspend fun deleteAll() { database.withTransaction { dao.deleteAll();dao.deleteSources() } }
    override suspend fun publishSummary(profile: String,areaId: String,prices: PreparedDistrictPrices,observedAt: Instant)=result {
        database.withTransaction {
            val area=dao.area(profile,areaId) ?: return@withTransaction false
            require(area.districtCode==prices.districtCode && prices.sampleCount>=0 && prices.sourceAsOf<=observedAt)
            require(if(prices.sampleCount==0) prices.averagePriceKrw==null && prices.highestPriceKrw==null
                else (prices.averagePriceKrw ?: 0)>0 && (prices.highestPriceKrw ?: 0)>=(prices.averagePriceKrw ?: 0))
            val old=dao.summary(profile,areaId)
            if(old!=null && old.sourceAsOf>prices.sourceAsOf.toEpochMilli()) return@withTransaction false
            val content=listOf(prices.districtCode,prices.periodStart,prices.periodEndInclusive,prices.areaMinM2,
                prices.areaMaxExclusiveM2,prices.sampleCount,prices.averagePriceKrw,prices.highestPriceKrw,prices.highestComplexId).joinToString("|")
            val version=MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
            val changed=old?.version!=version
            dao.upsertSummary(TourBriefingEntity(profile,areaId,version,prices.averagePriceKrw,prices.highestPriceKrw,prices.sampleCount,
                prices.periodStart.toEpochDay(),prices.periodEndInclusive.toEpochDay(),prices.sourceAsOf.toEpochMilli(),
                if(changed) observedAt.toEpochMilli() else old!!.observedAt,old?.readVersion))
            changed
        }
    }
    override suspend fun unread(profile: String,limit: Int)=result {
        require(limit in 1..3)
        dao.unread(profile,limit).map { e -> TourBriefing(e.areaId,requireNotNull(dao.area(profile,e.areaId)).name,e.version,e.averageKrw,e.highestKrw,
            e.sampleCount,LocalDate.ofEpochDay(e.periodStart),LocalDate.ofEpochDay(e.periodEnd),Instant.ofEpochMilli(e.sourceAsOf),Instant.ofEpochMilli(e.observedAt),e.readVersion) }
    }
    override suspend fun acknowledge(profile: String,receipts: List<BriefingReceipt>,allowed: ()->Boolean)=result {
        database.withTransaction {
            if(!allowed()) throw Invalidated()
            val count=receipts.distinct().sumOf { if(it.itemId.startsWith("notice:")) (dao.markNoticeRead(profile,it.itemId,it.version)>0).compareTo(false) else dao.markRead(profile,it.itemId.removePrefix("price-summary:"),it.version) }
            if(!allowed()) throw Invalidated()
            count
        }
    }
    override suspend fun publishNotice(profile:String,areaId:String,notice:TourNotice,at:Instant,allowed:()->Boolean)=result {
        database.withTransaction {
            if(!allowed()) throw Invalidated()
            require(notice.id.startsWith("notice:") && notice.title.isNotBlank() && notice.title.length<=500 && notice.url.startsWith("https://"))
            if(dao.area(profile,areaId)==null) return@withTransaction false
            val version=MessageDigest.getInstance("SHA-256").digest(listOf(notice.source,notice.type,notice.title,notice.url,notice.publishedAt,notice.publishedDate,notice.address,notice.scope).joinToString("|").toByteArray()).joinToString("") { "%02x".format(it) }
            val old=dao.notice(profile,notice.id)
            if(old!=null && old.collectedAt>at.toEpochMilli()) return@withTransaction false
            val changed=old?.version!=version
            if(changed) dao.removeNotice(profile,notice.id)
            dao.putNotice(TourNoticeEntity(profile,areaId,notice.id,notice.source,notice.type,notice.title,notice.url,notice.publishedAt?.toEpochMilli(),notice.publishedDate?.toEpochDay(),notice.address,notice.scope,
                version,at.toEpochMilli(),if(changed) at.toEpochMilli() else old!!.observedAt,if(changed) null else old?.readVersion))
            dao.trimNotices(profile,areaId)
            if(!allowed()) throw Invalidated()
            changed
        }
    }
    override suspend fun unreadNotices(profile:String)=result { dao.unreadNotices(profile).map { e -> StoredTourNotice(
        TourNotice(e.noticeId,e.source,e.type,e.title,e.url,e.publishedAt?.let(Instant::ofEpochMilli),e.address,e.scope,e.publishedDate?.let(java.time.LocalDate::ofEpochDay)),e.version,Instant.ofEpochMilli(e.observedAt),e.readVersion) } }
    override suspend fun sourceStates(profile:String)=result { dao.sources(profile).map { TourSourceState(it.source,it.state,Instant.ofEpochMilli(it.attemptedAt)) } }
    override suspend fun setSourceState(profile:String,state:TourSourceState,allowed:()->Boolean)=result {
        database.withTransaction {
            if(!allowed()) throw Invalidated()
            dao.putSource(TourSourceEntity(profile,state.source,state.state,state.attemptedAt.toEpochMilli()))
            if(!allowed()) throw Invalidated()
        }
    }
    override suspend fun latestEntry(profile: String,areaId: String)=result {
        dao.latestEntry(profile,areaId)?.let { AreaEntry(it.profile,it.areaId,it.session,it.visit,Instant.ofEpochMilli(it.at),it.contextRevision,it.areaGeneration) }
    }
    override suspend fun recordEntry(entry: AreaEntry,allowed: ()->Boolean)=result {
        database.withTransaction {
            if(!allowed()) throw Invalidated()
            val parent=dao.area(entry.profile,entry.areaId) ?: throw Invalidated()
            if(parent.generation!=entry.areaGeneration) throw Invalidated()
            run {
                dao.insertEntry(TourEntryEntity(entry.profile,entry.areaId,entry.session,entry.visit,entry.at.toEpochMilli(),entry.contextRevision,entry.areaGeneration))
                if(!allowed()) throw Invalidated()
            }
        }
    }
}

private fun TourAreaEntity.domain(): SavedTourArea {
    val boundary=when(shape) {
        "DISTRICT" -> AreaBoundary.District(districtCode)
        "CIRCLE" -> AreaBoundary.Circle(GeoPoint(requireNotNull(latitude),requireNotNull(longitude)),requireNotNull(radiusMeters))
        else -> throw IllegalArgumentException("invalid_area_shape")
    }
    return SavedTourArea(profile,TourArea(areaId,name,districtCode,boundary,sourceId,Instant.ofEpochMilli(sourceAsOf),districtName),Instant.ofEpochMilli(savedAt),generation)
}
