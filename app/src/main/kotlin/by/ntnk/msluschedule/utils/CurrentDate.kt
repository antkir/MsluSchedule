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

    /**
     * Get academic year for the current date.
     * Schedule for the next academic year usually becomes available in late weeks of August.
     * Returns current year from August through December.
     * Returns previous year from January through July.
     */
    val academicYear: Int
        get() = if (month >= Month.AUGUST.value) year else year - 1

    private val week: Int
        get() = date.get(weekField)

    /**
     * Get academic week for the current date.
     * Zero-based numbering is used, i.e. first week of September returns 0.
     * If the first week of September consists of Sunday only, it does not count as the first week.
     * Returns negative numbers in August.
     */
    val academicWeek: Int
        get() {
            var septemberDate = LocalDate.of(academicYear, Month.SEPTEMBER, 1)
            if (septemberDate.dayOfWeek == DayOfWeek.SUNDAY) {
                septemberDate = septemberDate.plusDays(1)
            }
            val septemberWeek = septemberDate.get(weekField)

            return when (month >= Month.AUGUST.value) {
                true -> week - septemberWeek
                false -> {
                    val yearEndDate = LocalDate.of(academicYear, Month.DECEMBER, 31)
                    val yearEndWeek = yearEndDate.get(weekField)
                    yearEndWeek - septemberWeek + week
                }
            }
        }

    companion object {
        private val localTimeZone: ZoneId = ZoneId.of("GMT+3")
        private val weekField: TemporalField = WeekFields.of(DayOfWeek.MONDAY, 7).weekOfYear()
    }
}
