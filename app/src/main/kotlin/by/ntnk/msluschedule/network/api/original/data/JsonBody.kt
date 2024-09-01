package by.ntnk.msluschedule.network.api.original.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JsonBody constructor(internal val zones: Zones) {
    val studyWeekZone: String
        get() = zones.studyWeekZone
    val studyGroupZone: String
        get() = zones.studyGroupZone

    @JsonClass(generateAdapter = true)
    data class Zones constructor(val studyWeekZone: String, val studyGroupZone: String)
}
