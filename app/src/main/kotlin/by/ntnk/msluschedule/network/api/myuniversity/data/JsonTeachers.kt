package by.ntnk.msluschedule.network.api.myuniversity.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JsonTeachers constructor(val result: List<String>?, val cache: Boolean?, val statusText: String?)
