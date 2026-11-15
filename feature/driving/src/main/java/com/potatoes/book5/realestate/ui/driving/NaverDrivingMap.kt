package com.potatoes.book5.realestate.ui.driving

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Typeface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.NaverMapOptions
import com.naver.maps.map.Projection
import com.naver.maps.map.overlay.CircleOverlay
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.overlay.OverlayImage
import com.potatoes.book5.realestate.feature.driving.R
import com.potatoes.book5.realestate.domain.model.GeoPoint
import com.potatoes.book5.realestate.domain.model.VehiclePose
import com.potatoes.book5.realestate.ui.common.MapPointUi
import com.potatoes.book5.realestate.ui.common.RankItemUi
import com.potatoes.book5.realestate.ui.theme.wpx
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.sin

/**
 * 차량 주변에 담을 반경 1km(SCR-009). 카메라 줌은 이 값과 지도 크기로 계산한다.
 * 순위 조회 반경(3km)이나 순위 핀 위치로 카메라를 넓히지 않는다.
 */
private const val VISIBLE_RADIUS_METERS = 1_000.0

/** 거리 원: 안쪽 500m 회색 눈금, 바깥 1km 강조색(화면 가장자리). */
private const val INNER_RING_METERS = 500.0
private const val OUTER_RING_METERS = VISIBLE_RADIUS_METERS

/** 차량을 화면 아래쪽 약 3/4 지점에 두기 위한 위쪽 콘텐츠 패딩 비율. (패딩 + 높이) / 2 = 0.74 × 높이 */
private const val TOP_PADDING_RATIO = 0.48f

private val OtherDot = Color(0xFFA9AFBA)

/**
 * SCR-009 주행 지도. 차량 위치가 없으면 임시 조회 기준을 중심으로 표시한다.
 */
