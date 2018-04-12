package by.ntnk.msluschedule.ui.main

import by.ntnk.msluschedule.data.ScheduleContainerInfo
import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.utils.*
import io.reactivex.Single
import javax.inject.Inject

class MainPresenter @Inject constructor(
        private val databaseRepository: DatabaseRepository,
        private val networkRepository: NetworkRepository,
        private val sharedPreferencesRepository: SharedPreferencesRepository,
        private val schedulerProvider: SchedulerProvider
) : Presenter<MainView>() {
    fun addGroup(studyGroup: StudyGroup) {
        databaseRepository.insertStudyGroup(studyGroup)
                .flatMap { insertWeeksWithWeekdays(it) }
                .subscribeOn(schedulerProvider.single())
                .observeOn(schedulerProvider.ui())
                .subscribe(
                        {
                            val info = ScheduleContainerInfo(it, studyGroup.name, ScheduleType.STUDYGROUP)
                            sharedPreferencesRepository.putSelectedScheduleContainer(info)
                            view?.initMainContent()
                            view?.addScheduleContainerMenuItem(info)
                            view?.checkScheduleContainerMenuItem(info)
                        },
                        { it.printStackTrace() }
                )
    }

    fun addTeacher(teacher: Teacher) {
        databaseRepository.insertTeacher(teacher)
                .flatMap { insertWeeksWithWeekdays(it) }
                .subscribeOn(schedulerProvider.single())
                .observeOn(schedulerProvider.ui())
                .subscribe(
                        {
                            val info = ScheduleContainerInfo(it, teacher.name, ScheduleType.TEACHER)
                            sharedPreferencesRepository.putSelectedScheduleContainer(info)
                            view?.initMainContent()
                            view?.addScheduleContainerMenuItem(info)
                            view?.checkScheduleContainerMenuItem(info)
                        },
                        { it.printStackTrace() }
                )
    }

    private fun insertWeeksWithWeekdays(containerId: Int): Single<Int> {
        return networkRepository.getWeeks()
                .flatMapObservable { databaseRepository.insertWeeksGetIds(it, containerId) }
                .ignoreElements()
                .doOnError { databaseRepository.deleteScheduleContainer(containerId) }
                .andThen(Single.just(containerId))
    }

    fun initContainerListView() {
        databaseRepository.getScheduleContainers()
                .subscribeOn(schedulerProvider.single())
                .observeOn(schedulerProvider.ui())
                .subscribe(
                        {
                            val info = ScheduleContainerInfo(it.id, it.name, it.type)
                            view?.addScheduleContainerMenuItem(info)
                        },
                        { it.printStackTrace() },
                        {
                            val info = sharedPreferencesRepository.getSelectedScheduleContainerInfo()
                            view?.checkScheduleContainerMenuItem(info)
                        }
                )
    }

    fun setSelectedSheduleContainer(id: Int, value: String, type: ScheduleType) =
            sharedPreferencesRepository.putSelectedScheduleContainer(id, value, type)

    fun isSelectedContainerNull() =
            sharedPreferencesRepository.getSelectedScheduleContainerInfo().type == null
}
