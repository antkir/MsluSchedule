package by.ntnk.msluschedule.ui.main

import by.ntnk.msluschedule.TestTree
import by.ntnk.msluschedule.any
import by.ntnk.msluschedule.data.ScheduleContainer
import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.CommonError
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.DEFAULT
import by.ntnk.msluschedule.utils.ScheduleType
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.observers.TestObserver
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import timber.log.Timber
import org.mockito.Mockito.`when` as whenever

class MainPresenterTest {

    @Mock
    private lateinit var currentDate: CurrentDate

    @Mock
    private lateinit var databaseRepository: DatabaseRepository

    @Mock
    private lateinit var networkRepository: NetworkRepository

    @Mock
    private lateinit var sharedPreferencesRepository: SharedPreferencesRepository

    @Mock
    private lateinit var schedulerProvider: SchedulerProvider

    private lateinit var presenter: MainPresenter

    private val insertStudyGroupTest = TestObserver.create<Int>()
    private val insertTeacherTest = TestObserver.create<Int>()
    private val getWeeksTest = TestObserver.create<ScheduleFilter>()
    private val getScheduleContainersReactiveTest = TestObserver.create<ScheduleContainer>()
    private val insertWeeksGetIdsTest = TestObserver.create<Int>()
    private val deleteScheduleContainerTest = TestObserver.create<Void>()

    @Before
    fun setUp() {
        Timber.plant(TestTree())
        MockitoAnnotations.openMocks(this)

        whenever(currentDate.academicYear).thenReturn(ACADEMIC_YEAR)

        whenever(sharedPreferencesRepository.getSelectedScheduleContainer()).thenReturn(SELECTED_SCHEDULE_CONTAINER)

        whenever(networkRepository.getWeeks())
            .thenReturn(
                Single.just(ScheduleFilter()).doOnSubscribe { getWeeksTest.onSubscribe(it) }
            )

        whenever(databaseRepository.getScheduleContainersReactive())
            .thenReturn(
                Observable.just(emptyList<ScheduleContainer>())
                    .doOnSubscribe { getScheduleContainersReactiveTest.onSubscribe(it) }
            )
        whenever(databaseRepository.insertStudyGroup(any()))
            .thenReturn(
                Single.just(0).doOnSubscribe { insertStudyGroupTest.onSubscribe(it) }
            )
        whenever(databaseRepository.insertTeacher(any()))
            .thenReturn(
                Single.just(0).doOnSubscribe { insertTeacherTest.onSubscribe(it) }
            )
        whenever(databaseRepository.insertWeeksGetIds(any(), eq(0)))
            .thenReturn(
                Observable.just(0).doOnSubscribe { insertWeeksGetIdsTest.onSubscribe(it) }
            )
        whenever(databaseRepository.deleteScheduleContainer(eq(0)))
            .thenReturn(
                Completable.complete().doOnSubscribe { deleteScheduleContainerTest.onSubscribe(it) }
            )

        whenever(schedulerProvider.io())
            .thenReturn(Schedulers.trampoline())
        whenever(schedulerProvider.single())
            .thenReturn(Schedulers.trampoline())
        whenever(schedulerProvider.ui())
            .thenReturn(Schedulers.trampoline())

        presenter = MainPresenter(
            currentDate,
            databaseRepository,
            networkRepository,
            sharedPreferencesRepository,
            schedulerProvider
        )
    }

    @Test
    fun addGroup() {
        // given
        val studyGroup = StudyGroup(
            id = 0,
            key = String.DEFAULT,
            value = String.DEFAULT,
            year = ACADEMIC_YEAR,
            faculty = 0,
            course = 0
        )
        val scheduleContainer =
            ScheduleContainer(studyGroup.id, studyGroup.value, ScheduleType.STUDYGROUP, studyGroup.year)
        whenever(sharedPreferencesRepository.getSelectedScheduleContainer()).thenReturn(scheduleContainer)
        val uiStateObserver = presenter.uiStateObservable.test()
        // when
        presenter.addGroup(studyGroup)
        // then
        verify(networkRepository).getWeeks()
        verify(databaseRepository).insertStudyGroup(any())
        verify(databaseRepository).insertWeeksGetIds(any(), eq(0))
        verify(sharedPreferencesRepository).setSelectedScheduleContainer(any())
        uiStateObserver.assertNoErrors()
        uiStateObserver.assertValues(
            MainUiState(selectedScheduleContainer = SELECTED_SCHEDULE_CONTAINER),
            MainUiState(selectedScheduleContainer = SELECTED_SCHEDULE_CONTAINER, isLoading = true),
            MainUiState(selectedScheduleContainer = scheduleContainer)
        )
    }