@Composable
fun DrivingMap(state: DrivingScreenState, naverMapAvailable: Boolean, modifier: Modifier = Modifier) {
    val vehicle = state.vehicle
    val center = state.mapCenter ?: vehicle
    if (center == null || !naverMapAvailable) {
        Surface(modifier = modifier, shape = RoundedCornerShape(28.wpx), color = MaterialTheme.colorScheme.surfaceVariant) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (center == null) state.referenceLabel ?: stringResource(R.string.driving_location_waiting)
                        else stringResource(R.string.driving_map_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }
    val rankedIds = state.mapRanking.mapTo(mutableSetOf()) { it.id }
    NaverDrivingMap(center, vehicle, state.mapRanking,
        state.interestMatches.filterNot { it.id in rankedIds },
        state.others.filterNot { it.id in rankedIds }, modifier)
}

/**
 * 네이버 지도 위에 순위 핀, 관심 매물, 그 밖의 단지, 거리 원을 그린다.
 * 차량 마커는 확인된 차량 위치가 있을 때만 그린다.
 * 진행 방향이 확인되면 그 방향을 위로, 없으면 북쪽을 위로 둔다. 주행 중 조작을 막기 위해 모든 제스처와 컨트롤을 끈다. 카메라는 애니메이션 없이 바꾼다(DD-005).
 */
@Composable
private fun NaverDrivingMap(
    center: VehiclePose,
    vehicle: VehiclePose?,
    ranking: List<RankItemUi>,
    interestMatches: List<MapPointUi>,
    others: List<MapPointUi>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val density = LocalDensity.current
    val colors = MarkerColors(
        accent = MaterialTheme.colorScheme.primary.toArgb(),
        ink = MaterialTheme.colorScheme.onSurface.toArgb(),
        ground = MaterialTheme.colorScheme.background.toArgb(),
        dot = OtherDot.toArgb(),
    )
    val sizes = with(density) {
        MarkerSizes(
            pin = 68.wpx.toPx(),
            crown = 44.wpx.toPx(),
            vehicle = 140.wpx.toPx(),
            dot = 24.wpx.toPx(),
            stroke = 4.wpx.toPx(),
        )
    }
    val images = remember(colors, sizes) { MarkerImages(colors, sizes) }

    val mapView = remember {
        MapView(context, NaverMapOptions().useTextureView(true).apply { applyDrivingOptions() })
    }
    var naverMap by remember { mutableStateOf<NaverMap?>(null) }

    DisposableEffect(lifecycle, mapView) {
        mapView.onCreate(null)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        mapView.getMapAsync { map ->
            map.applyDrivingSettings()
            naverMap = map
        }
        onDispose {
            lifecycle.removeObserver(observer)
            val current = lifecycle.currentState
            if (current.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (current.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
            naverMap = null
        }
    }

    BoxWithConstraints(modifier.clip(RoundedCornerShape(28.wpx))) {
        val widthPx = constraints.maxWidth
        val heightPx = constraints.maxHeight
        // 차량에서 위쪽(높이의 0.74)과 좌우(너비의 0.5) 중 짧은 쪽에 반경이 들어오게 한다.
        val visibleDp = with(density) { min(widthPx * 0.5f, heightPx * 0.74f).toDp().value }
        val cameraZoom = zoomFor(Projection.getMetersPerDp(center.location.latitude, 0.0), visibleDp, VISIBLE_RADIUS_METERS)

        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        val map = naverMap
        if (map != null) {
            DisposableEffect(map, center, widthPx, heightPx, cameraZoom) {
                map.setContentPadding(0, (heightPx * TOP_PADDING_RATIO).toInt(), 0, 0)
                map.moveCamera(
                    CameraUpdate.toCameraPosition(
                        CameraPosition(center.location.toLatLng(), cameraZoom, 0.0, center.headingDeg ?: 0.0),
                    ).animate(CameraAnimation.None).finishCallback {
                        if (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                            // Verify the applied camera, without logging any coordinates.
                            android.util.Log.d("DrivingMapCamera", "Applied zoom=" + map.cameraPosition.zoom)
                        }
                    },
                )
                onDispose { }
            }
            DisposableEffect(map, center, vehicle, ranking, interestMatches, others, images) {
                val overlays = buildOverlays(center, vehicle, ranking, interestMatches, others, images, colors, sizes)
                overlays.forEach { it.map = map }
                onDispose { overlays.forEach { it.map = null } }
            }
        }

        // 지도 글자와 겹쳐도 읽히도록 배경을 둔다.
        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(24.wpx),
            shape = RoundedCornerShape(16.wpx),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
        ) {
            Text(
                text = stringResource(if (center.headingDeg == null) R.string.driving_north_up else R.string.driving_heading_up),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.wpx, vertical = 8.wpx),
            )
        }
    }
}

private fun NaverMapOptions.applyDrivingOptions() {
    scrollGesturesEnabled(false)
    zoomGesturesEnabled(false)
    tiltGesturesEnabled(false)
    rotateGesturesEnabled(false)
    stopGesturesEnabled(false)
    zoomControlEnabled(false)
    compassEnabled(false)
    scaleBarEnabled(false)
    locationButtonEnabled(false)
    indoorLevelPickerEnabled(false)
}

private fun NaverMap.applyDrivingSettings() {
    mapType = NaverMap.MapType.Basic
    isIndoorEnabled = false
    uiSettings.apply {
        isScrollGesturesEnabled = false
        isZoomGesturesEnabled = false
        isTiltGesturesEnabled = false
        isRotateGesturesEnabled = false
        isStopGesturesEnabled = false
        isZoomControlEnabled = false
        isCompassEnabled = false
        isScaleBarEnabled = false
        isLocationButtonEnabled = false
        isIndoorLevelPickerEnabled = false
    }
}

/**
 * [visibleDp] 길이에 [radiusMeters]가 들어가는 줌.
 * 네이버 지도 SDK의 `Projection.getMetersPerDp(latitude, zoom)`은 줌이 1 오를 때마다 절반이 되므로
 * `metersPerDp(zoom) = metersPerDp(0) / 2^zoom`이고, 원하는 dp당 미터(`radiusMeters / visibleDp`)를 넣어 줌을 푼다.
 * @param metersPerDpAtZoom0 차량 위도에서 줌 0의 dp당 미터(`Projection.getMetersPerDp(latitude, 0.0)`)
 */
internal fun zoomFor(metersPerDpAtZoom0: Double, visibleDp: Float, radiusMeters: Double = VISIBLE_RADIUS_METERS): Double {
    val wantedMetersPerDp = radiusMeters / visibleDp
    return ln(metersPerDpAtZoom0 / wantedMetersPerDp) / ln(2.0)
}

private fun GeoPoint.toLatLng() = LatLng(latitude, longitude)

private fun buildOverlays(
    centerPose: VehiclePose,
    vehicle: VehiclePose?,
    ranking: List<RankItemUi>,
    interestMatches: List<MapPointUi>,
    others: List<MapPointUi>,
    images: MarkerImages,
    colors: MarkerColors,
    sizes: MarkerSizes,
): List<Overlay> {
    val center = centerPose.location.toLatLng()
    val circles = listOf(
        CircleOverlay(center, INNER_RING_METERS).apply {
            color = android.graphics.Color.TRANSPARENT
            outlineColor = withAlpha(colors.ink, 0.35f)
            outlineWidth = sizes.stroke.toInt()
            zIndex = 0
        },
        CircleOverlay(center, OUTER_RING_METERS).apply {
            color = android.graphics.Color.TRANSPARENT
            outlineColor = colors.accent
            outlineWidth = sizes.stroke.toInt()
            zIndex = 0
        },
    )
    val dots = others.mapNotNull { point ->
        point.location?.let { marker(it, images.dot, z = 1) }
    }
    val interests = interestMatches.mapNotNull { point ->
        point.location?.let { marker(it, images.interest, z = 2) }
    }
    // Keep each pin at its real coordinate. The map viewport reveals it only as the car approaches;
    // never pan to the leader or fit all 3 km ranking pins into the camera.
    // 1위가 가장 위에 오도록 순위가 높을수록 zIndex를 크게 한다.
    val pins = ranking.mapNotNull { item ->
        item.location?.let { location ->
            val image = images.rank(item.rank)
            marker(location, image, z = 10 - item.rank).apply {
                // 1위 이미지는 원 위에 왕관이 있어 원 중심을 좌표에 맞춘다.
                if (item.rank == 1) anchor = PointF(0.5f, 1f - sizes.pin / 2 / (sizes.pin + sizes.crown * 0.8f))
            }
        }
    }
    val car = vehicle?.let { marker(it.location, if (it.headingDeg == null) images.vehiclePosition else images.vehicle, z = 20) }
    return circles + dots + interests + pins + listOfNotNull(car)
}

private fun marker(location: GeoPoint, image: OverlayImage, z: Int) = Marker().apply {
    position = location.toLatLng()
    icon = image
    anchor = PointF(0.5f, 0.5f)
    zIndex = z
    isFlat = false
    // 주행 중 조작을 두지 않으므로 핀 선택과 겹침 숨김을 쓰지 않는다.
    isHideCollidedMarkers = false
    isHideCollidedSymbols = false
    isHideCollidedCaptions = true
}

private data class MarkerColors(val accent: Int, val ink: Int, val ground: Int, val dot: Int)

private data class MarkerSizes(val pin: Float, val crown: Float, val vehicle: Float, val dot: Float, val stroke: Float)

private fun withAlpha(color: Int, alpha: Float): Int =
    (color and 0x00FFFFFF) or ((alpha * 255).toInt() shl 24)

/** 마커 이미지. 간이 지도(SchematicMap)와 같은 모양을 비트맵으로 그린다. */
private class MarkerImages(private val colors: MarkerColors, private val sizes: MarkerSizes) {
    private val ranks = mutableMapOf<Int, OverlayImage>()

    val dot: OverlayImage by lazy {
        draw(sizes.dot, sizes.dot) { canvas, w, _ ->
            canvas.drawCircle(w / 2, w / 2, w / 2, fill(colors.ground))
            canvas.drawCircle(w / 2, w / 2, w / 2 - sizes.stroke / 2, fill(colors.dot))
        }
    }

    val interest: OverlayImage by lazy {
        draw(sizes.pin, sizes.pin) { canvas, w, _ ->
            val r = w / 2
            canvas.drawCircle(r, r, r - sizes.stroke / 2, fill(colors.ground))
            canvas.drawCircle(r, r, r - sizes.stroke / 2, stroke(colors.accent, sizes.stroke))
            canvas.drawPath(starPath(r, r, r * 0.55f, r * 0.24f), fill(colors.accent))
        }
    }

    val vehicle: OverlayImage by lazy {
        draw(sizes.vehicle, sizes.vehicle) { canvas, w, _ ->
            val c = w / 2
            canvas.drawCircle(c, c, c, fill(withAlpha(colors.accent, 0.15f)))
            canvas.drawCircle(c, c, w * 0.32f, fill(colors.accent))
            canvas.drawCircle(c, c, w * 0.24f, fill(colors.ground))
            val r = w * 0.2f
            val arrow = Path().apply {
                moveTo(c, c - r * 0.9f)
                lineTo(c + r * 0.66f, c + r * 0.7f)
                lineTo(c, c + r * 0.35f)
                lineTo(c - r * 0.66f, c + r * 0.7f)
                close()
            }
            canvas.drawPath(arrow, fill(colors.accent))
        }
    }

    val vehiclePosition: OverlayImage by lazy {
        draw(sizes.vehicle, sizes.vehicle) { canvas, w, _ ->
            val c = w / 2
            canvas.drawCircle(c, c, c, fill(withAlpha(colors.accent, 0.15f)))
            canvas.drawCircle(c, c, w * 0.32f, fill(colors.accent))
            canvas.drawCircle(c, c, w * 0.16f, fill(colors.ground))
        }
    }

    fun rank(rank: Int): OverlayImage = ranks.getOrPut(rank) {
        val leader = rank == 1
        val crownHeight = if (leader) sizes.crown * 0.8f else 0f
        draw(sizes.pin, sizes.pin + crownHeight) { canvas, w, _ ->
            val r = w / 2
            val cy = crownHeight + r
            if (leader) canvas.drawPath(crownPath((w - sizes.crown) / 2, 0f, sizes.crown), fill(colors.accent))
            canvas.drawCircle(r, cy, r, fill(colors.ground))
            canvas.drawCircle(r, cy, r - sizes.stroke / 2, fill(if (leader) colors.accent else colors.ink))
            val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.ground
                textAlign = Paint.Align.CENTER
                textSize = w * 0.5f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText(rank.toString(), r, cy - (text.descent() + text.ascent()) / 2, text)
        }
    }

    private fun draw(width: Float, height: Float, block: (Canvas, Float, Float) -> Unit): OverlayImage {
        val bitmap = Bitmap.createBitmap(width.toInt().coerceAtLeast(1), height.toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        block(Canvas(bitmap), bitmap.width.toFloat(), bitmap.height.toFloat())
        return OverlayImage.fromBitmap(bitmap)
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }

    private fun stroke(color: Int, width: Float) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = width }

    /** CrownIcon과 같은 44 × 34 비율의 왕관. */
    private fun crownPath(left: Float, top: Float, width: Float): Path {
        val k = width / 44f
        val pts = listOf(0f to 34f, 0f to 8f, 12f to 20f, 22f to 2f, 32f to 20f, 44f to 8f, 44f to 34f)
        return Path().apply {
            pts.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(left + x * k, top + y * k * 0.8f) else lineTo(left + x * k, top + y * k * 0.8f) }
            close()
        }
    }

    private fun starPath(cx: Float, cy: Float, outer: Float, inner: Float): Path = Path().apply {
        for (i in 0 until 10) {
            val radius = if (i % 2 == 0) outer else inner
            val angle = Math.toRadians(-90.0 + i * 36.0)
            val x = cx + (radius * cos(angle)).toFloat()
            val y = cy + (radius * sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}
