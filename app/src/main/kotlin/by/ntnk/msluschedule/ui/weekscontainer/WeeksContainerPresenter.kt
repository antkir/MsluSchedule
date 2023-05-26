package by.ntnk.msluschedule.ui.weekscontainer

import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.db.data.Week
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import timber.log.Timber
import javax.inject.Inject

class WeeksContainerPresenter @Inject constructor(
    private val databaseRepository: DatabaseRepository,
    private val sharedPreferencesRepository: SharedPreferencesRepository,
    private val currentDate: CurrentDate,
    private val schedulerProvider: SchedulerProvider
) : Presenter<WeeksContainerView>() {

    private val disposables: CompositeDisposable = CompositeDisposable()

    fun getWeeks() {
        val containerInfo = sharedPreferencesRepository.getSelectedScheduleContainerInfo()
        disposables += databaseRepository.getWeeks(containerInfo.id)
            .toList()
            .map { weeks ->
                val currentAcademicWeekIndex = when {
                    currentDate.academicWeek < 0 -> 0
                    else -> currentDate.academicWeek
                }
                val filteredWeeks = mutableListOf<Week>()
                for (i in currentAcademicWeekIndex - SIDE_WEEKS_NUM..currentAcademicWeekIndex + SIDE_WEEKS_NUM) {
                    if (i in weeks.indices) {
                        filteredWeeks.add(weeks[i])
                    }
                }
                Pair(filteredWeeks, currentAcademicWeekIndex)
            }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { (weeks, currentAcademicWeekIndex) -> view?.onData(weeks, currentAcademicWeekIndex) },
                onError = { throwable -> Timber.e(throwable) }
            )
    }

    fun deleteSelectedScheduleContainer() {
        val containerInfo = sharedPreferencesRepository.getSelectedScheduleContainerInfo()
        databaseRepository.deleteScheduleContainer(containerInfo.id)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onComplete = {
                    sharedPreferencesRepository.selectEmptyScheduleContainer()
                    view?.onDeleteScheduleContainer(containerInfo)
                },
                onError = { throwable -> Timber.e(throwable) }
            )
    }

    fun clearDisposables() {
        disposables.clear()
    }

    companion object {
        private const val WEEKS_NUM = 5
        private const val SIDE_WEEKS_NUM = WEEKS_NUM / 2
        const val CENTRAL_VIEW_POSITION = WEEKS_NUM / 2
    }
}
