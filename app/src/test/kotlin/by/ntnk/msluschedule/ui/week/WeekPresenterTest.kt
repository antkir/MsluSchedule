package by.ntnk.msluschedule.ui.week

import by.ntnk.msluschedule.TestTree
import by.ntnk.msluschedule.any
import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.Note
import by.ntnk.msluschedule.data.ScheduleContainer
import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.data.WeekdayWithLessons
import by.ntnk.msluschedule.data.WeekdayWithStudyGroupLessons
import by.ntnk.msluschedule.data.WeekdayWithTeacherLessons
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.DEFAULT
import by.ntnk.msluschedule.utils.ScheduleType
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import by.ntnk.msluschedule.utils.Utils
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import timber.log.Timber
import org.mockito.Mockito.`when` as whenever

class WeekPresenterTest {

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

    @Mock
    private lateinit var view: WeekView

    private lateinit var presenter: WeekPresenter

    private val isWeekInitializedTest = TestObserver.create<Boolean>()
    private val getStudyGroupTest = TestObserver.create<ScheduleContainer>()
    private val getWeekKeyTest = TestObserver.create<Int>()
    private val deleteLessonsForWeekTest = TestObserver.create<Void>()
    private val getWeekdayWithStudyGroupLessonsForWeekTest = TestObserver.create<WeekdayWithLessons<Lesson>>()
    private val getWeekdayWithTeacherLessonsForWeekTest = TestObserver.create<WeekdayWithLessons<Lesson>>()
    private val insertStudyGroupScheduleTest = TestObserver.create<List<WeekdayWithStudyGroupLessons>>()
    private val insertTeacherScheduleTest = TestObserver.create<List<WeekdayWithTeacherLessons>>()
    private val downloadStudyGroupScheduleTest = TestObserver.create<List<WeekdayWithStudyGroupLessons>>()
    private val downloadTeacherScheduleTest = TestObserver.create<List<WeekdayWithTeacherLessons>>()
    private val insertWeekdaysTest = TestObserver.create<Int>()
    private val getNotesForWeekdayTest = TestObserver.create<Int>()

