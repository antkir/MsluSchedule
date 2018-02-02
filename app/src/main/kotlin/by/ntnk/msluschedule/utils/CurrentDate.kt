package by.ntnk.msluschedule.utils

import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import org.threeten.bp.ZoneId

open class CurrentDate {
    open val date: LocalDate
        get() = LocalDate.now(localTimeZone)

    private val year: Int
        get() = date.year

    private val month: Int
        get() = date.month.value

    // Schedule for the next academic year usually becomes available in late weeks of August
    val academicYear: Int
        get() = if (month >= Month.AUGUST.value) year else year - 1

    companion object {
        val localTimeZone: ZoneId = ZoneId.of("GMT+3")
    }
}
