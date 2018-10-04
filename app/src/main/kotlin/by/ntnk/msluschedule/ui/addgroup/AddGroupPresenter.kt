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
import timber.log.Timber
import javax.inject.Inject

class AddGroupPresenter @Inject constructor(
        private val databaseRepository: DatabaseRepository,
        private val networkRepository: NetworkRepository,
        private val currentDate: CurrentDate,
        private val schedulerProvider: SchedulerProvider
) : Presenter<AddGroupView>() {
    private val disposables: CompositeDisposable = CompositeDisposable()
    private var faculties: ScheduleFilter? = null
    private var courses: ScheduleFilter? = null
    private var groups: ScheduleFilter? = null
    private var faculty: Int = 0
    private var course: Int = 0
    private var group: Int = 0

    private var scheduleContaners: List<ScheduleContainer>? = null

    fun isFacultiesInitialized(): Boolean = faculties != null

    fun isCoursesInitialized(): Boolean = courses != null

    fun isGroupsInitialized(): Boolean = groups != null

    fun isFacultySet(): Boolean = faculty != 0

    fun isCourseSet(): Boolean = course > 0

    fun setFacultyKeyFromPosition(position: Int) {
        faculty = faculties!!.keyAt(position)
    }

    fun setCourseKeyFromPosition(position: Int) {
        course = courses!!.keyAt(position)
    }

    fun setCoursesNull() {
        courses = null
        course = 0
    }

    fun setGroupsNull() {
        groups = null
        group = 0
    }

    fun getFacultyScheduleFilter() {
        databaseRepository.getScheduleContainers()
                .toList()
                .subscribeOn(schedulerProvider.io())
                .subscribeBy(
                        onSuccess = { scheduleContaners = it },
                        onError = { throwable -> Timber.e(throwable) }
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

    private fun getCourseScheduleFilter() {
        disposables += networkRepository.getCourses()
                .subscribeOn(schedulerProvider.single())
                .observeOn(schedulerProvider.ui())
                .subscribeBy(
                        onSuccess = {
                            courses = it
                            populateCoursesAdapter()
                        },
                        onError = {
                            it.printStackTrace()
                            view?.showError(it)
                        }
                )
    }

    fun getScheduleGroups(showGroupsForAllCourses: Boolean = true) {
        val courseKey = if (course > 0) course else COURSE_VALUE
        val year = if (!showGroupsForAllCourses) currentDate.academicYear else 0
        disposables += networkRepository.getGroups(faculty, courseKey, year)
                .subscribeOn(schedulerProvider.single())
                .observeOn(schedulerProvider.ui())
                .subscribeBy(
                        onSuccess = {
                            if (it.size > 0
                                    && course == 0
                                    && !it.valueAt(0).first().isDigit()
                                    && !it.valueAt(1).first().isDigit()) {
                                getCourseScheduleFilter()
                            } else {
                                groups = it
                                populateGroupsAdapter()
                            }
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
                ?.filter { it.year == currentDate.academicYear }
                ?.map { it.name }
                ?.any { it == string } == true
    }

    fun getStudyGroup(): StudyGroup {
        val courseKey = if (course > 0) {
            course
        } else {
            Character.getNumericValue(groups!!.getEntry(group).value[0])
        }
        return StudyGroup(group, groups!!.getValue(group), faculty, courseKey, currentDate.academicYear)
    }

    fun populateFacultiesAdapter() = view?.populateFacultiesAdapter(faculties!!)

    fun populateCoursesAdapter() = view?.populateCoursesAdapter(courses!!)

    fun populateGroupsAdapter() = view?.populateGroupsAdapter(groups!!)

    fun clearDisposables() = disposables.clear()
}
