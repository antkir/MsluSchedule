package by.ntnk.msluschedule.network.api.myuniversity

import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.data.StudyGroupLesson
import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.data.TeacherLesson
import by.ntnk.msluschedule.data.WeekdayWithStudyGroupLessons
import by.ntnk.msluschedule.data.WeekdayWithTeacherLessons
import by.ntnk.msluschedule.di.PerApp
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.Days
import by.ntnk.msluschedule.utils.EMPTY_STRING
import by.ntnk.msluschedule.utils.HttpStatusException
import by.ntnk.msluschedule.utils.NetworkApiVersion
import by.ntnk.msluschedule.utils.NetworkApiVersionException
import by.ntnk.msluschedule.utils.NoDataOnServerException
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import by.ntnk.msluschedule.utils.firstOrDefault
import by.ntnk.msluschedule.utils.getNetworkApiVersionFromWeekKey
import by.ntnk.msluschedule.utils.takeIfOrDefault
import by.ntnk.msluschedule.utils.takeUnlessOrDefault
import com.squareup.moshi.JsonDataException
import io.reactivex.Observable
import io.reactivex.Single
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.Month
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeFormatterBuilder
import org.threeten.bp.format.TextStyle
import org.threeten.bp.temporal.ChronoField
import org.threeten.bp.temporal.ChronoUnit
import org.threeten.bp.temporal.TemporalAdjusters
import retrofit2.Response
import java.util.Locale
import javax.inject.Inject

