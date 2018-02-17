package by.ntnk.msluschedule.ui.addgroup

import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.utils.COURSE_VALUE
import io.reactivex.Scheduler
import javax.inject.Inject

class AddGroupPresenter
@Inject constructor(private val networkRepository: NetworkRepository) : Presenter<AddGroupView>() {
    private val course: Int = COURSE_VALUE
    var faculty: Int = 0
        private set

    fun getFacultyScheduleFilter(uiScheduler: Scheduler) {
        networkRepository.getFaculties()
                .observeOn(uiScheduler)
                .subscribe(
                        { view!!.populateFacultiesView(it) },
                        { it.printStackTrace() }
                )
    }

    fun getScheduleGroups(uiScheduler: Scheduler) {
        networkRepository.getGroups(faculty, course)
                .observeOn(uiScheduler)
                .subscribe(
                        { view!!.populateFacultiesView(it) },
                        { it.printStackTrace() }
                )
    }
}
