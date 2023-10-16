package by.ntnk.msluschedule.ui.addgroup

import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.db.data.ScheduleContainer
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.EMPTY_STRING
import by.ntnk.msluschedule.utils.NetworkApiVersion
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import by.ntnk.msluschedule.utils.isUnexpectedException
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import timber.log.Timber
import javax.inject.Inject

class AddGroupPresenter @Inject constructor(
    private val databaseRepository: DatabaseRepository,
    private val networkRepository: NetworkRepository,
    private val currentDate: CurrentDate,
    private val schedulerProvider: SchedulerProvider,
    private val sharedPreferencesRepository: SharedPreferencesRepository
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

    fun isFacultySet(): Boolean = faculty > 0

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
                onSuccess = { scheduleFilter ->
                    faculties = scheduleFilter
                    populateFacultiesAdapter()
                },
                onError = { throwable ->
                    if (isUnexpectedException(throwable)) {
                        Timber.e(throwable)
                    } else {
                        Timber.i(throwable)
                    }
                    view?.showError(throwable)
                }
            )
    }

    fun getCourseScheduleFilter() {
        disposables += networkRepository.getCourses(faculty)
            .subscribeOn(schedulerProvider.single())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { scheduleFilter ->
                    courses = scheduleFilter
                    populateCoursesAdapter()
                },
                onError = { throwable ->
                    if (isUnexpectedException(throwable)) {
                        Timber.e(throwable)
                    } else {
                        Timber.i(throwable)
                    }
                    view?.showError(throwable)
                }
            )
    }

    fun getScheduleGroups() {
        disposables += networkRepository.getGroups(faculty, course, currentDate.academicYear)
            .subscribeOn(schedulerProvider.single())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { scheduleFilter ->
                    groups = scheduleFilter
                    populateGroupsAdapter()
                },
                onError = { throwable ->
                    if (isUnexpectedException(throwable)) {
                        Timber.e(throwable)
                    } else {
                        Timber.i(throwable)
                    }
                    view?.showError(throwable)
                }
            )
    }

    fun setGroupValue(value: Int) {
        group = value
    }

    fun isValidGroup(name: String): Boolean {
        return groups?.containsValue(name) == true
    }

    fun isGroupStored(name: String): Boolean {
        return scheduleContaners
            ?.filter { scheduleContainer -> scheduleContainer.year == currentDate.academicYear }
            ?.filter { scheduleContainer -> scheduleContainer.faculty == faculty }
            ?.any { scheduleContaner ->
                val facultyName = faculties!!.getValue(faculty)
                scheduleContaner.name == formatGroupName(name, facultyName)
            } == true
    }

    fun getStudyGroup(): StudyGroup? {
        val facultyName = faculties!!.getValue(faculty)
        if (facultyName == EMPTY_STRING) {
            return null
        }
        val groupName = groups!!.getValue(group)
        if (groupName == EMPTY_STRING) {
            return null
        }
        val name = formatGroupName(groupName, facultyName)
        return StudyGroup(group, name, faculty, course, currentDate.academicYear)
    }

    private fun formatGroupName(groupName: String, facultyName: String): String {
        return if (sharedPreferencesRepository.getCurrentNetworkApiVersion() == NetworkApiVersion.MYUNIVERSITY) {
            "$groupName ($facultyName)"
        } else {
            groupName
        }
    }

    fun populateFacultiesAdapter() = view?.populateFacultiesAdapter(faculties!!)

    fun populateCoursesAdapter() = view?.populateCoursesAdapter(courses!!)

    fun populateGroupsAdapter() = view?.populateGroupsAdapter(groups!!)

    fun clearDisposables() = disposables.clear()
}
