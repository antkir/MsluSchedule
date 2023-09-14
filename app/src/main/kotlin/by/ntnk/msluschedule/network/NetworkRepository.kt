package by.ntnk.msluschedule.network

import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.data.WeekdayWithStudyGroupLessons
import by.ntnk.msluschedule.data.WeekdayWithTeacherLessons
import by.ntnk.msluschedule.network.data.ScheduleFilter
import io.reactivex.Observable
import io.reactivex.Single

interface NetworkRepository {
    fun getFaculties(): Single<ScheduleFilter>

    fun getCourses(facultyKey: Int): Single<ScheduleFilter>

    fun getGroups(facultyKey: Int, courseKey: Int, yearKey: Int): Single<ScheduleFilter>

    fun getTeachers(): Single<ScheduleFilter>

    fun getWeeks(): Single<ScheduleFilter>

    fun getSchedule(studyGroup: StudyGroup, weekKey: Int): Observable<WeekdayWithStudyGroupLessons>

    fun getSchedule(teacher: Teacher, weekKey: Int): Observable<WeekdayWithTeacherLessons>
}
