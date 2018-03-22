package by.ntnk.msluschedule.utils

import by.ntnk.msluschedule.di.PerApp
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import org.threeten.bp.ZoneId
import org.threeten.bp.temporal.TemporalField
import org.threeten.bp.temporal.WeekFields
import javax.inject.Inject

@PerApp
open class CurrentDate @Inject constructor() {
    open val date: LocalDate
        get() = LocalDate.now(localTimeZone)

    private val year: Int
        get() = date.year

    private val month: Int
        get() = date.month.value

    // Schedule for the next academic year usually becomes available in late weeks of August
    val academicYear: Int
        get() = if (month >= Month.AUGUST.value) year else year - 1

    private val week: Int
        get() = date.get(weekField)

    val academicWeek: Int
        get() {
            var septemberDate = LocalDate.of(academicYear, Month.SEPTEMBER, 1)
            if (septemberDate.dayOfWeek == DayOfWeek.SUNDAY) {
                septemberDate = septemberDate.plusDays(1)
            }
            val septemberWeek = septemberDate.get(weekField)

            val yearEndDate = LocalDate.of(academicYear, Month.DECEMBER, 31)
            val yearEndWeek = yearEndDate.get(weekField)

            val academicWeek = when (month >= Month.AUGUST.value) {
                true -> week - septemberWeek
                false -> yearEndWeek - septemberWeek + week
            }

            return if (academicWeek > 0) academicWeek else 0
        }

    companion object {
        private val localTimeZone: ZoneId = ZoneId.of("GMT+3")
        private val weekField: TemporalField =
                WeekFields.of(DayOfWeek.MONDAY, 7).weekOfYear()
    }
}