@PerApp
class NetworkRepository @Inject constructor(
    private val currentDate: CurrentDate,
    private val sharedPreferencesRepository: SharedPreferencesRepository,
    private val scheduleService: ScheduleService
) : NetworkRepository {

    override fun getFaculties(): Single<ScheduleFilter> {
        return scheduleService.getStudyGroups()
            .map { response ->
                checkResponse(response)

                val scheduleFilter = ScheduleFilter()
                val body = response.body() ?: throw JsonDataException()
                val faculties = body.result ?: throw NoDataOnServerException()
                for (faculty in faculties) {
                    val facultyCode = faculty.facultyCode ?: throw JsonDataException()
                    val facultyName = faculty.faculty ?: throw JsonDataException()
                    scheduleFilter.put(facultyCode, facultyName)
                }
                return@map scheduleFilter
            }
    }

    override fun getCourses(facultyKey: Int): Single<ScheduleFilter> {
        return scheduleService.getStudyGroups()
            .map { response ->
                checkResponse(response)

                val scheduleFilter = ScheduleFilter()
                val body = response.body() ?: throw JsonDataException()
                val faculties = body.result ?: throw NoDataOnServerException()
                for (faculty in faculties) {
                    val facultyCode = faculty.facultyCode ?: throw JsonDataException()
                    if (facultyCode == facultyKey) {
                        val courses = faculty.courses ?: throw JsonDataException()
                        for (course in courses) {
                            val courseCode = course.courseCode ?: throw JsonDataException()
                            val courseName = course.course ?: throw JsonDataException()
                            scheduleFilter.put(courseCode, courseName)
                        }
                    }
                }
                return@map scheduleFilter
            }
    }

    override fun getGroups(facultyKey: Int, courseKey: Int, yearKey: Int): Single<ScheduleFilter> {
        return scheduleService.getStudyGroups()
            .map { response ->
                checkResponse(response)

                val scheduleFilter = ScheduleFilter()
                val body = response.body() ?: throw JsonDataException()
                val faculties = body.result ?: throw NoDataOnServerException()
                for (faculty in faculties) {
                    val facultyCode = faculty.facultyCode ?: throw JsonDataException()
                    if (facultyCode == facultyKey) {
                        val courses = faculty.courses ?: throw JsonDataException()
                        for (course in courses) {
                            val courseCode = course.courseCode ?: throw JsonDataException()
                            if (courseCode == courseKey) {
                                val groups = course.groups ?: throw JsonDataException()
                                for (group in groups) {
                                    val groupCode = group.groupCode ?: throw JsonDataException()
                                    val groupName = group.group ?: throw JsonDataException()
                                    val groupKey = groupKeyFromString(groupCode)
                                    scheduleFilter.put(groupKey, groupName)
                                }
                            }
                        }
                    }
                }
                return@map scheduleFilter
            }
    }

    override fun getTeachers(): Single<ScheduleFilter> {
        return scheduleService.getTeachers()
            .map { response ->
                checkResponse(response)

                val scheduleFilter = ScheduleFilter()
                val body = response.body() ?: throw JsonDataException()
                val teachers = body.result ?: throw NoDataOnServerException()
                for ((i, teacher) in teachers.withIndex()) {
                    scheduleFilter.put(i, teacher)
                }
                return@map scheduleFilter
            }
    }

    override fun getWeeks(): Single<ScheduleFilter> {
        return Single.just(getAcademicYearWeeks())
            .map { weeks ->
                val scheduleFilter = ScheduleFilter()
                for ((i, week) in weeks.withIndex()) {
                    scheduleFilter.put(i, week)
                }
                return@map scheduleFilter
            }
    }

    override fun getSchedule(studyGroup: StudyGroup, weekKey: Int): Observable<WeekdayWithStudyGroupLessons> {
        if (getNetworkApiVersionFromWeekKey(weekKey) != NetworkApiVersion.MYUNIVERSITY) {
            throw NetworkApiVersionException()
        }

        val academicWeek = weekKey.toLong()
        // Schedule data is served only for the current week (for now), so
        // we need to preserve the local data if the schedule for
        // the selected academic week is already gone from the server.
        if (getLastDayOfAcademicWeek(academicWeek) < currentDate.date) {
            throw NoDataOnServerException()
        }

        val groupKey = groupKeyToString(studyGroup.key)
        return scheduleService.getGroupClasses(groupKey)
            .flatMapObservable { response ->
                checkResponse(response)

                val weekdays = listOf(
                    WeekdayWithStudyGroupLessons(Days.MONDAY),
                    WeekdayWithStudyGroupLessons(Days.TUESDAY),
                    WeekdayWithStudyGroupLessons(Days.WEDNESDAY),
                    WeekdayWithStudyGroupLessons(Days.THURSDAY),
                    WeekdayWithStudyGroupLessons(Days.FRIDAY),
                    WeekdayWithStudyGroupLessons(Days.SATURDAY),
                    WeekdayWithStudyGroupLessons(Days.SUNDAY)
                )

                val body = response.body() ?: throw JsonDataException()
                val result = body.result ?: throw NoDataOnServerException()
                val classes = result.schedule ?: throw JsonDataException()

                val filteredClasses = mutableListOf<Pair<LocalDateTime, StudyGroupLesson>>()

                for (jsonGroupClass in classes) {
                    jsonGroupClass.title ?: throw JsonDataException()
                    jsonGroupClass.summary ?: throw JsonDataException()
                    jsonGroupClass.room ?: throw JsonDataException()
                    jsonGroupClass.start ?: throw JsonDataException()
                    jsonGroupClass.end ?: throw JsonDataException()
                    jsonGroupClass.day ?: throw JsonDataException()

                    val classDate = LocalDate.parse(jsonGroupClass.day)
                    if (isClassDateInWeek(classDate, academicWeek)) {
                        val subjectTypePair = jsonGroupClass.title.trim()
                        val subjectTypeSplitIdx = findOpeningParenthesisIndex(subjectTypePair)
                        val subject = subjectTypePair
                            .substring(0, subjectTypeSplitIdx)
                            .trim()
                        val type = subjectTypePair
                            .substring(subjectTypeSplitIdx, subjectTypePair.length)
                            .takeUnlessOrDefault({ s -> s.length == subjectTypePair.length }, EMPTY_STRING)
                            .drop(1)
                            .dropLast(1)
                            .trim()
                        val teacher = jsonGroupClass.summary
                            .split('/')
                            .getOrElse(0) { EMPTY_STRING }
                            .trim()
                            .takeIfOrDefault({ s -> s.count { c -> c.isDigit() } < 3 }, EMPTY_STRING)
                        val classroom = jsonGroupClass.room
                            .replace(" ", EMPTY_STRING)
                            .trim()
                            .ifEmpty {
                                jsonGroupClass.summary
                                    .split('/')
                                    .getOrElse(1) { EMPTY_STRING }
                                    .trim()
                                    .ifEmpty {
                                        jsonGroupClass.summary
                                            .replace(" ", EMPTY_STRING)
                                            .trim()
                                            .takeIfOrDefault({ s -> s.count { c -> c.isDigit() } >= 3 }, EMPTY_STRING)
                                    }
                            }
                        val startDateTime = parseDateTime(jsonGroupClass.start)
                        val start = formatTime(startDateTime)
                        val endDateTime = parseDateTime(jsonGroupClass.end)
                        val end = formatTime(endDateTime)
                        val groupClass = StudyGroupLesson(subject, type, teacher, classroom, start, end)
                        filteredClasses.add(Pair(startDateTime, groupClass))
                    }
                }

                val isPhysEdClassHidden = sharedPreferencesRepository.isPhysEdClassHidden()
                val isSelfStudyClassHidden = sharedPreferencesRepository.isSelfStudyClassHidden()
                filteredClasses
                    .filter { entry ->
                        val studyGroupClass = entry.second
                        val isPhysEdClass =
                            studyGroupClass.subject.startsWith("физ", ignoreCase = true) &&
                                studyGroupClass.subject.endsWith("ра", ignoreCase = true)
                        val isSelfStudyClass =
                            studyGroupClass.type.contains("ср", ignoreCase = true) &&
                                studyGroupClass.subject.contains("сам", ignoreCase = true) &&
                                studyGroupClass.subject.contains("раб", ignoreCase = true)
                        return@filter (!isPhysEdClassHidden || !isPhysEdClass) &&
                            (!isSelfStudyClassHidden || !isSelfStudyClass)
                    }
                    .sortedBy { entry -> entry.first }
                    .forEach { entry ->
                        val dayOfWeekIndex = entry.first.dayOfWeek.ordinal
                        weekdays[dayOfWeekIndex].lessons.add(entry.second)
                    }

                return@flatMapObservable Observable.fromIterable(weekdays)
            }
    }

    override fun getSchedule(teacher: Teacher, weekKey: Int): Observable<WeekdayWithTeacherLessons> {
        if (getNetworkApiVersionFromWeekKey(weekKey) != NetworkApiVersion.MYUNIVERSITY) {
            throw NetworkApiVersionException()
        }

        val academicWeek = weekKey.toLong()
        // Schedule data is served only for the current week (for now), so
        // we need to preserve the local data if the schedule for
        // the selected academic week is already gone from the server.
        if (getLastDayOfAcademicWeek(academicWeek) < currentDate.date) {
            throw NoDataOnServerException()
        }

        return scheduleService.getTeacherClasses(teacher.name)
            .flatMapObservable { response ->
                checkResponse(response)

                val weekdays = listOf(
                    WeekdayWithTeacherLessons(Days.MONDAY),
                    WeekdayWithTeacherLessons(Days.TUESDAY),
                    WeekdayWithTeacherLessons(Days.WEDNESDAY),
                    WeekdayWithTeacherLessons(Days.THURSDAY),
                    WeekdayWithTeacherLessons(Days.FRIDAY),
                    WeekdayWithTeacherLessons(Days.SATURDAY),
                    WeekdayWithTeacherLessons(Days.SUNDAY)
                )

                val body = response.body() ?: throw JsonDataException()
                val classes = body.result ?: throw NoDataOnServerException()

                val filteredClasses = mutableMapOf<LocalDateTime, MutableList<TeacherLesson>>()

                for (jsonTeacherClass in classes) {
                    jsonTeacherClass.title ?: throw JsonDataException()
                    jsonTeacherClass.summary ?: throw JsonDataException()
                    jsonTeacherClass.room ?: throw JsonDataException()
                    jsonTeacherClass.start ?: throw JsonDataException()
                    jsonTeacherClass.end ?: throw JsonDataException()
                    jsonTeacherClass.day ?: throw JsonDataException()
                    jsonTeacherClass.groupLabel ?: throw JsonDataException()

                    val classDate = LocalDate.parse(jsonTeacherClass.day)
                    if (isClassDateInWeek(classDate, academicWeek)) {
                        val subjectType = jsonTeacherClass.title.trim()
                        val subjectTypeSplitIdx = findOpeningParenthesisIndex(subjectType)
                        val subject = subjectType
                            .substring(0, subjectTypeSplitIdx)
                            .trim()
                        val type = subjectType
                            .substring(subjectTypeSplitIdx, subjectType.length)
                            .takeUnlessOrDefault({ s -> s.length == subjectType.length }, EMPTY_STRING)
                            .drop(1)
                            .dropLast(1)
                            .trim()
                        val faculty = EMPTY_STRING
                        val groups = jsonTeacherClass.groupLabel.trim()
                        val classroom = jsonTeacherClass.room
                            .replace(" ", EMPTY_STRING)
                            .trim()
                        val startDateTime = parseDateTime(jsonTeacherClass.start)
                        val start = formatTime(startDateTime)
                        val endDateTime = parseDateTime(jsonTeacherClass.end)
                        val end = formatTime(endDateTime)

                        if (!filteredClasses.contains(startDateTime)) {
                            filteredClasses[startDateTime] = mutableListOf()

                            val teacherClass = TeacherLesson(subject, faculty, groups, type, classroom, start, end)
                            filteredClasses[startDateTime]!!.add(teacherClass)
                        } else {
                            // Use a list of teacher classes instead of a single element,
                            // so conflicting classes don't go unnoticed.
                            val updatedTeacherClasses = filteredClasses[startDateTime]!!
                                .filter {
                                    it.subject != subject ||
                                        it.type != type ||
                                        (it.classroom != classroom &&
                                            it.classroom != EMPTY_STRING &&
                                            classroom != EMPTY_STRING)
                                }
                                .toMutableList()

                            updatedTeacherClasses.add(
                                filteredClasses[startDateTime]!!
                                    .firstOrDefault(
                                        {
                                            it.subject == subject &&
                                                it.type == type &&
                                                (it.classroom == classroom ||
                                                    it.classroom == EMPTY_STRING ||
                                                    classroom == EMPTY_STRING)
                                        },
                                        TeacherLesson(subject, faculty, EMPTY_STRING, type, classroom, start, end)
                                    )
                                    .let {
                                        it.copy(
                                            groups = concatListStrings(it.groups, groups),
                                            classroom = if (it.classroom != EMPTY_STRING) it.classroom else classroom
                                        )
                                    }
                            )

                            filteredClasses[startDateTime] = updatedTeacherClasses
                        }
                    }
                }

                filteredClasses.entries
                    .sortedBy { entry -> entry.key }
                    .forEach { entry ->
                        val dayOfWeekIndex = entry.key.dayOfWeek.ordinal
                        weekdays[dayOfWeekIndex].lessons.addAll(entry.value)
                    }

                return@flatMapObservable Observable.fromIterable(weekdays)
            }
    }

    private fun concatListStrings(lhs: String, rhs: String): String {
        return if (rhs != EMPTY_STRING) {
            if (lhs != EMPTY_STRING) "$lhs, $rhs" else rhs
        } else {
            lhs
        }
    }

    private fun findOpeningParenthesisIndex(str: String): Int {
        var closingParenthesisCnt = 0
        var openingParenthesisCnt = 0
        for (chIdx in str.indices) {
            val idx = str.lastIndex - chIdx
            if (str[idx] == ')') closingParenthesisCnt++
            if (str[idx] == '(') openingParenthesisCnt++

            if (closingParenthesisCnt != 0 && closingParenthesisCnt == openingParenthesisCnt) {
                return idx
            }
        }
        return str.length
    }

    private fun isClassDateInWeek(classDate: LocalDate, academicWeek: Long): Boolean {
        val academicYearFirstWeekMonday =
            currentDate.academicYearStartDate
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val dayDifference = ChronoUnit.DAYS.between(academicYearFirstWeekMonday, classDate)
        return dayDifference / 7 == academicWeek
    }

    private fun getLastDayOfAcademicWeek(academicWeek: Long): LocalDate {
        return currentDate.academicYearStartDate
            // Schedule data for the current week is usually deleted from the server on Sunday.
            // Classes cannot be scheduled on Sunday (for now), so
            // we assume Saturday is the last day of an academic week.
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
            .plusWeeks(academicWeek)
    }

    private fun getAcademicYearWeeks(): List<String> {
        val weeks = mutableListOf<String>()
        // Stay compatible with the original schedule API until it's completely gone.
        // When it is gone, we can store LocalDate objects for Monday of every week instead of
        // these strings and parse it using the user's locale when preparing data for UI.
        val locale = Locale("ru")
        val startDate = currentDate.academicYearStartDate
        val endDate = LocalDate.of(currentDate.academicYear + 1, Month.JULY, 1)
        var weekMonday = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        var weekSunday = startDate.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))

        while (weekMonday < endDate) {
            val weekMondayMonth = weekMonday.month.getDisplayName(TextStyle.FULL, locale)
            val weekSundayMonth = weekSunday.month.getDisplayName(TextStyle.FULL, locale)
            val week = "${weekMonday.dayOfMonth} $weekMondayMonth - ${weekSunday.dayOfMonth} $weekSundayMonth"
            weeks.add(week)

            weekMonday = weekMonday.plusDays(7)
            weekSunday = weekSunday.plusDays(7)
        }

        return weeks
    }

    private fun parseDateTime(dateTime: String): LocalDateTime {
        assert(dateTime != EMPTY_STRING)
        val dateTimeFormatter = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral(' ')
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .toFormatter()
        return LocalDateTime.parse(dateTime, dateTimeFormatter)
    }

    private fun formatTime(localDateTime: LocalDateTime): String {
        val dateTimeFormatter = DateTimeFormatterBuilder()
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .toFormatter()
        return localDateTime.format(dateTimeFormatter)
    }

    private fun groupKeyFromString(groupCode: String): Int {
        return if (!groupCode.contains('-')) {
            groupCode.toInt()
        } else {
            val code = groupCode.substringBefore('-').toInt()
            val subcode = groupCode.substringAfter('-').toInt()
            -(code * GROUP_CODE_OFFSET + subcode)
        }
    }

    private fun groupKeyToString(groupKey: Int): String {
        return if (groupKey >= 0) {
            groupKey.toString(10)
        } else {
            val groupCode = -groupKey
            val code = groupCode / GROUP_CODE_OFFSET
            val subcode = groupCode % GROUP_CODE_OFFSET
            "$code-$subcode"
        }
    }

    @Throws(HttpStatusException::class)
    private fun <T> checkResponse(response: Response<T>) {
        if (!response.isSuccessful) {
            throw HttpStatusException(
                response.message(),
                response.code(),
                response.raw().request().url().toString()
            )
        }
    }

    companion object {
        // Myuniversity API serves group codes not only as integers, but also as strings (e.g. "1000-7").
        // We store group keys as integer numbers, so use an offset to transform these string values to integers.
        // Maximum group code value is 202092, while maximum group subcode value is 4 (for now).
        private const val GROUP_CODE_OFFSET = 100
    }
}
