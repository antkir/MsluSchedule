package by.ntnk.msluschedule.ui.main

import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.utils.singleScheduler
import by.ntnk.msluschedule.utils.uiScheduler
import io.reactivex.CompletableTransformer
import javax.inject.Inject

class MainPresenter @Inject constructor(
        private val databaseRepository: DatabaseRepository,
        private val networkRepository: NetworkRepository
) : Presenter<MainView>() {
    fun addGroup(studyGroup: StudyGroup) {
        databaseRepository.insertStudyGroup(studyGroup)
                .compose(insertWeeksWithWeekdays(studyGroup.id))
                .subscribeOn(singleScheduler)
                .observeOn(uiScheduler)
                .subscribe(
                        { view?.initMainContent() },
                        { it.printStackTrace() }
                )
    }

    fun addTeacher(teacher: Teacher) {
        databaseRepository.insertTeacher(teacher)
                .compose(insertWeeksWithWeekdays(teacher.id))
                .subscribeOn(singleScheduler)
                .observeOn(uiScheduler)
                .subscribe(
                        { view!!.initMainContent() },
                        { it.printStackTrace() }
                )
    }

    private fun insertWeeksWithWeekdays(containerId: Int): CompletableTransformer {
        return CompletableTransformer {
            return@CompletableTransformer it
                    .andThen(networkRepository.getWeeks())
                    .flatMapObservable { databaseRepository.insertWeeksGetIds(it, containerId) }
                    .flatMapCompletable { databaseRepository.insertWeekdays(it) }
        }
    }
}