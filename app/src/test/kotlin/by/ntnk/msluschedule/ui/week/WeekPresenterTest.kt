package by.ntnk.msluschedule.ui.week

import by.ntnk.msluschedule.TestTree
import by.ntnk.msluschedule.any
import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.Note
import by.ntnk.msluschedule.data.ScheduleContainerInfo
import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.data.WeekdayWithLessons
import by.ntnk.msluschedule.data.WeekdayWithStudyGroupLessons
import by.ntnk.msluschedule.data.WeekdayWithTeacherLessons
import by.ntnk.msluschedule.db.DatabaseDataMapper
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.db.data.ScheduleContainer
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.network.api.original.NetworkRepository as NetworkRepositoryOriginal
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.DEFAULT
import by.ntnk.msluschedule.utils.ScheduleType
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import timber.log.Timber
import org.mockito.Mockito.`when` as whenever

class WeekPresenterTest {

    @Mock
    private lateinit var currentDate: CurrentDate

    @Mock
    private lateinit var databaseRepository: DatabaseRepository

    @Mock
    private lateinit var databaseDataMapper: DatabaseDataMapper

    @Mock
    private lateinit var networkRepositoryLazy: dagger.Lazy<NetworkRepository>

    private val networkRepository: NetworkRepository
        get() = networkRepositoryLazy.get()

    @Mock
    private lateinit var sharedPreferencesRepository: SharedPreferencesRepository

    @Mock
    private lateinit var schedulerProvider: SchedulerProvider

    @Mock
    private lateinit var view: WeekView

    private lateinit var presenter: WeekPresenter

    private val isWeekInitializedTest = TestObserver.create<Boolean>()
    private val getScheduleContainerTest = TestObserver.create<ScheduleContainer>()
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

        whenever(networkRepositoryLazy.get()).thenReturn(mock(NetworkRepositoryOriginal::class.java))
        whenever(sharedPreferencesRepository.getSelectedScheduleContainerInfo())
            .thenReturn(ScheduleContainerInfo(id = 0, value = String.DEFAULT, ScheduleType.STUDYGROUP))
        whenever(databaseRepository.isWeekInitialized(anyInt()))
            .thenReturn(
                Single.just(true)
                    .doOnSubscribe { disposable -> isWeekInitializedTest.onSubscribe(disposable) }
            )
        whenever(databaseRepository.getScheduleContainer(anyInt()))
            .thenReturn(
                Single.just(ScheduleContainer(key = 0, name = String.DEFAULT, ScheduleType.STUDYGROUP, year = 0))
                    .doOnSubscribe { disposable -> getScheduleContainerTest.onSubscribe(disposable) }
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
                Observable.just(WeekdayWithStudyGroupLessons(weekday = String.DEFAULT) as WeekdayWithLessons<Lesson>)
                    .doOnSubscribe { disposable -> getWeekdayWithStudyGroupLessonsForWeekTest.onSubscribe(disposable) }
            )
        whenever(databaseRepository.getWeekdaysWithTeacherLessons(anyInt()))
            .thenReturn(
                Observable.just(WeekdayWithTeacherLessons(weekday = String.DEFAULT) as WeekdayWithLessons<Lesson>)
                    .doOnSubscribe { disposable -> getWeekdayWithTeacherLessonsForWeekTest.onSubscribe(disposable) }
            )
        whenever(databaseRepository.insertStudyGroupSchedule(any(), anyInt()))
            .thenReturn(
                Single.just(listOf(WeekdayWithStudyGroupLessons(weekday = String.DEFAULT)))
                    .doOnSubscribe { disposable -> insertStudyGroupScheduleTest.onSubscribe(disposable) }
            )
        whenever(databaseRepository.insertTeacherSchedule(any(), anyInt()))
            .thenReturn(
                Single.just(listOf(WeekdayWithTeacherLessons(weekday = String.DEFAULT)))
                    .doOnSubscribe { disposable -> insertTeacherScheduleTest.onSubscribe(disposable) }
            )
        whenever(databaseRepository.insertWeekdays(anyInt()))
            .thenReturn(
                Completable.complete()
                    .doOnSubscribe { disposable -> insertWeekdaysTest.onSubscribe(disposable) }
            )
        whenever(databaseRepository.getNotesForWeekday(anyInt()))
            .thenReturn(
                Observable.just(Note(id = 0, text = String.DEFAULT, subject = String.DEFAULT))
                    .doOnSubscribe { disposable -> getNotesForWeekdayTest.onSubscribe(disposable) }
            )
        whenever(networkRepository.getSchedule(any<StudyGroup>(), anyInt()))
            .thenReturn(
                Observable.just(WeekdayWithStudyGroupLessons(weekday = String.DEFAULT))
                    .doOnSubscribe { disposable -> downloadStudyGroupScheduleTest.onSubscribe(disposable) }
            )
        whenever(networkRepository.getSchedule(any<Teacher>(), anyInt()))
            .thenReturn(
                Observable.just(WeekdayWithTeacherLessons(weekday = String.DEFAULT))
                    .doOnSubscribe { disposable -> downloadTeacherScheduleTest.onSubscribe(disposable) }
            )
        whenever(databaseDataMapper.mapToStudyGroup(any()))
            .thenReturn(StudyGroup(key = 0, name = String.DEFAULT, faculty = 0, course = 0, year = 0))
        whenever(databaseDataMapper.mapToTeacher(any()))
            .thenReturn(Teacher(key = 0, name = String.DEFAULT, year = 0))

