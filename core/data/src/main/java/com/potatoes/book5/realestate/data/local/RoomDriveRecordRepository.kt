package com.potatoes.book5.realestate.data.local

import androidx.room.withTransaction
import com.potatoes.book5.realestate.data.local.db.AppDatabase
import com.potatoes.book5.realestate.data.local.db.DriveLeaderChangeEntity
import com.potatoes.book5.realestate.data.local.db.DrivePassedInterestEntity
import com.potatoes.book5.realestate.data.local.db.DriveRecommendationEntity
import com.potatoes.book5.realestate.data.local.db.DriveRankEntity
import com.potatoes.book5.realestate.data.local.db.DriveSessionEntity
import com.potatoes.book5.realestate.data.local.db.DriveRegionRoundEntity
import com.potatoes.book5.realestate.data.local.db.DriveRegionRankEntity
import com.potatoes.book5.realestate.domain.model.DriveRegionRound
import com.potatoes.book5.realestate.domain.model.MarketRound
import com.potatoes.book5.realestate.domain.model.LeaderEventKind
import com.potatoes.book5.realestate.domain.model.DriveLeaderChange
import com.potatoes.book5.realestate.domain.model.DriveRank
import com.potatoes.book5.realestate.domain.model.DriveRecommendation
import com.potatoes.book5.realestate.domain.model.RecommendationKey
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

    override suspend fun startSession(at: LocalDateTime, radiusKm: Int): Long = db.withTransaction {
        ensureOpenSession(at, radiusKm)
    }

    override suspend fun recordSnapshot(
        sessionId: Long,
        at: LocalDateTime,
        top: List<DriveRank>,
        passed: List<PassedInterest>,
        recommendations: List<DriveRecommendation>,
    ) {
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
            recommendations.forEach { item ->
                val old = dao.recommendation(sessionId, item.apartmentId)
                dao.upsertRecommendation(
                    DriveRecommendationEntity(
                        sessionId = sessionId,
                        apartmentId = item.apartmentId,
                        name = item.name,
                        priceManwon = item.priceManwon,
                        areaPyeong = item.areaPyeong,
                        scorePercent = item.scorePercent,
                        closestKm = minOf(item.closestKm, old?.closestKm ?: item.closestKm),
                        mainRecoKey = item.mainRecoKey.name,
                        neighborhoodName = item.neighborhoodName,
                        households = item.households,
                        builtYear = item.builtYear,
                    ),
                )
            }
            dao.touch(sessionId, at.millis())
        }
    }

    override suspend fun recordAlertedInterest(at: LocalDateTime, radiusKm: Int, interest: PassedInterest) {
        db.withTransaction {
            val sessionId = ensureOpenSession(at, radiusKm)
            val old = dao.passed(sessionId, interest.apartmentId)
            dao.upsertPassed(
                DrivePassedInterestEntity(
                    sessionId = sessionId,
                    apartmentId = interest.apartmentId,
                    name = interest.name,
                    closestKm = minOf(interest.closestKm, old?.closestKm ?: interest.closestKm),
                    priceManwon = interest.priceManwon,
                    firstSeenAt = old?.firstSeenAt ?: at.millis(),
                ),
            )
            dao.touch(sessionId, at.millis())
        }
    }

    override suspend fun recordLeaderChange(sessionId: Long, change: DriveLeaderChange) {
        dao.insertLeaderChange(DriveLeaderChangeEntity(sessionId = sessionId, at = change.at.millis(),
            apartmentId = change.apartmentId, name = change.name, kind = change.kind.name, districtName = change.districtName))
    }

    override suspend fun recordRound(sessionId: Long, at: LocalDateTime, round: MarketRound, top: List<DriveRank>) {
        db.withTransaction {
            dao.closeRounds(sessionId, at.millis(), round.id)
            if (dao.round(sessionId, round.id) == null) {
                dao.insertRound(DriveRegionRoundEntity(sessionId, round.id, round.districtCode, round.districtName, at.millis()))
            }
            val old = dao.roundRanks(sessionId, round.id).map { DriveRank(it.rank, it.apartmentId, it.name, it.priceManwon) }
            val best = (old + top).groupBy { it.apartmentId }.values.map { rows -> rows.maxBy { it.priceManwon } }
                .sortedWith(compareByDescending<DriveRank> { it.priceManwon }.thenBy { it.apartmentId }).take(3)
            dao.clearRoundRanks(sessionId, round.id)
            dao.insertRoundRanks(best.mapIndexed { index, rank ->
                DriveRegionRankEntity(sessionId, round.id, index + 1, rank.apartmentId, rank.name, rank.priceManwon)
            })
            dao.touch(sessionId, at.millis())
        }
    }

    override suspend fun finishSession(sessionId: Long, endedAt: LocalDateTime?): Boolean = db.withTransaction {
        val session = dao.session(sessionId) ?: return@withTransaction false
        if (dao.rankCount(sessionId) == 0 && dao.roundRankCount(sessionId) == 0 &&
            dao.recommendationCount(sessionId) == 0 && dao.passedCount(sessionId) == 0) {
            dao.deleteSession(sessionId)
            return@withTransaction false
        }
        val end = endedAt?.millis() ?: session.lastRecordedAt
        dao.closeRounds(sessionId, end)
        dao.end(sessionId, end)
        true
    }

    override suspend fun trim(keep: Int) = dao.trim(keep)

    override fun observeSummary(id: Long): Flow<DriveSummary?> = combine(
        dao.observeSession(id),
        dao.observeRanks(id),
        dao.observeLeaderChanges(id),
        dao.observePassed(id),
        combine(dao.observeRounds(id), dao.observeRoundRanks(id), dao.observeRecommendations(id)) { rounds, ranks, recommendations ->
            (rounds to ranks) to recommendations
        },
    ) { session, ranks, changes, passed, roundData ->
        session ?: return@combine null
        DriveSummary(
            id = session.id,
            startedAt = session.startedAt.dateTime(),
            endedAt = (session.endedAt ?: session.lastRecordedAt).dateTime(),
            distanceKm = session.distanceKm,
            radiusKm = session.radiusKm,
            finalTop = ranks.map { DriveRank(it.rank, it.apartmentId, it.name, it.priceManwon) },
            leaderChanges = changes.map { change ->
                DriveLeaderChange(change.at.dateTime(), change.apartmentId, change.name,
                    LeaderEventKind.entries.find { it.name == change.kind } ?: LeaderEventKind.REPLACED, change.districtName)
            },
            passedInterests = passed.map { PassedInterest(it.apartmentId, it.name, it.closestKm, it.priceManwon) },
            seen = session.seen,
            rounds = roundData.first.first.map { round ->
                DriveRegionRound(MarketRound(round.roundId, round.districtCode, round.districtName),
                    round.startedAt.dateTime(), round.endedAt?.dateTime(),
                    roundData.first.second.filter { it.roundId == round.roundId }.map {
                        DriveRank(it.rank, it.apartmentId, it.name, it.priceManwon)
                    })
            },
            recommendations = roundData.second.mapNotNull { item ->
                val key = RecommendationKey.entries.firstOrNull { it.name == item.mainRecoKey } ?: return@mapNotNull null
                DriveRecommendation(item.apartmentId, item.name, item.priceManwon, item.areaPyeong,
                    item.scorePercent, item.closestKm, key, item.neighborhoodName, item.households, item.builtYear)
            },
        )
    }

    override fun observeLatestId(): Flow<Long?> = dao.observeLatestId()

    override fun observeLatestUnseenId(): Flow<Long?> = dao.observeLatestUnseenId()

    override suspend fun markSeen(id: Long) = dao.markSeen(id)

    override suspend fun deleteAll() = dao.deleteAll()

    private fun LocalDateTime.millis(): Long = atZone(zone).toInstant().toEpochMilli()

    /** Reuse the recorder's session if an alert wins the race to create it. */
    private suspend fun ensureOpenSession(at: LocalDateTime, radiusKm: Int): Long =
        dao.openSessionId() ?: dao.insertSession(
            DriveSessionEntity(
                startedAt = at.millis(), endedAt = null, lastRecordedAt = at.millis(),
                radiusKm = radiusKm, distanceKm = null, seen = false,
            ),
        )

    private fun Long.dateTime(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), zone)
}
