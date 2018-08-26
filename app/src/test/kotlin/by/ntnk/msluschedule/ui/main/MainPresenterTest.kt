package by.ntnk.msluschedule.ui.main

import by.ntnk.msluschedule.TestTree
import by.ntnk.msluschedule.any
import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
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

    @Mock
    private lateinit var view: MainView

    private lateinit var presenter: MainPresenter

    private val insertStudyGroupTest = TestObserver.create<Int>()
    private val insertTeacherTest = TestObserver.create<Int>()
    private val getWeeksTest = TestObserver.create<ScheduleFilter>()
    private val insertWeeksGetIdsTest = TestObserver.create<Int>()
    private val deleteScheduleContainerTest = TestObserver.create<Void>()

    @Before
    fun setUp() {
        Timber.plant(TestTree())
        MockitoAnnotations.initMocks(this)

        whenever(networkRepository.getWeeks())
                .thenReturn(Single.just(ScheduleFilter()).doOnSubscribe { getWeeksTest.onSubscribe(it) })
        whenever(databaseRepository.insertStudyGroup(any()))
                .thenReturn(Single.just(0).doOnSubscribe { insertStudyGroupTest.onSubscribe(it) })
        whenever(databaseRepository.insertTeacher(any()))
                .thenReturn(Single.just(0).doOnSubscribe { insertTeacherTest.onSubscribe(it) })
        whenever(databaseRepository.insertWeeksGetIds(any(), eq(0)))
                .thenReturn(Observable.just(0).doOnSubscribe { insertWeeksGetIdsTest.onSubscribe(it) })
        whenever(databaseRepository.deleteScheduleContainer(eq(0)))
                .thenReturn(Completable.complete().doOnSubscribe { deleteScheduleContainerTest.onSubscribe(it) })

        whenever(schedulerProvider.single())
                .thenReturn(Schedulers.trampoline())
        whenever(schedulerProvider.ui())
                .thenReturn(Schedulers.trampoline())
        whenever(schedulerProvider.cachedThreadPool())
                .thenReturn(Schedulers.trampoline())

        presenter = MainPresenter(
                currentDate,
                databaseRepository,
                networkRepository,
                sharedPreferencesRepository,
                schedulerProvider
        )
        presenter.bindView(view)
    }


    @Test
    fun addGroup() {
        // given
        val studyGroup = StudyGroup(0, "", 0, 0, 0)
        // when
        presenter.addGroup(studyGroup)
        // then
        insertStudyGroupTest.assertSubscribed()
        getWeeksTest.assertSubscribed()
        insertWeeksGetIdsTest.assertSubscribed()
        deleteScheduleContainerTest.assertNotSubscribed()

        verify(networkRepository).getWeeks()
        verify(databaseRepository).insertStudyGroup(any())
        verify(databaseRepository).insertWeeksGetIds(any(), eq(0))
        verify(sharedPreferencesRepository).putSelectedScheduleContainer(any())
        verify(view).initMainContent()
        verify(view).showNewScheduleContainerLoading(any())
    }

    @Test
    fun addTeacher() {
        // given
        val teacher = Teacher(0, "", 0)
        // when
        presenter.addTeacher(teacher)
        // then
        insertTeacherTest.assertSubscribed()
        getWeeksTest.assertSubscribed()
        insertWeeksGetIdsTest.assertSubscribed()
        deleteScheduleContainerTest.assertNotSubscribed()

        verify(networkRepository).getWeeks()
        verify(databaseRepository).insertTeacher(any())
        verify(databaseRepository).insertWeeksGetIds(any(), eq(0))
        verify(sharedPreferencesRepository).putSelectedScheduleContainer(any())
        verify(view).initMainContent()
        verify(view).showNewScheduleContainerLoading(any())
    }

    @Test
    fun `Delete the inserted group if we aren't able to get weeks`() {
        // given
        val studyGroup = StudyGroup(0, "", 0, 0, 0)
        whenever(networkRepository.getWeeks()).thenReturn(Single.error(NullPointerException()))
        // when
        presenter.addGroup(studyGroup)
        // then
        insertStudyGroupTest.assertSubscribed()
        getWeeksTest.assertNotSubscribed()
        insertWeeksGetIdsTest.assertNotSubscribed()
        deleteScheduleContainerTest.assertSubscribed()

        verify(networkRepository).getWeeks()
        verify(databaseRepository).deleteScheduleContainer(eq(0))
        verify(sharedPreferencesRepository, times(0)).putSelectedScheduleContainer(any())
        verify(view).showNewScheduleContainerLoading(any())
        verify(view).showError()
    }

    @Test
    fun `Delete the inserted teacher if we aren't able to get weeks`() {
        // given
        val teacher = Teacher(0, "", 0)
        whenever(networkRepository.getWeeks()).thenReturn(Single.error(NullPointerException()))
        // when
        presenter.addTeacher(teacher)
        // then
        insertTeacherTest.assertSubscribed()
        getWeeksTest.assertNotSubscribed()
        insertWeeksGetIdsTest.assertNotSubscribed()
        deleteScheduleContainerTest.assertSubscribed()

        verify(networkRepository).getWeeks()
        verify(databaseRepository).deleteScheduleContainer(eq(0))
        verify(sharedPreferencesRepository, times(0)).putSelectedScheduleContainer(any())
        verify(view).showNewScheduleContainerLoading(any())
        verify(view).showError()
    }
}
