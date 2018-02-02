package by.ntnk.msluschedule.utils

import org.junit.Test

import org.junit.Assert.*
import org.threeten.bp.LocalDate

class CurrentDateTest {
    private val currentDate = object : CurrentDate() {
        override var date = LocalDate.now(CurrentDate.localTimeZone)
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
        assertEquals(academicYear, year)
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
        assertEquals(academicYear, year - 1)
    }

}
