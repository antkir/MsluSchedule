package by.ntnk.msluschedule.network.api.myuniversity.data

data class JsonGroupClasses constructor(val statusText: String?, val result: Schedule?) {

    data class Schedule constructor(val schedule: List<Class>?)

    data class Class constructor(
        val title: String?,
        val summary: String?,
        val room: String?,
        val start: String?,
        val end: String?,
        val day: String?)
}
