package by.ntnk.msluschedule.network.api.myuniversity.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JsonGroups constructor(val statusText: String?, val result: List<Faculty>?) {
    @JsonClass(generateAdapter = true)
    data class Faculty constructor(val faculty: String?, val facultyCode: String?, val courses: List<Course>?)
    @JsonClass(generateAdapter = true)
    data class Course constructor(val course: String?, val courseCode: String?, val groups: List<Group>?)
    @JsonClass(generateAdapter = true)
    data class Group constructor(val group: String?, val groupCode: String?)
}