    @Before
    fun setUp() {
        Timber.plant(TestTree())
        MockitoAnnotations.openMocks(this)

        whenever(sharedPreferencesRepository.getSelectedScheduleContainer())
            .thenReturn(ScheduleContainer(id = 0, value = String.DEFAULT, ScheduleType.STUDYGROUP, year = 0))
        whenever(databaseRepository.isWeekInitialized(anyInt()))
            .thenReturn(
                Single.just(true)
                    .doOnSubscribe { disposable -> isWeekInitializedTest.onSubscribe(disposable) }
            )
        whenever(databaseRepository.getStudyGroup(anyInt()))
            .thenReturn(
                Single.just(
                    StudyGroup(
                        id = 0,
                        key = String.DEFAULT,
                        value = String.DEFAULT,
                        year = 0,
                        faculty = 0,
                        course = 0
                    )
                ).doOnSubscribe { disposable -> getStudyGroupTest.onSubscribe(disposable) }
            )
        whenever(databaseRepository.getWeekKey(anyInt()))
            .thenReturn(
                Single.just(0)
                    .doOnSubscribe { disposable -> getWeekKeyTest.onSubscribe(disposable) }
            )
        whenever(databaseRepository.deleteLessons(anyInt(), any()))
            .thenReturn(
                Completable.complete()
                    .doOnSubscribe { disposable -> deleteLessonsForWeekTest.onSubscribe(disposable) }
            )
        whenever(databaseRepository.getWeekdaysWithStudyGroupLessons(anyInt()))
            .thenReturn(
                Observable.just(
                    WeekdayWithStudyGroupLessons(id = 0, value = String.DEFAULT) as WeekdayWithLessons<Lesson>
                )
                    .doOnSubscribe { disposable -> getWeekdayWithStudyGroupLessonsForWeekTest.onSubscribe(disposable) }
            )
        whenever(databaseRepository.getWeekdaysWithTeacherLessons(anyInt()))
            .thenReturn(
                Observable.just(
                    WeekdayWithTeacherLessons(id = 0, value = String.DEFAULT) as WeekdayWithLessons<Lesson>
                )
                    .doOnSubscribe { disposable -> getWeekdayWithTeacherLessonsForWeekTest.onSubscribe(disposable) }
            )
        whenever(databaseRepository.insertStudyGroupSchedule(any(), anyInt()))
            .thenReturn(
                Single.just(listOf(WeekdayWithStudyGroupLessons(id = 0, value = String.DEFAULT)))
                    .doOnSubscribe { disposable -> insertStudyGroupScheduleTest.onSubscribe(disposable) }
            )
        whenever(databaseRepository.insertTeacherSchedule(any(), anyInt()))
            .thenReturn(
                Single.just(listOf(WeekdayWithTeacherLessons(id = 0, value = String.DEFAULT)))
                    .doOnSubscribe { disposable -> insertTeacherScheduleTest.onSubscribe(disposable) }
            )
        whenever(databaseRepository.insertWeekdays(anyInt()))
            .thenReturn(
                Completable.complete()
                    .doOnSubscribe { disposable -> insertWeekdaysTest.onSubscribe(disposable) }
            )
        whenever(databaseRepository.getNotesForWeekday(anyInt()))
            .thenReturn(
                Observable.just(listOf(Note(id = 0, text = String.DEFAULT, subject = String.DEFAULT)))
                    .doOnSubscribe { disposable -> getNotesForWeekdayTest.onSubscribe(disposable) }
            )
        whenever(networkRepository.getSchedule(any<StudyGroup>(), anyInt()))
            .thenReturn(
                Observable.just(WeekdayWithStudyGroupLessons(id = 0, value = String.DEFAULT))
                    .doOnSubscribe { disposable -> downloadStudyGroupScheduleTest.onSubscribe(disposable) }
            )
        whenever(networkRepository.getSchedule(any<Teacher>(), anyInt()))
            .thenReturn(
                Observable.just(WeekdayWithTeacherLessons(id = 0, value = String.DEFAULT))
                    .doOnSubscribe { disposable -> downloadTeacherScheduleTest.onSubscribe(disposable) }
            )

        whenever(schedulerProvider.io())
            .thenReturn(Schedulers.trampoline())
        whenever(schedulerProvider.single())
            .thenReturn(Schedulers.trampoline())
        whenever(schedulerProvider.ui())
            .thenReturn(Schedulers.trampoline())

        presenter = WeekPresenter(
            currentDate,
            databaseRepository,
            sharedPreferencesRepository,
            schedulerProvider,
            networkRepository
        )
        presenter.bindView(view)
    }

    @Test
    fun `first getData call for StudyGroup`() {
        // given
        val scheduleContainer =
            ScheduleContainer(id = 0, value = String.DEFAULT, type = ScheduleType.STUDYGROUP, year = 0)
        val weekId = 0
        whenever(databaseRepository.isWeekInitialized(anyInt()))
            .thenReturn(
                Single.just(false)
                    .doOnSubscribe { isWeekInitializedTest.onSubscribe(it) }
            )
        val uiStateObserver = presenter.uiStateObservable.test()
        // when
        presenter.getData(scheduleContainer, weekId)
        // then
        isWeekInitializedTest.assertSubscribed()
        insertWeekdaysTest.assertSubscribed()
        getStudyGroupTest.assertSubscribed()
        getWeekKeyTest.assertSubscribed()
        downloadStudyGroupScheduleTest.assertSubscribed()
        deleteLessonsForWeekTest.assertSubscribed()
        insertStudyGroupScheduleTest.assertSubscribed()
        uiStateObserver.assertSubscribed()
        uiStateObserver.assertNoErrors()
        uiStateObserver.assertValues(
            WeekUiState(),
            WeekUiState(isUiStateInitialized = true),
            WeekUiState(
                isUiStateInitialized = true,
                isLoading = true
            ),
            WeekUiState(
                isUiStateInitialized = true,
                dataState = DataState(
                    data = listOf(WeekdayWithStudyGroupLessons(id = 0, value = String.DEFAULT))
                )
            )
        )
    }

