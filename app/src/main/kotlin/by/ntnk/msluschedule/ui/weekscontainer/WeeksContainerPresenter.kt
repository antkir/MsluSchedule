package by.ntnk.msluschedule.ui.weekscontainer

import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.ImmutableEntry
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

    fun initWeeksAdapter() {
        val info = sharedPreferencesRepository.getSelectedScheduleContainerInfo()
        disposables += databaseRepository.getWeeksForContainer(info.id)
                .toList()
                .map { weeks ->
                    val index = when {
                        currentDate.academicWeek < 0 -> 0
                        else -> currentDate.academicWeek
                    }
                    val weekIds = ArrayList<ImmutableEntry>()
                    for (j in index - 2..index + 2) {
                        if (j in weeks.indices) {
                            weekIds.add(ImmutableEntry(weeks[j].id, weeks[j].value))
                        }
                    }
                    val currentItemIndex = if (index < 2) index else 2
                    return@map Pair(weekIds, currentItemIndex)
                }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeBy(
                        onSuccess = { view?.initWeeksAdapter(it.first, it.second) },
                        onError = { throwable -> Timber.e(throwable) }
                )
    }

    fun deleteSelectedScheduleContainer() {
        val info = sharedPreferencesRepository.getSelectedScheduleContainerInfo()
        databaseRepository.deleteScheduleContainer(info.id)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeBy(
                        onComplete = {
                            sharedPreferencesRepository.selectEmptyScheduleContainer()
                            view?.removeScheduleContainerFromView(info)
                        },
                        onError = { throwable -> Timber.e(throwable) }
                )
    }

    fun clearDisposables() = disposables.clear()
}
