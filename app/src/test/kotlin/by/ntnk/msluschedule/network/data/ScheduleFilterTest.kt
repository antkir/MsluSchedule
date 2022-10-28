package by.ntnk.msluschedule.network.data

import by.ntnk.msluschedule.TestTree
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import timber.log.Timber

class ScheduleFilterTest {

    @Before
    fun setUp() {
        Timber.plant(TestTree())
    }

    @Test
    fun `Two empty ScheduleFilter objects must be equal`() {
        // given
        // when
        val scheduleFilter1 = ScheduleFilter()
        val scheduleFilter2 = ScheduleFilter()
        // then
        assertEquals(scheduleFilter1, scheduleFilter2)
    }
}