    @Test
    fun `first getData call for StudyGroup with database error during week initialization`() {
        // given
        val scheduleContainer =
            ScheduleContainer(id = 0, value = String.DEFAULT, type = ScheduleType.STUDYGROUP, year = 0)
        val weekId = 0
        val expectedThrowable = NullPointerException()
        val error = Utils.getErrorType(expectedThrowable)
        whenever(databaseRepository.isWeekInitialized(anyInt()))
            .thenReturn(
                Single.just(false)
                    .doOnSubscribe { disposable -> isWeekInitializedTest.onSubscribe(disposable) }
            )
        whenever(databaseRepository.insertWeekdays(anyInt()))
            .thenReturn(
                Completable.error(expectedThrowable)
                    .doOnSubscribe { disposable -> insertWeekdaysTest.onSubscribe(disposable) }
                    .doOnError { throwable -> insertWeekdaysTest.onError(throwable) }
            )
        val uiStateObserver = presenter.uiStateObservable.test()
        // when
        presenter.getData(scheduleContainer, weekId)
        // then
        isWeekInitializedTest.assertSubscribed()
        insertWeekdaysTest.assertSubscribed()
        insertWeekdaysTest.assertError(expectedThrowable)
        uiStateObserver.assertSubscribed()
        uiStateObserver.assertNoErrors()
        uiStateObserver.assertValues(
            WeekUiState(),
            WeekUiState(isUiStateInitialized = true),
            WeekUiState(
                isUiStateInitialized = true,
                isLoading = true
            ),
            WeekUiState(
                isUiStateInitialized = true,
                errorEvent = ErrorUiEvent(error = error)
            )
        )
    }

    @Test
    fun `first getData call for StudyGroup with network error during initial update`() {
        // given
        val scheduleContainer =
            ScheduleContainer(id = 0, value = String.DEFAULT, type = ScheduleType.STUDYGROUP, year = 0)
        val weekId = 0
        val expectedThrowable = NullPointerException()
        val error = Utils.getErrorType(expectedThrowable)
        whenever(databaseRepository.isWeekInitialized(anyInt()))
            .thenReturn(
                Single.just(false)
                    .doOnSubscribe { isWeekInitializedTest.onSubscribe(it) }
            )
        whenever(networkRepository.getSchedule(any<StudyGroup>(), anyInt()))
            .thenReturn(
                Observable.error<WeekdayWithStudyGroupLessons>(expectedThrowable)
                    .doOnSubscribe { disposable -> downloadStudyGroupScheduleTest.onSubscribe(disposable) }
                    .doOnError { throwable -> downloadStudyGroupScheduleTest.onError(throwable) }
            )
        val uiStateObserver = presenter.uiStateObservable.test()
        // when
        presenter.getData(scheduleContainer, weekId)
        // then
        isWeekInitializedTest.assertSubscribed()
        insertWeekdaysTest.assertSubscribed()
        getStudyGroupTest.assertSubscribed()
        getWeekKeyTest.assertSubscribed()
        downloadStudyGroupScheduleTest.assertSubscribed()
        downloadStudyGroupScheduleTest.assertError(expectedThrowable)
        uiStateObserver.assertSubscribed()
        uiStateObserver.assertNoErrors()
        uiStateObserver.assertValues(
            WeekUiState(),
            WeekUiState(isUiStateInitialized = true),
            WeekUiState(
                isUiStateInitialized = true,
                isLoading = true
            ),
            WeekUiState(
                isUiStateInitialized = true,
                isLoading = true,
                errorEvent = ErrorUiEvent(error = error)
            ),
            WeekUiState(
                isUiStateInitialized = true,
                dataState = DataState(
                    data = listOf(WeekdayWithStudyGroupLessons(id = 0, value = String.DEFAULT))
                ),
                errorEvent = ErrorUiEvent(error = error)
            )
        )
    }

    @Test
    fun `getData call for StudyGroup with initialized week`() {
        // given
        val scheduleContainer =
            ScheduleContainer(id = 0, value = String.DEFAULT, type = ScheduleType.STUDYGROUP, year = 0)
        val weekId = 0
        val uiStateObserver = presenter.uiStateObservable.test()
        // when
        presenter.getData(scheduleContainer, weekId)
        // then
        isWeekInitializedTest.assertSubscribed()
        insertWeekdaysTest.assertNotSubscribed()
        uiStateObserver.assertSubscribed()
        uiStateObserver.assertNoErrors()
        uiStateObserver.assertValues(
            WeekUiState(),
            WeekUiState(isUiStateInitialized = true),
            WeekUiState(
                isUiStateInitialized = true,
                dataState = DataState(
                    data = listOf(WeekdayWithStudyGroupLessons(id = 0, value = String.DEFAULT))
                )
            )
        )
    }

