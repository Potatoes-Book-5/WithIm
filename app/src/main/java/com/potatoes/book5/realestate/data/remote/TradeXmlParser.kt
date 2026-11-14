package com.potatoes.book5.realestate.data.remote

import java.io.InputStream
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

internal object TradeXmlParser {
    fun parse(input: InputStream, maxItems: Int): TradeResponseDto {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        // KXml rejects even setting false when the feature is unsupported.
        if (parser.getFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL)) {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
        }
        parser.setInput(input, null)
        val path = mutableListOf<String>()
        val metadata = mutableMapOf<String, String>()
        val items = mutableListOf<Map<String, String>>()
        var item: MutableMap<String, String>? = null
        var text = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.DOCDECL -> throw InvalidTradeResponse()
                XmlPullParser.START_TAG -> {
                    path.add(parser.name)
                    if (path.size > 16) throw InvalidTradeResponse()
                    text = StringBuilder()
                    if (path.joinToString("/") == "response/body/items/item") item = mutableMapOf()
                }
                XmlPullParser.TEXT, XmlPullParser.CDSECT, XmlPullParser.ENTITY_REF -> {
                    text.append(parser.text ?: throw InvalidTradeResponse())
                    if (text.length > 16_384) throw InvalidTradeResponse()
                }
                XmlPullParser.END_TAG -> {
                    val fullPath = path.joinToString("/")
                    val value = text.toString().trim()
                    when {
                        fullPath == "response/body/items/item" -> {
                            if (items.size >= maxItems) throw InvalidTradeResponse()
                            items.add(item ?: throw InvalidTradeResponse())
                            item = null
                        }
                        path.size == 5 && fullPath.startsWith("response/body/items/item/") ->
                            item?.set(parser.name, value)
                        (path.size == 3 && fullPath.startsWith("response/header/")) ||
                            (path.size == 3 && fullPath.startsWith("response/body/")) ||
                            fullPath == "OpenAPI_ServiceResponse/cmmMsgHeader/returnReasonCode" ->
                            metadata[parser.name] = value
                    }
                    path.removeAt(path.lastIndex)
                    text = StringBuilder()
                }
            }
            event = parser.nextToken()
        }
        return TradeResponseDto(metadata["resultCode"], metadata.containsKey("returnReasonCode"),
            metadata["pageNo"], metadata["numOfRows"], metadata["totalCount"], items)
    }
}
