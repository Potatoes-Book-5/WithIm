package com.potatoes.book5.realestate

import android.app.Application
import com.potatoes.book5.realestate.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RealEstateApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    /** 화면과 관계없이 프로세스가 살아 있는 동안 도는 작업(주행 기록, F-009 백그라운드 기록 유지). */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            container.prepareRealData()
            launch { container.marketRepository.run() }
            launch { container.refreshScrapChanges() }
            container.driveSessionRecorder.run()
        }
        appScope.launch { container.tourAreas.run() }
        appScope.launch { container.tourBriefingCollector.run() }
        appScope.launch { container.tourNoticeCollector.run() }
        appScope.launch {
            container.vehicleLocationProvider.locations.collect { fix ->
                val context=container.captureTourContext()
                val current=context.priceLocation?.copy(point=fix.pose?.location.takeIf { fix.status==com.potatoes.book5.realestate.domain.port.LocationStatus.AVAILABLE },
                    observedAt=fix.observedAt,accuracyMeters=fix.accuracyMeters,source=fix.source)
                container.tourEntries(context.copy(priceLocation=current))
            }
        }
        container.fusedLocationBridge.start(appScope)
        appScope.launch {
            container.consentChanges.collect { container.voiceContext.setConsented(it?.locationConsented == true) }
        }
    }
    fun launchGleoRequest(intent: android.content.Intent, receivedAtMillis: Long): kotlinx.coroutines.Job {
        val handler = container.gleoIntentHandler
        val captured = handler?.captureContext()
        return appScope.launch { handler?.handle(intent, receivedAtMillis, captured) }
    }
}
