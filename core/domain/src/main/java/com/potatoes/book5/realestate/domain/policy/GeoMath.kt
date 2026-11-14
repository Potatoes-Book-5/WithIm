package com.potatoes.book5.realestate.domain.policy

import com.potatoes.book5.realestate.domain.model.GeoPoint
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** 구면 근사 좌표 계산. 반경 10km 안의 표시용이라 구면 오차는 무시한다. */
object GeoMath {
    private const val EARTH_RADIUS_KM = 6371.0

    /** [origin]에서 북쪽 기준 방위 [bearingDeg]로 [distanceKm]만큼 간 지점. */
    fun destination(origin: GeoPoint, distanceKm: Double, bearingDeg: Double): GeoPoint {
        val d = distanceKm / EARTH_RADIUS_KM
        val brg = Math.toRadians(bearingDeg)
        val lat1 = Math.toRadians(origin.latitude)
        val lon1 = Math.toRadians(origin.longitude)
        val lat2 = asin(sin(lat1) * cos(d) + cos(lat1) * sin(d) * cos(brg))
        val lon2 = lon1 + atan2(sin(brg) * sin(d) * cos(lat1), cos(d) - sin(lat1) * sin(lat2))
        return GeoPoint(Math.toDegrees(lat2), Math.toDegrees(lon2))
    }

    /** 두 지점 사이 대원 거리(km). */
    fun distanceKm(a: GeoPoint, b: GeoPoint): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_KM * asin(kotlin.math.sqrt(h))
    }
}
