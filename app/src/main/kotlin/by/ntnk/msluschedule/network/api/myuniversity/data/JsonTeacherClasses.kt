package by.ntnk.msluschedule.network.api.myuniversity.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JsonTeacherClasses constructor(val result: List<Class>?, val statusText: String?) {
    @JsonClass(generateAdapter = true)
    data class Class constructor(
        val title: String?,
        val summary: String?,
        val room: String?,
        val start: String?,
        val end: String?,
        val day: String?,
        val groupLabel: String?,
    )
}
