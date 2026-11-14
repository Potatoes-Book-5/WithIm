package com.potatoes.book5.realestate.data.local

import androidx.room.withTransaction
import com.potatoes.book5.realestate.data.local.db.AppDatabase
import com.potatoes.book5.realestate.data.local.db.DriveLeaderChangeEntity
import com.potatoes.book5.realestate.data.local.db.DrivePassedInterestEntity
import com.potatoes.book5.realestate.data.local.db.DriveRankEntity
import com.potatoes.book5.realestate.data.local.db.DriveSessionEntity
import com.potatoes.book5.realestate.domain.model.DriveLeaderChange
import com.potatoes.book5.realestate.domain.model.DriveRank
import com.potatoes.book5.realestate.domain.model.DriveSummary
import com.potatoes.book5.realestate.domain.model.PassedInterest
import com.potatoes.book5.realestate.domain.port.DriveRecordRepository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** 주행 기록을 Room에 저장한다(F-009, ADR-005). 서버로 보내지 않는다. */
class RoomDriveRecordRepository(
    private val db: AppDatabase,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : DriveRecordRepository {

    private val dao = db.driveDao()

    override suspend fun openSessionId(): Long? = dao.openSessionId()

    override suspend fun startSession(at: LocalDateTime, radiusKm: Int): Long = dao.insertSession(
        DriveSessionEntity(startedAt = at.millis(), endedAt = null, lastRecordedAt = at.millis(), radiusKm = radiusKm, distanceKm = null, seen = false)
    )

    override suspend fun recordSnapshot(sessionId: Long, at: LocalDateTime, top: List<DriveRank>, passed: List<PassedInterest>) {
        db.withTransaction {
            dao.replaceRanks(sessionId, top.map { DriveRankEntity(sessionId, it.rank, it.apartmentId, it.name, it.priceManwon) })
            passed.forEach { p ->
                val old = dao.passed(sessionId, p.apartmentId)
                dao.upsertPassed(
                    DrivePassedInterestEntity(
                        sessionId = sessionId,
                        apartmentId = p.apartmentId,
                        name = p.name,
                        closestKm = minOf(p.closestKm, old?.closestKm ?: p.closestKm),
                        priceManwon = p.priceManwon,
                        firstSeenAt = old?.firstSeenAt ?: at.millis(),
                    )
                )
            }
            dao.touch(sessionId, at.millis())
        }
    }

    override suspend fun recordLeaderChange(sessionId: Long, change: DriveLeaderChange) {
        dao.insertLeaderChange(DriveLeaderChangeEntity(sessionId = sessionId, at = change.at.millis(), apartmentId = change.apartmentId, name = change.name))
    }

    override suspend fun finishSession(sessionId: Long, endedAt: LocalDateTime?): Boolean = db.withTransaction {
        val session = dao.session(sessionId) ?: return@withTransaction false
        if (dao.rankCount(sessionId) == 0) {
            dao.deleteSession(sessionId)
            return@withTransaction false
        }
        dao.end(sessionId, endedAt?.millis() ?: session.lastRecordedAt)
        true
    }

    override suspend fun trim(keep: Int) = dao.trim(keep)

    override fun observeSummary(id: Long): Flow<DriveSummary?> = combine(
        dao.observeSession(id),
        dao.observeRanks(id),
        dao.observeLeaderChanges(id),
        dao.observePassed(id),
    ) { session, ranks, changes, passed ->
        session ?: return@combine null
        DriveSummary(
            id = session.id,
            startedAt = session.startedAt.dateTime(),
            endedAt = (session.endedAt ?: session.lastRecordedAt).dateTime(),
            distanceKm = session.distanceKm,
            radiusKm = session.radiusKm,
            finalTop = ranks.map { DriveRank(it.rank, it.apartmentId, it.name, it.priceManwon) },
            leaderChanges = changes.map { DriveLeaderChange(it.at.dateTime(), it.apartmentId, it.name) },
            passedInterests = passed.map { PassedInterest(it.apartmentId, it.name, it.closestKm, it.priceManwon) },
            seen = session.seen,
        )
    }

    override fun observeLatestId(): Flow<Long?> = dao.observeLatestId()

    override fun observeLatestUnseenId(): Flow<Long?> = dao.observeLatestUnseenId()

    override suspend fun markSeen(id: Long) = dao.markSeen(id)

    override suspend fun deleteAll() = dao.deleteAll()

    private fun LocalDateTime.millis(): Long = atZone(zone).toInstant().toEpochMilli()

    private fun Long.dateTime(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), zone)
}
