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
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
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
        get() = Character.getNumericValue(groups!!.getEntry(group).value[0])
    private var faculty: Int = 0
    private var group: Int = 0

    private lateinit var scheduleContaners: List<ScheduleContainer>

    fun isFacultiesNotEmpty(): Boolean = faculties != null

    fun isGroupsNotEmpty(): Boolean = groups != null

    fun isFacultySelected(): Boolean = faculty != 0

    fun setFacultyValueFromPosition(position: Int) {
        faculty = faculties!!.keyAt(position)
    }

    fun getFacultyScheduleFilter() {
        databaseRepository.getScheduleContainers()
                .toList()
                .subscribeOn(schedulerProvider.io())
                .subscribeBy(
                        onSuccess = { scheduleContaners = it },
                        onError = { it.printStackTrace() }
                )

        disposables += networkRepository.getFaculties()
                .subscribeOn(schedulerProvider.single())
                .observeOn(schedulerProvider.ui())
                .subscribeBy(
                        onSuccess = {
                            faculties = it
                            populateFacultiesAdapter()
                        },
                        onError = {
                            it.printStackTrace()
                            view?.showError(it)
                        }
                )
    }

    fun getScheduleGroups() {
        disposables += networkRepository.getGroups(faculty, COURSE_VALUE)
                .subscribeOn(schedulerProvider.single())
                .observeOn(schedulerProvider.ui())
                .subscribeBy(
                        onSuccess = {
                            groups = it
                            populateGroupsAdapter()
                        },
                        onError = {
                            it.printStackTrace()
                            view?.showError(it)
                        }
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

    fun getStudyGroup() = StudyGroup(group, groups!!.getValue(group), faculty, course, currentDate.academicYear)

    fun populateFacultiesAdapter() = view?.populateFacultiesAdapter(faculties!!)

    fun populateGroupsAdapter() = view?.populateGroupsAdapter(groups!!)

    fun clearDisposables() = disposables.clear()
}
