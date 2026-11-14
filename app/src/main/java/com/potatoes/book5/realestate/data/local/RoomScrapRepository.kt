package com.potatoes.book5.realestate.data.local

import com.potatoes.book5.realestate.data.local.db.ScrapDao
import com.potatoes.book5.realestate.data.local.db.ScrapEntity
import com.potatoes.book5.realestate.domain.model.PriceChange
import com.potatoes.book5.realestate.domain.model.PriceRecord
import com.potatoes.book5.realestate.domain.model.Scrap
import com.potatoes.book5.realestate.domain.model.ScrapChangeKind
import com.potatoes.book5.realestate.domain.port.ScrapRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room에 스크랩을 저장한다(ADR-005). Room 타입은 이 패키지 밖으로 나가지 않는다. */
class RoomScrapRepository(
    private val dao: ScrapDao,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ScrapRepository {

    override fun observeAll(): Flow<List<Scrap>> = dao.observeAll().map { list -> list.map { it.toDomain(zone) } }

    override fun observeIsScrapped(apartmentId: String): Flow<Boolean> = dao.observeExists(apartmentId)

    override suspend fun getAll(): List<Scrap> = dao.getAll().map { it.toDomain(zone) }

    override suspend fun get(apartmentId: String): Scrap? = dao.get(apartmentId)?.toDomain(zone)

    override suspend fun upsert(scrap: Scrap) = dao.upsert(scrap.toEntity(zone))

    override suspend fun deleteAll() = dao.deleteAll()

    override suspend fun delete(apartmentId: String) = dao.delete(apartmentId)

    override suspend fun markChangeSeen(apartmentId: String) = dao.markChangeSeen(apartmentId)
}

internal fun ScrapEntity.toDomain(zone: ZoneId): Scrap = Scrap(
    apartmentId = apartmentId,
    name = name,
    neighborhood = neighborhood,
    areaPyeong = areaPyeong,
    scrappedAt = scrappedAt.toDateTime(zone),
    baseline = PriceRecord(baselinePriceManwon, LocalDate.ofEpochDay(baselineDealDate)),
    last = PriceRecord(lastPriceManwon, LocalDate.ofEpochDay(lastDealDate)),
    lastCheckedAt = lastCheckedAt.toDateTime(zone),
    change = changeKind
        ?.let { kind -> ScrapChangeKind.entries.firstOrNull { it.name == kind } }
        ?.let { kind ->
            PriceChange(
                kind = kind,
                deltaManwon = changeDeltaManwon ?: 0,
                detectedAt = (changeDetectedAt ?: lastCheckedAt).toDateTime(zone),
                seen = changeSeen,
            )
        },
    households = households,
    builtYear = builtYear,
)

internal fun Scrap.toEntity(zone: ZoneId): ScrapEntity = ScrapEntity(
    apartmentId = apartmentId,
    name = name,
    neighborhood = neighborhood,
    areaPyeong = areaPyeong,
    scrappedAt = scrappedAt.toEpochMilli(zone),
    baselinePriceManwon = baseline.priceManwon,
    baselineDealDate = baseline.dealDate.toEpochDay(),
    lastPriceManwon = last.priceManwon,
    lastDealDate = last.dealDate.toEpochDay(),
    lastCheckedAt = lastCheckedAt.toEpochMilli(zone),
    changeKind = change?.kind?.name,
    changeDeltaManwon = change?.deltaManwon,
    changeDetectedAt = change?.detectedAt?.toEpochMilli(zone),
    changeSeen = change?.seen ?: true,
    households = households,
    builtYear = builtYear,
)

private fun Long.toDateTime(zone: ZoneId): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), zone)

private fun LocalDateTime.toEpochMilli(zone: ZoneId): Long = atZone(zone).toInstant().toEpochMilli()
