package com.potatoes.book5.realestate.data.remote

import com.potatoes.book5.realestate.data.repository.VworldDistrictResolver
import com.potatoes.book5.realestate.domain.port.MarketDistrictResolver
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit

object VworldDistrictResolverFactory {
    fun create(apiKey: String): MarketDistrictResolver {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS).retryOnConnectionFailure(false)
            .followRedirects(false).followSslRedirects(false).build()
        val service = Retrofit.Builder().baseUrl("https://api.vworld.kr/")
            .client(client).build().create(VworldDistrictService::class.java)
        return VworldDistrictResolver(service, apiKey.trim())
    }
}
