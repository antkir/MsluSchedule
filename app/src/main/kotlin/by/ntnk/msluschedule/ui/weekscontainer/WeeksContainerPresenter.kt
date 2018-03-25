package by.ntnk.msluschedule.ui.weekscontainer

import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.utils.*
import org.threeten.bp.DayOfWeek
import javax.inject.Inject

class WeeksContainerPresenter @Inject constructor(
        private val databaseRepository: DatabaseRepository,
        private val sharedPreferencesRepository: SharedPreferencesRepository,
        private val currentDate: CurrentDate
) : Presenter<WeeksContainerView>() {
    fun initWeeksAdapter() {
        val info = sharedPreferencesRepository.getSelectedScheduleContainerInfo()
        databaseRepository.getWeeksForContainer(info.id)
                .toList()
                .map { weeks ->
                    val index = if (currentDate.date.dayOfWeek == DayOfWeek.SUNDAY) {
                        currentDate.academicWeek + 1
                    } else {
                        currentDate.academicWeek
                    }
                    val weekIds = ArrayList<ImmutableEntry>()
                    for (j in index - 2 .. index + 2) {
                        if (j in weeks.indices) {
                            weekIds.add(ImmutableEntry(weeks[j].id, weeks[j].value))
                        }
                    }
                    val currentItemIndex = if (index < 2) index else 2
                    return@map Pair(weekIds, currentItemIndex)
                }
                .subscribeOn(ioScheduler)
                .observeOn(uiScheduler)
                .subscribe(
                        { view!!.initWeeksAdapter(it.first, it.second) },
                        { it.printStackTrace() })
    }

    fun removeSelectedScheduleContainer() {
        val info = sharedPreferencesRepository.getSelectedScheduleContainerInfo()
        databaseRepository.deleteScheduleContainer(info.id)
                .subscribeOn(ioScheduler)
                .observeOn(uiScheduler)
                .subscribe(
                        {
                            sharedPreferencesRepository.selectEmptyScheduleContainer()
                            view!!.removeScheduleContainerFromView(info)
                        },
                        { it.printStackTrace() }
                )
    }
}
