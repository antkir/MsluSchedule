package by.ntnk.msluschedule.network.data

data class JsonBody constructor(private val zones: Zones) {
    val studyWeekZone: String
        get() = zones.studyWeekZone
    val studyGroupZone: String
        get() = zones.studyGroupZone

    data class Zones constructor(val studyWeekZone: String, val studyGroupZone: String)
}
