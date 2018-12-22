package by.ntnk.msluschedule.ui.lessoninfo

import by.ntnk.msluschedule.data.StudyGroupLesson
import by.ntnk.msluschedule.data.TeacherLesson
import by.ntnk.msluschedule.mvp.View

interface LessonInfoView : View {
    fun showInfo(lesson: StudyGroupLesson, weekdaysWithLesson: List<String>)
    fun showInfo(lesson: TeacherLesson, weekdaysWithLesson: List<String>)
    fun destroyView()
}