        whenever(schedulerProvider.io())
            .thenReturn(Schedulers.trampoline())
        whenever(schedulerProvider.single())
            .thenReturn(Schedulers.trampoline())
        whenever(schedulerProvider.ui())
            .thenReturn(Schedulers.trampoline())

        presenter = WeekPresenter(
            currentDate,
            databaseRepository,
            databaseDataMapper,
            sharedPreferencesRepository,
            schedulerProvider,
            networkRepositoryLazy
        )
        presenter.bindView(view)
    }

    @Test
    fun `first checkWeekInitialized call for StudyGroup`() {
        // given
        val weekId = 0
        whenever(databaseRepository.isWeekInitialized(anyInt()))
            .thenReturn(
                Single.just(false)
                    .doOnSubscribe { isWeekInitializedTest.onSubscribe(it) }
            )
        val scheduleUiStateObserver = presenter.scheduleUiStateObservable.test()
        // when
        presenter.checkWeekInitialized(weekId)
        // then
        isWeekInitializedTest.assertSubscribed()
        insertWeekdaysTest.assertSubscribed()
        scheduleUiStateObserver.assertSubscribed()
        scheduleUiStateObserver.assertNoErrors()
        scheduleUiStateObserver.assertValues(
            ScheduleUiState(),
            ScheduleUiState(isCheckingWeekStatus = true),
            ScheduleUiState(isInitializingWeek = true),
            ScheduleUiState(isWeekInitializedNoData = true)
        )
    }

    @Test
    fun `first checkWeekInitialized call for StudyGroup with error`() {
        // given
        val weekId = 0
        val throwableObject = NullPointerException()
        whenever(databaseRepository.isWeekInitialized(anyInt()))
            .thenReturn(
                Single.just(false)
                    .doOnSubscribe { disposable -> isWeekInitializedTest.onSubscribe(disposable) }
            )
        whenever(databaseRepository.insertWeekdays(anyInt()))
            .thenReturn(
                Completable.error(throwableObject)
                    .doOnSubscribe { disposable -> insertWeekdaysTest.onSubscribe(disposable) }
                    .doOnError { throwable -> insertWeekdaysTest.onError(throwable) }
            )
        val scheduleUiStateObserver = presenter.scheduleUiStateObservable.test()
        // when
        presenter.checkWeekInitialized(weekId)
        // then
        isWeekInitializedTest.assertSubscribed()
        insertWeekdaysTest.assertSubscribed()
        insertWeekdaysTest.assertError(throwableObject)
        scheduleUiStateObserver.assertSubscribed()
        scheduleUiStateObserver.assertNoErrors()
        scheduleUiStateObserver.assertValues(
            ScheduleUiState(),
            ScheduleUiState(isCheckingWeekStatus = true),
            ScheduleUiState(isInitializingWeek = true),
            ScheduleUiState(throwable = throwableObject)
        )
    }

    @Test
    fun `checkWeekInitialized call for StudyGroup with initialized week`() {
        // given
        val weekId = 0
        val scheduleUiStateObserver = presenter.scheduleUiStateObservable.test()
        // when
        presenter.checkWeekInitialized(weekId)
        // then
        isWeekInitializedTest.assertSubscribed()
        insertWeekdaysTest.assertNotSubscribed()
        scheduleUiStateObserver.assertSubscribed()
        scheduleUiStateObserver.assertNoErrors()
        scheduleUiStateObserver.assertValues(
            ScheduleUiState(),
            ScheduleUiState(isCheckingWeekStatus = true),
            ScheduleUiState(isWeekInitialized = true)
        )
    }

    @Test
    fun `getSchedule call for StudyGroup`() {
        // given
        val weekId = 0
        val scheduleUiStateObserver = presenter.scheduleUiStateObservable.test()
        // when
        presenter.getSchedule(ScheduleType.STUDYGROUP, weekId)
        // then
        getWeekdayWithStudyGroupLessonsForWeekTest.assertSubscribed()
        scheduleUiStateObserver.assertSubscribed()
        scheduleUiStateObserver.assertNoErrors()
        scheduleUiStateObserver.assertValues(
            ScheduleUiState(),
            ScheduleUiState(data = listOf(WeekdayWithStudyGroupLessons(weekday = String.DEFAULT)))
        )
    }

    @Test
    fun `getSchedule call for StudyGroup with error`() {
        // given
        val weekId = 0
        val throwableObject = NullPointerException()
        whenever(databaseRepository.getWeekdaysWithStudyGroupLessons(anyInt()))
            .thenReturn(
                Observable.error<WeekdayWithLessons<Lesson>>(throwableObject)
                    .doOnSubscribe { disposable -> getWeekdayWithStudyGroupLessonsForWeekTest.onSubscribe(disposable) }
                    .doOnError { throwable -> getWeekdayWithStudyGroupLessonsForWeekTest.onError(throwable) }
            )
        val scheduleUiStateObserver = presenter.scheduleUiStateObservable.test()
        // when
        presenter.getSchedule(ScheduleType.STUDYGROUP, weekId)
        // then
        getWeekdayWithStudyGroupLessonsForWeekTest.assertSubscribed()
        getWeekdayWithStudyGroupLessonsForWeekTest.assertError(throwableObject)
        scheduleUiStateObserver.assertSubscribed()
        scheduleUiStateObserver.assertNoErrors()
        scheduleUiStateObserver.assertValues(
            ScheduleUiState(),
            ScheduleUiState(throwable = throwableObject)
        )
    }

    @Test
    fun `first updateSchedule call for StudyGroup`() {
        // given
        val schedulerContainerId = 0
        val weekId = 0
        val scheduleUiStateObserver = presenter.scheduleUiStateObservable.test()
        val scheduleUpdateEventObserver = presenter.scheduleUpdateEventObservable.test()
        // when
        presenter.updateSchedule(schedulerContainerId, weekId, isFirstUpdate = true)
        // then
        getScheduleContainerTest.assertSubscribed()
        getWeekKeyTest.assertSubscribed()
        verify(databaseDataMapper).mapToStudyGroup(any())
        downloadStudyGroupScheduleTest.assertSubscribed()
        deleteLessonsForWeekTest.assertSubscribed()
        insertStudyGroupScheduleTest.assertSubscribed()
        scheduleUiStateObserver.assertSubscribed()
        scheduleUiStateObserver.assertNoErrors()
        scheduleUiStateObserver.assertValues(
            ScheduleUiState(),
            ScheduleUiState(isProcessingFirstUpdate = true),
            ScheduleUiState(data = listOf(WeekdayWithStudyGroupLessons(weekday = String.DEFAULT)))
        )
        scheduleUpdateEventObserver.assertSubscribed()
        scheduleUpdateEventObserver.assertNoErrors()
        scheduleUpdateEventObserver.assertValues(
            ScheduleUpdateEvent()
        )
    }

    @Test
    fun `first updateSchedule call for StudyGroup with network error`() {
        // given
        val schedulerContainerId = 0
        val weekId = 0
        val throwableObject = NullPointerException()
        whenever(networkRepository.getSchedule(any<StudyGroup>(), anyInt()))
            .thenReturn(
                Observable.error<WeekdayWithStudyGroupLessons>(throwableObject)
                    .doOnSubscribe { disposable -> downloadStudyGroupScheduleTest.onSubscribe(disposable) }
                    .doOnError { throwable -> downloadStudyGroupScheduleTest.onError(throwable) }
            )
        val scheduleUiStateObserver = presenter.scheduleUiStateObservable.test()
        val scheduleUpdateEventObserver = presenter.scheduleUpdateEventObservable.test()
        // when
        presenter.updateSchedule(schedulerContainerId, weekId, isFirstUpdate = true)
        // then
        getScheduleContainerTest.assertSubscribed()
        getWeekKeyTest.assertSubscribed()
        verify(databaseDataMapper).mapToStudyGroup(any())
        downloadStudyGroupScheduleTest.assertSubscribed()
        downloadStudyGroupScheduleTest.assertError(throwableObject)
        scheduleUiStateObserver.assertSubscribed()
        scheduleUiStateObserver.assertNoErrors()
        scheduleUiStateObserver.assertValues(
            ScheduleUiState(),
            ScheduleUiState(isProcessingFirstUpdate = true),
            ScheduleUiState(throwable = throwableObject, isFirstUpdateError = true)
        )
        scheduleUpdateEventObserver.assertSubscribed()
        scheduleUpdateEventObserver.assertNoErrors()
        scheduleUpdateEventObserver.assertValues(
            ScheduleUpdateEvent()
        )
    }

    @Test
    fun `updateSchedule call for StudyGroup`() {
        // given
        val schedulerContainerId = 0
        val weekId = 0
        val scheduleUiStateObserver = presenter.scheduleUiStateObservable.test()
        val scheduleUpdateEventObserver = presenter.scheduleUpdateEventObservable.test()
        // when
        presenter.updateSchedule(schedulerContainerId, weekId)
        // then
        getScheduleContainerTest.assertSubscribed()
        getWeekKeyTest.assertSubscribed()
        verify(databaseDataMapper).mapToStudyGroup(any())
        downloadStudyGroupScheduleTest.assertSubscribed()
        deleteLessonsForWeekTest.assertSubscribed()
        insertStudyGroupScheduleTest.assertSubscribed()
        scheduleUiStateObserver.assertSubscribed()
        scheduleUiStateObserver.assertNoErrors()
        scheduleUiStateObserver.assertValues(
            ScheduleUiState(),
            ScheduleUiState(data = listOf(WeekdayWithStudyGroupLessons(weekday = String.DEFAULT)))
        )
        scheduleUpdateEventObserver.assertSubscribed()
        scheduleUpdateEventObserver.assertNoErrors()
        scheduleUpdateEventObserver.assertValues(
            ScheduleUpdateEvent(),
            ScheduleUpdateEvent(isLoading = true),
            ScheduleUpdateEvent(isUpdateSuccessful = true)
        )
    }

    @Test
    fun `updateSchedule call for StudyGroup with network error`() {
        // given
        val schedulerContainerId = 0
        val weekId = 0
        val throwableObject = NullPointerException()
        whenever(networkRepository.getSchedule(any<StudyGroup>(), anyInt()))
            .thenReturn(
                Observable.error<WeekdayWithStudyGroupLessons>(throwableObject)
                    .doOnSubscribe { disposable -> downloadStudyGroupScheduleTest.onSubscribe(disposable) }
                    .doOnError { throwable -> downloadStudyGroupScheduleTest.onError(throwable) }
            )
        val scheduleUiStateObserver = presenter.scheduleUiStateObservable.test()
        val scheduleUpdateEventObserver = presenter.scheduleUpdateEventObservable.test()
        // when
        presenter.updateSchedule(schedulerContainerId, weekId)
        // then
        getScheduleContainerTest.assertSubscribed()
        getWeekKeyTest.assertSubscribed()
        verify(databaseDataMapper).mapToStudyGroup(any())
        downloadStudyGroupScheduleTest.assertSubscribed()
        downloadStudyGroupScheduleTest.assertError(throwableObject)
        scheduleUiStateObserver.assertSubscribed()
        scheduleUiStateObserver.assertNoErrors()
        scheduleUiStateObserver.assertValues(
            ScheduleUiState()
        )
        scheduleUpdateEventObserver.assertSubscribed()
        scheduleUpdateEventObserver.assertNoErrors()
        scheduleUpdateEventObserver.assertValues(
            ScheduleUpdateEvent(),
            ScheduleUpdateEvent(isLoading = true),
            ScheduleUpdateEvent(throwable = throwableObject)
        )
    }
}
