package by.ntnk.msluschedule.network

class JsonBody {
    private val zones: Zones? = null
    val studyWeekZone: String?
        get() = zones!!.studyWeekZone
    val studyGroupZone: String?
        get() = zones!!.studyGroupZone

    private class Zones {
        val studyWeekZone: String? = null
        val studyGroupZone: String? = null
    }
}
