package by.ntnk.msluschedule.ui.main

import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.mvp.Presenter
import timber.log.Timber
import javax.inject.Inject

class MainPresenter @Inject constructor(
        private val databaseRepository: DatabaseRepository
) : Presenter<MainView>() {
    fun addGroup(studyGroup: StudyGroup) {
        Timber.d(studyGroup.toString())
    }

    fun addTeacher(teacher: Teacher) {
        Timber.d(teacher.toString())
    }
}