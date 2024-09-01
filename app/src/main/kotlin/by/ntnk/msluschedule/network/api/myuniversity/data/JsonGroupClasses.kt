package by.ntnk.msluschedule.network.api.myuniversity.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JsonGroupClasses constructor(val statusText: String?, val result: Schedule?) {
    @JsonClass(generateAdapter = true)
    data class Schedule constructor(val schedule: List<Class>?)
    @JsonClass(generateAdapter = true)
    data class Class constructor(
        val title: String?,
        val summary: String?,
        val room: String?,
        val start: String?,
        val end: String?,
        val day: String?)
}
