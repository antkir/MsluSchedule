package by.ntnk.msluschedule.utils

import by.ntnk.msluschedule.TestTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import timber.log.Timber

class CurrentDateTest {
    private val currentDate = object : CurrentDate() {
        override var date = LocalDate.of(1970, 9, 1)
    }

    @Before
    fun setUp() {
        Timber.plant(TestTree())
    }

    @Test
    fun `Academic year equals current year when month value is equal to or greater than august`() {
        // given
        val year = 2000
        val month = 9
        val day = 1
        currentDate.date = LocalDate.of(year, month, day)
        // when
        val academicYear = currentDate.academicYear
        // then
        assertEquals(year, academicYear)
    }

    @Test
    fun `Academic year equals current year - 1 when month value is less than august`() {
        // given
        val year = 2000
        val month = 7
        val day = 1
        currentDate.date = LocalDate.of(year, month, day)
        // when
        val academicYear = currentDate.academicYear
        // then
        assertEquals(year - 1, academicYear)
    }

    @Test
    fun `"academicWeek" for September the 1st must return 0 or -1 for Sundays`() {
        // given
        val year = 1970
        val month = 9
        val day = 1
        for (i in 0..100) {
            val currentYear = year + i
            currentDate.date = LocalDate.of(currentYear, month, day)
            // when
            val academicWeek = currentDate.academicWeek
            // then
            if (currentDate.date.dayOfWeek == DayOfWeek.SUNDAY) {
                assertEquals(-1, academicWeek)
            } else {
                assertEquals(0, academicWeek)
            }
        }
    }

    @Test
    fun `When September the 1st is Sunday, "academicWeek" for September the 2nd must return 0`() {
        // given
        val year = 2019
        val month = 9
        val day = 2
        currentDate.date = LocalDate.of(year, month, day)
        // when
        val academicWeek = currentDate.academicWeek
        // then
        assertEquals(0, academicWeek)
    }

    @Test
    fun `If the current month is August, "academicWeek" must return a negative number`() {
        // given
        val year = 1970
        val month = 8
        val day = 15
        currentDate.date = LocalDate.of(year, month, day)
        // when
        val academicWeek = currentDate.academicWeek
        // then
        assertTrue(academicWeek < 0)
    }
}
