package com.potatoes.book5.realestate.fakes

import com.potatoes.book5.realestate.domain.model.DriveLeaderChange
import com.potatoes.book5.realestate.domain.model.DriveRank
import com.potatoes.book5.realestate.domain.model.DriveSummary
import com.potatoes.book5.realestate.domain.model.PassedInterest
import com.potatoes.book5.realestate.domain.port.DriveRecordRepository
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** 메모리 주행 기록. Room 구현과 같은 계약을 따른다. */
class FakeDriveRecordRepository : DriveRecordRepository {

    data class Session(
        val id: Long,
        val startedAt: LocalDateTime,
        val endedAt: LocalDateTime?,
        val lastRecordedAt: LocalDateTime,
        val radiusKm: Int,
        val top: List<DriveRank> = emptyList(),
        val changes: List<DriveLeaderChange> = emptyList(),
        val passed: List<PassedInterest> = emptyList(),
        val seen: Boolean = false,
    )

    val sessions = MutableStateFlow<Map<Long, Session>>(emptyMap())
    private var nextId = 1L

    private fun edit(id: Long, block: (Session) -> Session) = sessions.update { m -> m[id]?.let { m + (id to block(it)) } ?: m }

    override suspend fun openSessionId(): Long? = sessions.value.values.filter { it.endedAt == null }.maxByOrNull { it.startedAt }?.id

    override suspend fun startSession(at: LocalDateTime, radiusKm: Int): Long {
        val id = nextId++
        sessions.update { it + (id to Session(id, at, null, at, radiusKm)) }
        return id
    }

    override suspend fun recordSnapshot(sessionId: Long, at: LocalDateTime, top: List<DriveRank>, passed: List<PassedInterest>) = edit(sessionId) { s ->
        val merged = s.passed.associateBy { it.apartmentId }.toMutableMap()
        passed.forEach { p -> merged[p.apartmentId] = merged[p.apartmentId]?.let { o -> p.copy(closestKm = minOf(o.closestKm, p.closestKm)) } ?: p }
        s.copy(top = top, passed = merged.values.toList(), lastRecordedAt = at)
    }

    override suspend fun recordLeaderChange(sessionId: Long, change: DriveLeaderChange) = edit(sessionId) { it.copy(changes = it.changes + change) }

    override suspend fun finishSession(sessionId: Long, endedAt: LocalDateTime?): Boolean {
        val s = sessions.value[sessionId] ?: return false
        if (s.top.isEmpty()) {
            sessions.update { it - sessionId }
            return false
        }
        edit(sessionId) { it.copy(endedAt = endedAt ?: it.lastRecordedAt) }
        return true
    }

    override suspend fun trim(keep: Int) = sessions.update { m ->
        val keepIds = m.values.filter { it.endedAt != null }.sortedByDescending { it.endedAt }.take(keep).map { it.id }.toSet()
        m.filter { (id, s) -> s.endedAt == null || id in keepIds }
    }

    override fun observeSummary(id: Long): Flow<DriveSummary?> = sessions.map { m -> m[id]?.takeIf { it.endedAt != null }?.toSummary() }

    override fun observeLatestId(): Flow<Long?> = sessions.map { m -> m.values.filter { it.endedAt != null }.maxByOrNull { it.endedAt!! }?.id }

    override fun observeLatestUnseenId(): Flow<Long?> =
        sessions.map { m -> m.values.filter { it.endedAt != null && !it.seen }.maxByOrNull { it.endedAt!! }?.id }

    override suspend fun markSeen(id: Long) = edit(id) { it.copy(seen = true) }

    override suspend fun deleteAll() = sessions.update { emptyMap() }

    private fun Session.toSummary() = DriveSummary(id, startedAt, endedAt!!, null, radiusKm, top, changes, passed, seen)
}
