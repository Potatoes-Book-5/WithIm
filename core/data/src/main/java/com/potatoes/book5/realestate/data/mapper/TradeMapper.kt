package com.potatoes.book5.realestate.data.mapper

import com.potatoes.book5.realestate.data.remote.InvalidTradeResponse
import com.potatoes.book5.realestate.data.remote.TradeProviderRejected
import com.potatoes.book5.realestate.data.remote.TradeResponseDto
import com.potatoes.book5.realestate.domain.model.ApartmentTrade
import com.potatoes.book5.realestate.domain.model.TradePage
import com.potatoes.book5.realestate.domain.model.TradeRoadAddress
import java.time.DateTimeException
import java.time.LocalDate
import java.time.YearMonth

internal fun TradeResponseDto.toDomain(
    districtCode: String,
    contractMonth: YearMonth,
    requestedPage: Int,
    requestedSize: Int,
): TradePage {
    if (gatewayRejected) throw TradeProviderRejected()
    val code = resultCode ?: throw InvalidTradeResponse()
    if (code != "000") throw TradeProviderRejected()
    val page = pageNumber?.toIntOrNull() ?: throw InvalidTradeResponse()
    val size = pageSize?.toIntOrNull() ?: throw InvalidTradeResponse()
    val count = totalCount?.toIntOrNull() ?: throw InvalidTradeResponse()
    val offset = (page - 1L) * size
    if (page != requestedPage || size != requestedSize || count < 0 || items.size > size ||
        offset + items.size > count || (items.isEmpty() && offset < count)
    ) throw InvalidTradeResponse()
    return TradePage(items.map { it.toTrade(districtCode, contractMonth) }, page, size, count)
}

private fun Map<String, String>.toTrade(districtCode: String, month: YearMonth): ApartmentTrade {
    fun optional(name: String) = get(name)?.trim()?.takeIf(String::isNotBlank)
    fun required(name: String) = optional(name) ?: throw InvalidTradeResponse()
    fun integer(name: String) = required(name).toIntOrNull() ?: throw InvalidTradeResponse()
    fun optionalInteger(name: String) = optional(name)?.let { it.toIntOrNull() ?: throw InvalidTradeResponse() }
    if (required("sggCd") != districtCode) throw InvalidTradeResponse()
    val rawAmount = required("dealAmount")
    if (!rawAmount.matches(Regex("(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)"))) throw InvalidTradeResponse()
    val amount = rawAmount.replace(",", "").toLongOrNull() ?: throw InvalidTradeResponse()
    if (amount <= 0) throw InvalidTradeResponse()
    val date = try {
        LocalDate.of(integer("dealYear"), integer("dealMonth"), integer("dealDay"))
    } catch (_: DateTimeException) {
        throw InvalidTradeResponse()
    }
    if (YearMonth.from(date) != month) throw InvalidTradeResponse()
    val area = optional("excluUseAr")?.let { raw ->
        raw.toBigDecimalOrNull()?.takeIf { it.signum() > 0 } ?: throw InvalidTradeResponse()
    }
    val road = optional("roadNm")?.let { name ->
        val main = optionalInteger("roadNmBonbun")
        val sub = optionalInteger("roadNmBubun")
        if ((main != null && main < 0) || (sub != null && sub < 0)) throw InvalidTradeResponse()
        main?.takeIf { it > 0 }?.let { TradeRoadAddress(name, it, sub?.takeIf { n -> n > 0 }) }
    }
    return ApartmentTrade(
        apartmentId = optional("aptSeq"), apartmentName = optional("aptNm"), districtCode = districtCode,
        legalDong = optional("umdNm"), lotNumber = optional("jibun"), priceManwon = amount,
        exclusiveAreaSquareMeters = area, contractDate = date, floor = optionalInteger("floor"),
        builtYear = optionalInteger("buildYear"), roadAddress = road,
        cancellationType = optional("cdealType"), cancellationDate = optional("cdealDay"),
    )
}
