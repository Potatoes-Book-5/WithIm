package com.potatoes.book5.realestate.data.remote

import com.potatoes.book5.realestate.domain.model.GeoCoordinates
import java.io.InputStream
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

internal class InvalidGeocoderResponse : RuntimeException()
internal class GeocoderProviderRejected : RuntimeException()

internal object VworldXmlParser {
    fun parse(input: InputStream): GeoCoordinates {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        if (parser.getFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL)) {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
        }
        parser.setInput(input, null)
        val path = mutableListOf<String>()
        var text = StringBuilder()
        var status: String? = null
        var longitude: String? = null
        var latitude: String? = null
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.DOCDECL -> throw InvalidGeocoderResponse()
                XmlPullParser.START_TAG -> {
                    path.add(parser.name)
                    if (path.size > 16) throw InvalidGeocoderResponse()
                    text = StringBuilder()
                }
                XmlPullParser.TEXT, XmlPullParser.CDSECT, XmlPullParser.ENTITY_REF -> {
                    text.append(parser.text ?: throw InvalidGeocoderResponse())
                    if (text.length > 16_384) throw InvalidGeocoderResponse()
                }
                XmlPullParser.END_TAG -> {
                    when (path.joinToString("/")) {
                        "response/status" -> status = text.toString().trim()
                        "response/result/point/x" -> longitude = text.toString().trim()
                        "response/result/point/y" -> latitude = text.toString().trim()
                    }
                    path.removeAt(path.lastIndex)
                    text = StringBuilder()
                }
            }
            event = parser.nextToken()
        }
        if (status == null) throw InvalidGeocoderResponse()
        if (status != "OK") throw GeocoderProviderRejected()
        val x = longitude?.toDoubleOrNull() ?: throw InvalidGeocoderResponse()
        val y = latitude?.toDoubleOrNull() ?: throw InvalidGeocoderResponse()
        if (!x.isFinite() || !y.isFinite() || x !in -180.0..180.0 || y !in -90.0..90.0) {
            throw InvalidGeocoderResponse()
        }
        return GeoCoordinates(longitude = x, latitude = y)
    }
}
