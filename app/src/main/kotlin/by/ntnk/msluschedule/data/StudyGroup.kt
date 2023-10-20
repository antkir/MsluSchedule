package by.ntnk.msluschedule.data

data class StudyGroup constructor(
    val id: Int,
    val key: String,
    val value: String,
    val year: Int,
    val faculty: Int,
    val course: Int
)
