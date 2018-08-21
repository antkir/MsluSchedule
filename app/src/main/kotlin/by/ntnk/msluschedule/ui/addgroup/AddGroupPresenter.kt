package by.ntnk.msluschedule.ui.addgroup

import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.db.data.ScheduleContainer
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.COURSE_VALUE
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.plusAssign
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class AddGroupPresenter @Inject constructor(
        private val databaseRepository: DatabaseRepository,
        private val networkRepository: NetworkRepository,
        private val currentDate: CurrentDate,
        private val schedulerProvider: SchedulerProvider
) : Presenter<AddGroupView>() {
    private val disposables: CompositeDisposable = CompositeDisposable()
    private var faculties: ScheduleFilter? = null
    private var groups: ScheduleFilter? = null
    private val course: Int
        get() =
            if (group == 0) {
                COURSE_VALUE
            } else {
                Character.getNumericValue(groups!!.getEntry(group).value[0])
            }
    private var faculty: Int = 0
    private var group: Int = 0

    private lateinit var scheduleContaners: List<ScheduleContainer>

    val isFacultiesNotEmpty: Boolean
        get() = faculties != null

    val isGroupsNotEmpty: Boolean
        get() = groups != null

    val isFacultySelected: Boolean
        get() = faculty != 0

    fun setFacultyValueFromPosition(position: Int) {
        faculty = faculties!!.keyAt(position)
    }

    fun getFacultyScheduleFilter() {
        databaseRepository.getScheduleContainers()
                .toList()
                .subscribeOn(schedulerProvider.io())
                .subscribe(
                        { scheduleContaners = it },
                        { it.printStackTrace() }
                )

        disposables += networkRepository.getFaculties()
                .subscribeOn(schedulerProvider.single())
                .observeOn(schedulerProvider.ui())
                .subscribe(
                        {
                            faculties = it
                            populateFacultiesAdapter()
                        },
                        { it.printStackTrace() }
                )
    }

    fun getScheduleGroups() {
        disposables += networkRepository.getGroups(faculty, course)
                .subscribeOn(schedulerProvider.single())
                .observeOn(schedulerProvider.ui())
                .subscribe(
                        {
                            groups = it
                            populateGroupsAdapter()
                        },
                        { it.printStackTrace() }
                )
    }

    fun setGroupValue(value: Int) {
        group = value
    }

    fun isValidGroup(string: String): Boolean {
        return groups?.containsValue(string) == true
    }

    fun isGroupStored(string: String): Boolean {
        return scheduleContaners
                .filter { it.year == currentDate.academicYear }
                .map { it.name }
                .any { it == string }
    }

    fun getStudyGroup(): StudyGroup =
            StudyGroup(group, groups!!.getValue(group), faculty, course, currentDate.academicYear)

    fun populateFacultiesAdapter() = view!!.populateFacultiesAdapter(faculties!!)

    fun populateGroupsAdapter() = view!!.populateGroupsAdapter(groups!!)

    fun clearDisposables() = disposables.clear()
}
