package com.potatoes.book5.realestate.domain.port

enum class DrivingAlertSpeechKind { RECOMMENDATION, LEADER }

/** A short announcement belonging to one visible driving banner. */
interface DrivingAlertSpeech {
    fun announce(key: Long, apartmentName: String, kind: DrivingAlertSpeechKind)
    fun stop(key: Long? = null)
}
