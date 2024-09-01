package by.ntnk.msluschedule.network.api.original.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RequestData constructor(internal val requestInfo: RequestInfo, val selectedValue: Int) {
    val requestName: String
        get() = requestInfo.requestName

    val requestRelatedName: String
        get() = requestInfo.requestRelatedName
}
