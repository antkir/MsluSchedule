package by.ntnk.msluschedule.network.api.myuniversity.data

data class JsonTeacherClasses constructor(val result: List<Class>?, val statusText: String?) {

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