    @Test
    fun `getData call for StudyGroup with initialized week with error`() {
        // given
        val scheduleContainer =
            ScheduleContainer(id = 0, value = String.DEFAULT, type = ScheduleType.STUDYGROUP, year = 0)
        val weekId = 0
        val expectedThrowable = NullPointerException()
        val error = Utils.getErrorType(expectedThrowable)
        whenever(databaseRepository.getWeekdaysWithStudyGroupLessons(anyInt()))
            .thenReturn(
                Observable.error<WeekdayWithLessons<Lesson>>(expectedThrowable)
                    .doOnSubscribe { disposable -> getWeekdayWithStudyGroupLessonsForWeekTest.onSubscribe(disposable) }
                    .doOnError { throwable -> getWeekdayWithStudyGroupLessonsForWeekTest.onError(throwable) }
            )
        val uiStateObserver = presenter.uiStateObservable.test()
        // when
        presenter.getData(scheduleContainer, weekId)
        // then
        getWeekdayWithStudyGroupLessonsForWeekTest.assertSubscribed()
        getWeekdayWithStudyGroupLessonsForWeekTest.assertError(expectedThrowable)
        uiStateObserver.assertSubscribed()
        uiStateObserver.assertNoErrors()
        uiStateObserver.assertValues(
            WeekUiState(),
            WeekUiState(isUiStateInitialized = true),
            WeekUiState(
                isUiStateInitialized = true,
                errorEvent = ErrorUiEvent(error = error)
            )
        )
    }

    @Test
    fun `updateSchedule call for StudyGroup`() {
        // given
        val scheduleContainer =
            ScheduleContainer(id = 0, value = String.DEFAULT, type = ScheduleType.STUDYGROUP, year = 0)
        val weekId = 0
        val uiStateObserver = presenter.uiStateObservable.test()
        // when
        presenter.updateSchedule(scheduleContainer, weekId)
        // then
        getStudyGroupTest.assertSubscribed()
        getWeekKeyTest.assertSubscribed()
        downloadStudyGroupScheduleTest.assertSubscribed()
        deleteLessonsForWeekTest.assertSubscribed()
        insertStudyGroupScheduleTest.assertSubscribed()
        uiStateObserver.assertSubscribed()
        uiStateObserver.assertNoErrors()
        uiStateObserver.assertValues(
            WeekUiState(),
            WeekUiState(isUpdating = true),
            WeekUiState(
                dataState = DataState(
                    data = listOf(WeekdayWithStudyGroupLessons(id = 0, value = String.DEFAULT))
                ),
                successfulScheduleUpdateEvent = UiEvent(isTriggered = true)
            )
        )
    }

    @Test
    fun `updateSchedule call for StudyGroup with network error`() {
        // given
        val scheduleContainer =
            ScheduleContainer(id = 0, value = String.DEFAULT, type = ScheduleType.STUDYGROUP, year = 0)
        val weekId = 0
        val expectedThrowable = NullPointerException()
        val error = Utils.getErrorType(expectedThrowable)
        whenever(networkRepository.getSchedule(any<StudyGroup>(), anyInt()))
            .thenReturn(
                Observable.error<WeekdayWithStudyGroupLessons>(expectedThrowable)
                    .doOnSubscribe { disposable -> downloadStudyGroupScheduleTest.onSubscribe(disposable) }
                    .doOnError { throwable -> downloadStudyGroupScheduleTest.onError(throwable) }
            )
        val uiStateObserver = presenter.uiStateObservable.test()
        // when
        presenter.updateSchedule(scheduleContainer, weekId)
        // then
        getStudyGroupTest.assertSubscribed()
        getWeekKeyTest.assertSubscribed()
        downloadStudyGroupScheduleTest.assertSubscribed()
        downloadStudyGroupScheduleTest.assertError(expectedThrowable)
        uiStateObserver.assertSubscribed()
        uiStateObserver.assertNoErrors()
        uiStateObserver.assertValues(
            WeekUiState(),
            WeekUiState(isUpdating = true),
            WeekUiState(
                errorEvent = ErrorUiEvent(
                    isScheduleUpdateError = true,
                    error = error
                )
            )
        )
    }
}
