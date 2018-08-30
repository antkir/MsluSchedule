package by.ntnk.msluschedule.ui.weekscontainer

import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.ImmutableEntry
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import io.reactivex.rxkotlin.subscribeBy
import org.threeten.bp.DayOfWeek
import javax.inject.Inject

class WeeksContainerPresenter @Inject constructor(
        private val databaseRepository: DatabaseRepository,
        private val sharedPreferencesRepository: SharedPreferencesRepository,
        private val currentDate: CurrentDate,
        private val schedulerProvider: SchedulerProvider
) : Presenter<WeeksContainerView>() {
    private var savedWeekItemIndex: Int = 0
    private val academicWeek: Int = currentDate.academicWeek

    fun initWeeksAdapter() {
        val info = sharedPreferencesRepository.getSelectedScheduleContainerInfo()
        databaseRepository.getWeeksForContainer(info.id)
                .toList()
                .map { weeks ->
                    val index = when {
                        currentDate.date.dayOfWeek == DayOfWeek.SUNDAY -> currentDate.academicWeek + 1
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
                        onSuccess = {
                            savedWeekItemIndex = it.second
                            view?.initWeeksAdapter(it.first, it.second)
                        },
                        onError = { it.printStackTrace() }
                )
    }

    fun getCurrentWeekIndex(): Int = savedWeekItemIndex + (currentDate.academicWeek - academicWeek)

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
                        onError = { it.printStackTrace() }
                )
    }
}
