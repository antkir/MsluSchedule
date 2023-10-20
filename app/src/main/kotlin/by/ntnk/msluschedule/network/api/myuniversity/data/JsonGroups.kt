package by.ntnk.msluschedule.network.api.myuniversity.data

data class JsonGroups constructor(val statusText: String?, val result: List<Faculty>?) {

    data class Faculty constructor(val faculty: String?, val facultyCode: String?, val courses: List<Course>?)

    data class Course constructor(val course: String?, val courseCode: String?, val groups: List<Group>?)

    data class Group constructor(val group: String?, val groupCode: String?)
}
