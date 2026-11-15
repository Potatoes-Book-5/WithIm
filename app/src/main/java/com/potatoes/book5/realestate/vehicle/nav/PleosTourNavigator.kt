package com.potatoes.book5.realestate.vehicle.nav

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.constants.NaviErrorCode
import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.tour.*
import com.potatoes.book5.realestate.domain.port.TourNavigator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicBoolean

/** No invented vendor package or isSupported API. Connection callback verifies actual service access. */
class PleosTourNavigator(context: Context,private val scope: CoroutineScope) : TourNavigator {
    private val app=context.applicationContext
    private val updates=MutableSharedFlow<NavigationEvent>(extraBufferCapacity=4)
    override val events=updates.asSharedFlow()
    override fun supported()=app.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE) &&
        listOf("pleos.car.permission.NAVI_ROUTE","pleos.car.permission.NAVI_ROUTE_SEARCH").all {
            ContextCompat.checkSelfPermission(app,it)==PackageManager.PERMISSION_GRANTED
        }
    private val inFlight=AtomicBoolean(false)
    override fun pending()=inFlight.get()
    override fun activeRoute(): Boolean? = null // No current-route evidence: require replacement confirmation.
    override suspend fun request(entrance: VerifiedEntrance,requestId: String,allowed: ()->Boolean,eventAllowed: ()->Boolean): Outcome<NavigationReceipt> = withContext(Dispatchers.Main.immediate) {
        if(!supported()) return@withContext Outcome.Failure(DomainError.Unsupported("navigation_permission_or_capability"))
        if(!inFlight.compareAndSet(false,true)) return@withContext Outcome.Failure(DomainError.Invalid("navigation_busy"))
        val completion=NavigationCompletion(requestId,eventAllowed)
        val ended=CompletableDeferred<Unit>()
        var helper:NaviHelper?=null
        var listener:NaviHelperEventListener?=null
        fun close() {
            runCatching { listener?.let { helper?.removeListener(it) } }
            runCatching { helper?.release() }
            inFlight.set(false)
        }
        fun finish(result:Outcome<NavigationReceipt>) {
            completion.finish(result)?.let { event ->
                updates.tryEmit(event)
                ended.complete(Unit)
            }
        }
        try {
            val binding=NaviHelper(app);helper=binding
            val connected=CompletableDeferred<Boolean>()
            binding.initialize({ connected.complete(true) },{ connected.complete(false) })
            if(withTimeoutOrNull(2_000) { connected.await() }!=true) {
                completion.cancel();close();return@withContext Outcome.Failure(DomainError.Unsupported("navigation_connection"))
            }
            listener=object : NaviHelperEventListener {
                override fun onRouteStarted(info:RouteStartInfo) { finish(Outcome.Success(NavigationReceipt("GUIDING",requestId))) }
                override fun onError(error:NaviErrorCode) { finish(Outcome.Failure(DomainError.ProviderRejected)) }
            }
            binding.addListener(requireNotNull(listener))
            // Initialization suspends: check the original context immediately before the side effect.
            if(!allowed()) {
                completion.cancel();close();return@withContext Outcome.Failure(DomainError.Invalid("context_invalidated"))
            }
            binding.requestRoute(RouteInfo(latitude=entrance.point.latitude,longitude=entrance.point.longitude,
                poiName=entrance.address,address=entrance.address,poiId=requireNotNull(entrance.navigationPoiId),poiSubId=requireNotNull(entrance.navigationPoiSubId)))
            scope.launch(Dispatchers.Main.immediate) {
                try {
                    if(withTimeoutOrNull(10_000) { ended.await() }==null) finish(Outcome.Failure(DomainError.Timeout))
                } finally { completion.cancel();close() }
            }
            Outcome.Success(NavigationReceipt("SUBMITTED",requestId))
        } catch(e:CancellationException) { completion.cancel();close();throw e }
        catch(_:SecurityException) { completion.cancel();close();Outcome.Failure(DomainError.Unsupported("navigation_permission")) }
        catch(_:Exception) { completion.cancel();close();Outcome.Failure(DomainError.InvalidResponse) }
    }
}
