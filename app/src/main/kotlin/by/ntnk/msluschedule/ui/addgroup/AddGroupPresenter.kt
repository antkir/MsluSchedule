package by.ntnk.msluschedule.ui.addgroup

import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.COURSE_VALUE
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class AddGroupPresenter
@Inject constructor(private val networkRepository: NetworkRepository) : Presenter<AddGroupView>() {
    private val disposables: CompositeDisposable = CompositeDisposable()
    private var faculties: ScheduleFilter? = null
    private var groups: ScheduleFilter? = null
    private val course: Int
        get() =
            if (group == 0 ) {
                COURSE_VALUE
            } else {
                Character.getNumericValue(groups!!.getEntry(group).value[0])
            }
    private var faculty: Int = 0
    private var group: Int = 0

    val isFacultiesNotEmpty: Boolean
        get() = faculties != null

    val isGroupsNotEmpty: Boolean
        get() = groups != null

    fun setFacultyValueFromPosition(position: Int) {
        faculty = faculties!!.keyAt(position)
    }

    fun setGroupValue(id: Long) {
        group = id.toInt()
    }

    fun getFacultyScheduleFilter(uiScheduler: Scheduler) {
        val disposable = networkRepository.getFaculties()
                .observeOn(uiScheduler)
                .subscribe(
                        {
                            faculties = it
                            populateFacultiesAdapter()
                        },
                        { it.printStackTrace() }
                )
        disposables.add(disposable)
    }

    fun getScheduleGroups(uiScheduler: Scheduler) {
        val disposable = networkRepository.getGroups(faculty, course)
                .observeOn(uiScheduler)
                .subscribe(
                        {
                            groups = it
                            populateGroupsAdapter()
                        },
                        { it.printStackTrace() }
                )
        disposables.add(disposable)
    }

    fun populateFacultiesAdapter() = view!!.populateFacultiesAdapter(faculties!!)

    fun populateGroupsAdapter() = view!!.populateGroupsAdapter(groups!!)

    fun clearDisposables() = disposables.clear()
}
