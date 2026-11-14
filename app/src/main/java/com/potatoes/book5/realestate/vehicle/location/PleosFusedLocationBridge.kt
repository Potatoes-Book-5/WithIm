package com.potatoes.book5.realestate.vehicle.location

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import androidx.core.content.ContextCompat
import com.potatoes.book5.realestate.domain.model.GeoPoint
import com.potatoes.book5.realestate.domain.model.VehiclePose
import com.potatoes.book5.realestate.domain.port.ConsentRepository
import com.potatoes.book5.realestate.domain.port.LocationSource
import com.potatoes.book5.realestate.domain.port.LocationStatus
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Pleos FusedLocation SDK 콜백을 [VehicleLocationInput]에 넣는다(F-001, CP-004, docs/platform/location-input.md).
 *
 * - 위치 사용 동의가 있을 때만 SDK를 쓴다(F-001 규칙 2). 지원 여부(CP-004)를 권한보다 먼저 확인한다.
 *   `isSupported()`는 초기화·권한과 무관하고, 위치 서비스가 없는 차량에는 권한 정의도 없기 때문이다.
 * - 권한이 있으면 문서의 생명주기를 따른다. 매번 새 인스턴스: initialize → registerFusedLocationCallback,
 *   닫을 때 unregisterFusedLocationCallback → release. `release()` 뒤 같은 인스턴스는 쓰지 않는다.
 * - `heading`, `confidence`는 단위와 기준이 문서에 없어 넘기지 않는다. 진행 방향과 정확도는 미제공(null)이다(T-029 미결).
 * - `onChanged(null)`은 위치 탐색 중으로, `onError`는 예외 종류를 알 수 없어 권한 오류만 구분하고 나머지는 탐색 중으로 넘긴다.
 */
class PleosFusedLocationBridge(
    context: Context,
    private val input: VehicleLocationInput,
    private val consentRepository: ConsentRepository,
) {
    private val appContext = context.applicationContext
    private val permissionGranted = MutableStateFlow(hasPermission())

    /** 권한 요청 결과나 설정 변경을 반영한다. 화면이 다시 보일 때 부른다. */
    fun refreshPermission() {
        permissionGranted.value = hasPermission()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(scope: CoroutineScope): Job = scope.launch {
        combine(consentRepository.observe().map { it?.locationConsented == true }, permissionGranted) { consented, granted ->
            consented to granted
        }.distinctUntilChanged().collectLatest { (consented, granted) ->
            when {
                // 입력 포트도 동의 없음을 알리지만, 남아 있던 좌표를 지워 다시 동의했을 때 쓰지 않게 한다.
                !consented -> input.unavailable(LocationStatus.CONSENT_REQUIRED)
                !FusedLocation(appContext).isSupported() -> input.unavailable(LocationStatus.UNSUPPORTED)
                !granted -> input.unavailable(LocationStatus.PERMISSION_REQUIRED)
                // 동의나 권한이 바뀌면 collectLatest가 이 수집을 취소하고, awaitClose에서 SDK를 해제한다.
                else -> fusedLocationEvents().collect(::deliver)
            }
        }
    }

    private fun deliver(event: FusedEvent) {
        when (event) {
            FusedEvent.Searching -> input.unavailable(LocationStatus.SEARCHING)
            FusedEvent.PermissionDenied -> {
                input.unavailable(LocationStatus.PERMISSION_REQUIRED)
                refreshPermission()
            }
            is FusedEvent.Fix -> input.updateLocation(
                pose = VehiclePose(GeoPoint(event.data.latitude, event.data.longitude), headingDeg = null),
                accuracyMeters = null,
                observedAt = Instant.ofEpochMilli(event.data.timestampMillis),
                source = LocationSource.VEHICLE,
            )
        }
    }

    /** 콜백 스레드는 문서에 없어 trySend로 넘긴다. */
    private fun fusedLocationEvents(): Flow<FusedEvent> = callbackFlow {
        val fusedLocation = FusedLocation(appContext)
        trySend(FusedEvent.Searching)
        val listener = object : FusedLocationListener {
            override fun onChanged(location: LocationData?) {
                trySend(location?.let(FusedEvent::Fix) ?: FusedEvent.Searching)
            }

            override fun onError(exception: Exception) {
                Log.w(TAG, "FusedLocation error", exception)
                trySend(if (exception is SecurityException) FusedEvent.PermissionDenied else FusedEvent.Searching)
            }
        }
        val registered = runCatching {
            fusedLocation.initialize()
            fusedLocation.registerFusedLocationCallback(listener)
        }.onFailure { error ->
            Log.w(TAG, "FusedLocation registration failed", error)
            trySend(if (error is SecurityException) FusedEvent.PermissionDenied else FusedEvent.Searching)
        }.isSuccess
        awaitClose {
            if (registered) runCatching { fusedLocation.unregisterFusedLocationCallback(listener) }
            runCatching { fusedLocation.release() }
        }
    }

    private fun hasPermission() =
        ContextCompat.checkSelfPermission(appContext, PERMISSION) == PackageManager.PERMISSION_GRANTED

    private sealed interface FusedEvent {
        data object Searching : FusedEvent
        data object PermissionDenied : FusedEvent
        data class Fix(val data: LocationData) : FusedEvent
    }

    companion object {
        /** FusedLocation 위치 API 권한(protection level dangerous) */
        const val PERMISSION = "pleos.car.permission.FUSED_LOCATION"
        private const val TAG = "PleosFusedLocation"
    }
}
