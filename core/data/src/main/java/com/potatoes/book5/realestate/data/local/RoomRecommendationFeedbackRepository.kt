package com.potatoes.book5.realestate.data.local

import com.potatoes.book5.realestate.data.local.db.RecommendationFeedbackDao
import com.potatoes.book5.realestate.data.local.db.RecommendationFeedbackEntity
import com.potatoes.book5.realestate.domain.model.DriveRecommendation
import com.potatoes.book5.realestate.domain.model.RecommendationFeedback
import com.potatoes.book5.realestate.domain.port.RecommendationFeedbackRepository

/** Stores only a capped per-apartment aggregate on device (T-047, SP-004). */
class RoomRecommendationFeedbackRepository(
    private val dao: RecommendationFeedbackDao,
) : RecommendationFeedbackRepository {

    override suspend fun getAll(): List<RecommendationFeedback> = dao.getAll().map { it.toDomain() }

    override suspend fun recordDetailOpen(recommendation: DriveRecommendation) {
        dao.recordDetailOpen(
            RecommendationFeedbackEntity(
                apartmentId = recommendation.apartmentId,
                priceManwon = recommendation.priceManwon,
                areaPyeong = recommendation.areaPyeong,
                neighborhoodName = recommendation.neighborhoodName,
                households = recommendation.households,
                builtYear = recommendation.builtYear,
                detailOpenCount = 0,
            ),
        )
    }

    override suspend fun deleteAll() = dao.deleteAll()

    private fun RecommendationFeedbackEntity.toDomain() = RecommendationFeedback(
        apartmentId = apartmentId,
        priceManwon = priceManwon,
        areaPyeong = areaPyeong,
        neighborhoodName = neighborhoodName,
        households = households,
        builtYear = builtYear,
        detailOpenCount = detailOpenCount,
    )

}
