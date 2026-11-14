package com.potatoes.book5.realestate.data.remote

import com.potatoes.book5.realestate.data.repository.KaptHouseholdRepository
import com.potatoes.book5.realestate.domain.port.ApartmentHouseholdRepository
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

object KaptRepositoryFactory {
    fun create(apiKey: String): ApartmentHouseholdRepository {
        val client = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(6, TimeUnit.SECONDS).retryOnConnectionFailure(false)
            .followRedirects(false).followSslRedirects(false).build()
        val service = Retrofit.Builder().baseUrl("https://apis.data.go.kr/1613000/")
            .client(client).build().create(KaptService::class.java)
        return KaptHouseholdRepository(service, apiKey.trim())
    }
}

internal interface KaptService {
    @Streaming
    @GET("AptListService4/getSigunguAptList4")
    suspend fun complexes(
        @Query("serviceKey") decodedKey: String,
        @Query("sigunguCode") districtCode: String,
        @Query("pageNo") page: Int,
        @Query("numOfRows") pageSize: Int,
    ): Response<ResponseBody>

    @Streaming
    @GET("AptBasisInfoServiceV5/getAphusBassInfoV5")
    suspend fun basicInfo(
        @Query("serviceKey") decodedKey: String,
        @Query("kaptCode") complexCode: String,
    ): Response<ResponseBody>
}
