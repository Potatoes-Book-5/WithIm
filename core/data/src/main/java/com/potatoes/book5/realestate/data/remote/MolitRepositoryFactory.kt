package com.potatoes.book5.realestate.data.remote

import com.potatoes.book5.realestate.data.repository.RemoteApartmentTradeRepository
import com.potatoes.book5.realestate.domain.port.ApartmentTradeRepository
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/** Creates only the data-layer implementation; no logging or disk cache. */
object MolitRepositoryFactory {
    fun create(apiKey: String): ApartmentTradeRepository {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS).retryOnConnectionFailure(false)
            .followRedirects(false).followSslRedirects(false).build()
        val service = Retrofit.Builder()
            .baseUrl("https://apis.data.go.kr/1613000/RTMSDataSvcAptTradeDev/")
            .client(client).build().create(MolitService::class.java)
        return RemoteApartmentTradeRepository(service, apiKey.trim())
    }
}
