package by.ntnk.msluschedule.ui.main

import by.ntnk.msluschedule.TestTree
import by.ntnk.msluschedule.any
import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.Test

import org.junit.Before
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.mockito.MockitoAnnotations
import timber.log.Timber
import org.mockito.Mockito.`when` as whenever

class MainPresenterTest {
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

    @Before
    fun setUp() {
        Timber.plant(TestTree())
        MockitoAnnotations.initMocks(this)
        whenever(networkRepository.getWeeks())
                .thenReturn(Single.just(ScheduleFilter()))
        whenever(databaseRepository.insertStudyGroup(any()))
                .thenReturn(Single.just(0))
        whenever(databaseRepository.insertWeeksGetIds(any(), eq(0)))
                .thenReturn(Observable.just(0))
        whenever(databaseRepository.insertWeekdays(eq(0)))
                .thenReturn(Completable.complete())
        whenever(databaseRepository.deleteScheduleContainer(eq(0)))
                .thenReturn(Completable.complete())
        whenever(schedulerProvider.single())
                .thenReturn(Schedulers.trampoline())
        whenever(schedulerProvider.ui())
                .thenReturn(Schedulers.trampoline())
        presenter = MainPresenter(
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
        val studyGroup = StudyGroup(0, "", 0, 0 , 0)
        // when
        presenter.addGroup(studyGroup)
        // then
        verify(networkRepository).getWeeks()
        verify(databaseRepository).insertStudyGroup(any())
        verify(databaseRepository).insertWeeksGetIds(any(), eq(0))
        verify(view).initMainContent()
    }

    @Test
    fun `Delete the inserted group if we aren't able to get weeks`() {
        // given
        val studyGroup = StudyGroup(0, "", 0, 0, 0)
        whenever(networkRepository.getWeeks()).thenReturn(Single.error(NullPointerException()))
        // when
        presenter.addGroup(studyGroup)
        // then
        verify(networkRepository).getWeeks()
        verify(databaseRepository).deleteScheduleContainer(eq(0))
        verifyZeroInteractions(view)
    }
}
