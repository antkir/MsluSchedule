package by.ntnk.msluschedule.ui.week

import by.ntnk.msluschedule.TestTree
import by.ntnk.msluschedule.any
import by.ntnk.msluschedule.data.*
import by.ntnk.msluschedule.db.DatabaseDataMapper
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.db.data.ScheduleContainer
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.utils.CurrentDate
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
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import timber.log.Timber
import java.util.Collections
import org.mockito.Mockito.`when` as whenever

class WeekPresenterTest {
    @Mock
    private lateinit var currentDate: CurrentDate

    @Mock
    private lateinit var databaseRepository: DatabaseRepository

    @Mock
    private lateinit var databaseDataMapper: DatabaseDataMapper

    @Mock
    private lateinit var networkRepository: NetworkRepository

    @Mock
    private lateinit var sharedPreferencesRepository: SharedPreferencesRepository

    @Mock
    private lateinit var schedulerProvider: SchedulerProvider

    @Mock
    private lateinit var view: WeekView

    private lateinit var presenter: WeekPresenter

    private val getScheduleContainerTest = TestObserver.create<ScheduleContainer>()
    private val getWeekKeyTest = TestObserver.create<Int>()
    private val deleteLessonsForWeekTest = TestObserver.create<Void>()
    private val getWeekdayWithStudyGroupLessonsForWeekTest = TestObserver.create<WeekdayWithLessons<Lesson>>()
    private val getWeekdayWithTeacherLessonsForWeekTest = TestObserver.create<WeekdayWithLessons<Lesson>>()
    private val insertStudyGroupScheduleTest = TestObserver.create<List<WeekdayWithStudyGroupLessons>>()
    private val insertTeacherScheduleTest = TestObserver.create<List<WeekdayWithTeacherLessons>>()
    private val getStudyGroupScheduleTest = TestObserver.create<List<WeekdayWithStudyGroupLessons>>()
    private val getTeacherScheduleTest = TestObserver.create<List<WeekdayWithTeacherLessons>>()
    private val insertWeekdaysTest = TestObserver.create<Int>()
    private val getNotesForWeekdayObservableTest = TestObserver.create<Int>()

    @Before
    fun setUp() {
        Timber.plant(TestTree())
        MockitoAnnotations.initMocks(this)

        whenever(sharedPreferencesRepository.getSelectedScheduleContainerInfo())
                .thenReturn(ScheduleContainerInfo(0, "", ScheduleType.STUDYGROUP))
        whenever(databaseRepository.getScheduleContainer(anyInt()))
                .thenReturn(Single.just(ScheduleContainer(0, "", ScheduleType.STUDYGROUP, 0))
                                    .doOnSubscribe { getScheduleContainerTest.onSubscribe(it) })
        whenever(databaseRepository.getWeekKey(anyInt()))
                .thenReturn(Single.just(0)
                                    .doOnSubscribe { getWeekKeyTest.onSubscribe(it) })
        whenever(databaseRepository.deleteLessonsForWeek(anyInt(), any()))
                .thenReturn(Completable.complete()
                                    .doOnSubscribe { deleteLessonsForWeekTest.onSubscribe(it) })
        whenever(databaseRepository.getWeekdayWithStudyGroupLessonsForWeek(anyInt()))
                .thenReturn(Observable.just(WeekdayWithStudyGroupLessons("") as WeekdayWithLessons<Lesson>)
                                    .doOnSubscribe { getWeekdayWithStudyGroupLessonsForWeekTest.onSubscribe(it) })
        whenever(databaseRepository.getWeekdayWithTeacherLessonsForWeek(anyInt()))
                .thenReturn(Observable.just(WeekdayWithTeacherLessons("") as WeekdayWithLessons<Lesson>)
                                    .doOnSubscribe { getWeekdayWithTeacherLessonsForWeekTest.onSubscribe(it) })
        whenever(databaseRepository.insertStudyGroupSchedule(any(), anyInt()))
                .thenReturn(Single.just(Collections.singletonList(WeekdayWithStudyGroupLessons("")))
                                    .doOnSubscribe { insertStudyGroupScheduleTest.onSubscribe(it) })
        whenever(databaseRepository.insertTeacherSchedule(any(), anyInt()))
                .thenReturn(Single.just(Collections.singletonList(WeekdayWithTeacherLessons("")))
                                    .doOnSubscribe { insertTeacherScheduleTest.onSubscribe(it) })
        whenever(databaseRepository.insertWeekdays(anyInt()))
                .thenReturn(Completable.complete()
                                    .doOnSubscribe { insertWeekdaysTest.onSubscribe(it) })
        whenever(databaseRepository.getNotesForWeekdayObservable(anyInt()))
                .thenReturn(Observable.just(List(1) { return@List Note(0, "") } )
                                    .doOnSubscribe { getNotesForWeekdayObservableTest.onSubscribe(it) })
        whenever(networkRepository.getSchedule(any<StudyGroup>(), anyInt()))
                .thenReturn(Observable.just(WeekdayWithStudyGroupLessons(""))
                                    .doOnSubscribe { getStudyGroupScheduleTest.onSubscribe(it) })
        whenever(networkRepository.getSchedule(any<Teacher>(), anyInt()))
                .thenReturn(Observable.just(WeekdayWithTeacherLessons(""))
                                    .doOnSubscribe { getTeacherScheduleTest.onSubscribe(it) })
        whenever(databaseDataMapper.mapToStudyGroup(any()))
                .thenReturn(StudyGroup(0, "", 0, 0, 0))
        whenever(databaseDataMapper.mapToTeacher(any()))
                .thenReturn(Teacher(0, "", 0))

        whenever(schedulerProvider.single())
                .thenReturn(Schedulers.trampoline())
        whenever(schedulerProvider.ui())
                .thenReturn(Schedulers.trampoline())
        whenever(schedulerProvider.cachedThreadPool())
                .thenReturn(Schedulers.trampoline())

        presenter = WeekPresenter(
                currentDate,
                databaseRepository,
                databaseDataMapper,
                sharedPreferencesRepository,
                schedulerProvider,
                networkRepository
        )
        presenter.bindView(view)
    }