    @Test
    fun addTeacher() {
        // given
        val teacher = Teacher(id = 0, key = String.DEFAULT, value = String.DEFAULT, year = ACADEMIC_YEAR)
        val scheduleContainer =
            ScheduleContainer(teacher.id, teacher.value, ScheduleType.TEACHER, teacher.year)
        whenever(sharedPreferencesRepository.getSelectedScheduleContainer()).thenReturn(scheduleContainer)
        val uiStateObserver = presenter.uiStateObservable.test()
        // when
        presenter.addTeacher(teacher)
        // then
        verify(networkRepository).getWeeks()
        verify(databaseRepository).insertTeacher(any())
        verify(databaseRepository).insertWeeksGetIds(any(), eq(0))
        verify(sharedPreferencesRepository).setSelectedScheduleContainer(any())
        uiStateObserver.assertNoErrors()
        uiStateObserver.assertValues(
            MainUiState(selectedScheduleContainer = SELECTED_SCHEDULE_CONTAINER),
            MainUiState(selectedScheduleContainer = SELECTED_SCHEDULE_CONTAINER, isLoading = true),
            MainUiState(selectedScheduleContainer = scheduleContainer)
        )
    }

    @Test
    fun `Delete the inserted group if we aren't able to get weeks`() {
        // given
        val studyGroup = StudyGroup(
            id = 0,
            key = String.DEFAULT,
            value = String.DEFAULT,
            year = ACADEMIC_YEAR,
            faculty = 0,
            course = 0
        )
        whenever(networkRepository.getWeeks()).thenReturn(Single.error(NullPointerException()))
        val uiStateObserver = presenter.uiStateObservable.test()
        // when
        presenter.addGroup(studyGroup)
        // then
        verify(networkRepository).getWeeks()
        verify(databaseRepository).deleteScheduleContainer(eq(0))
        verify(sharedPreferencesRepository, times(0)).setSelectedScheduleContainer(any())
        uiStateObserver.assertNoErrors()
        uiStateObserver.assertValues(
            MainUiState(selectedScheduleContainer = SELECTED_SCHEDULE_CONTAINER),
            MainUiState(selectedScheduleContainer = SELECTED_SCHEDULE_CONTAINER, isLoading = true),
            MainUiState(
                selectedScheduleContainer = SELECTED_SCHEDULE_CONTAINER,
                errorEvent = ErrorUiEvent(error = CommonError)
            )
        )
    }

    @Test
    fun `Delete the inserted teacher if we aren't able to get weeks`() {
        // given
        val teacher = Teacher(id = 0, key = String.DEFAULT, value = String.DEFAULT, year = ACADEMIC_YEAR)
        whenever(networkRepository.getWeeks()).thenReturn(Single.error(NullPointerException()))
        val uiStateObserver = presenter.uiStateObservable.test()
        // when
        presenter.addTeacher(teacher)
        // then
        verify(networkRepository).getWeeks()
        verify(databaseRepository).deleteScheduleContainer(eq(0))
        verify(sharedPreferencesRepository, times(0)).setSelectedScheduleContainer(any())
        uiStateObserver.assertNoErrors()
        uiStateObserver.assertValues(
            MainUiState(selectedScheduleContainer = SELECTED_SCHEDULE_CONTAINER),
            MainUiState(selectedScheduleContainer = SELECTED_SCHEDULE_CONTAINER, isLoading = true),
            MainUiState(
                selectedScheduleContainer = SELECTED_SCHEDULE_CONTAINER,
                errorEvent = ErrorUiEvent(error = CommonError)
            )
        )
    }

    companion object {
        private const val ACADEMIC_YEAR = 2023
        private val SELECTED_SCHEDULE_CONTAINER = ScheduleContainer.DEFAULT.copy(year = ACADEMIC_YEAR)
    }
}
