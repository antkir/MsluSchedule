package by.ntnk.msluschedule.network.api.original.data

data class RequestData constructor(private val requestInfo: RequestInfo, val selectedValue: Int) {
    val requestName: String
        get() = requestInfo.requestName

    val requestRelatedName: String
        get() = requestInfo.requestRelatedName
}
