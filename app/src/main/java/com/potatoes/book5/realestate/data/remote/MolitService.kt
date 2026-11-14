package com.potatoes.book5.realestate.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

internal interface MolitService {
    @Streaming
    @GET("getRTMSDataSvcAptTradeDev")
    suspend fun getTrades(
        @Query("serviceKey") decodedKey: String,
        @Query("LAWD_CD") districtCode: String,
        @Query("DEAL_YMD") contractMonth: String,
        @Query("pageNo") pageNumber: Int,
        @Query("numOfRows") pageSize: Int,
    ): Response<ResponseBody>
}

internal data class TradeResponseDto(
    val resultCode: String?,
    val gatewayRejected: Boolean,
    val pageNumber: String?,
    val pageSize: String?,
    val totalCount: String?,
    val items: List<Map<String, String>>,
)

internal class InvalidTradeResponse : RuntimeException()
internal class TradeProviderRejected : RuntimeException()
