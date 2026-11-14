package com.potatoes.book5.realestate.data.remote

import com.potatoes.book5.realestate.data.repository.VworldAddressGeocoder
import com.potatoes.book5.realestate.domain.port.AddressGeocoder
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit

object VworldGeocoderFactory {
    fun create(apiKey: String): AddressGeocoder {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS).retryOnConnectionFailure(false)
            .followRedirects(false).followSslRedirects(false).build()
        val service = Retrofit.Builder().baseUrl("https://api.vworld.kr/")
            .client(client).build().create(VworldService::class.java)
        return VworldAddressGeocoder(service, apiKey.trim())
    }
}
