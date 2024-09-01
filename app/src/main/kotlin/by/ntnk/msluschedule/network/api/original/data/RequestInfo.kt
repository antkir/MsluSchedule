package by.ntnk.msluschedule.network.api.original.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RequestInfo constructor(val requestName: String, val requestRelatedName: String)