    @Test
    fun `initial getSchedule for StudyGroup`() {
        // given
        val weekId = 0
        val isWeekInitializedTest = TestObserver.create<Boolean>()
        whenever(databaseRepository.isWeekInitialized(anyInt()))
                .thenReturn(Single.just(false)
                                    .doOnSubscribe { isWeekInitializedTest.onSubscribe(it) })
        // when
        presenter.getSchedule(weekId)
        // then
        getScheduleContainerTest.assertSubscribed()
        isWeekInitializedTest.assertSubscribed()
        insertWeekdaysTest.assertSubscribed()
        getWeekKeyTest.assertSubscribed()
        getStudyGroupScheduleTest.assertSubscribed()
        insertStudyGroupScheduleTest.assertSubscribed()
        getNotesForWeekdayObservableTest.assertSubscribed()

        verify(sharedPreferencesRepository).getSelectedScheduleContainerInfo()
        verify(databaseDataMapper).mapToStudyGroup(any())
        verify(view).showInitProgressBar()
        verify(view).hideInitProgressBar()
        verify(view).showSchedule(any())
    }

    @Test
    fun `initial getSchedule for StudyGroup with error`() {
        // given
        val weekId = 0
        val isWeekInitializedTest = TestObserver.create<Boolean>()
        whenever(databaseRepository.isWeekInitialized(anyInt()))
                .thenReturn(Single.just(false)
                                    .doOnSubscribe { isWeekInitializedTest.onSubscribe(it) })
        whenever(networkRepository.getSchedule(any<StudyGroup>(), anyInt()))
                .thenReturn(Observable.error(NullPointerException()))
        // when
        presenter.getSchedule(weekId)
        // then
        getScheduleContainerTest.assertSubscribed()
        isWeekInitializedTest.assertSubscribed()
        insertWeekdaysTest.assertSubscribed()
        getWeekKeyTest.assertSubscribed()
        getStudyGroupScheduleTest.assertNotSubscribed()
        insertStudyGroupScheduleTest.assertNotSubscribed()
        getNotesForWeekdayObservableTest.assertNotSubscribed()

        verify(sharedPreferencesRepository).getSelectedScheduleContainerInfo()
        verify(databaseDataMapper).mapToStudyGroup(any())
        verify(view).showInitProgressBar()
        verify(view).hideInitProgressBar()
        verify(view).showError(any(), eq(true))
    }

    @Test
    fun `updateSchedule for StudyGroup`() {
        // given
        val weekId = 0
        // when
        presenter.updateSchedule(weekId)
        // then
        getScheduleContainerTest.assertSubscribed()
        deleteLessonsForWeekTest.assertSubscribed()
        getWeekKeyTest.assertSubscribed()
        getStudyGroupScheduleTest.assertSubscribed()
        insertStudyGroupScheduleTest.assertSubscribed()
        getWeekdayWithStudyGroupLessonsForWeekTest.assertSubscribed()

        verify(sharedPreferencesRepository).getSelectedScheduleContainerInfo()
        verify(view).showUpdateProgressBar()
        verify(view).hideUpdateProgressBar()
        verify(view).showUpdateSuccessMessage()
        verify(view).showSchedule(any())
    }

    @Test
    fun `updateSchedule for StudyGroup with error`() {
        // given
        val weekId = 0
        whenever(networkRepository.getSchedule(any<StudyGroup>(), anyInt()))
                .thenReturn(Observable.error(NullPointerException()))
        // when
        presenter.updateSchedule(weekId)
        // then
        getScheduleContainerTest.assertSubscribed()
        deleteLessonsForWeekTest.assertSubscribed()
        getWeekKeyTest.assertSubscribed()
        getStudyGroupScheduleTest.assertNotSubscribed()
        insertStudyGroupScheduleTest.assertNotSubscribed()
        getWeekdayWithStudyGroupLessonsForWeekTest.assertNotSubscribed()

        verify(sharedPreferencesRepository).getSelectedScheduleContainerInfo()
        verify(view).showUpdateProgressBar()
        verify(view).hideUpdateProgressBar()
        verify(view).showError(any(), eq(false))
    }
}
