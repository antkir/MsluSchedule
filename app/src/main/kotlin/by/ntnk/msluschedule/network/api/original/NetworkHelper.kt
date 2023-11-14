package by.ntnk.msluschedule.network.api.original

import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.di.PerApp
import by.ntnk.msluschedule.network.api.original.data.JsonBody
import by.ntnk.msluschedule.network.api.original.data.RequestData
import by.ntnk.msluschedule.network.api.original.data.RequestInfo
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.DEFAULT
import by.ntnk.msluschedule.utils.InvalidYearException
import by.ntnk.msluschedule.utils.toInt
import io.reactivex.rxjava3.core.Single
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.inject.Inject

@PerApp
class NetworkHelper @Inject constructor(private val currentDate: CurrentDate) {

    private fun createYearRequestDataInstance(key: Int) = RequestData(YEAR_REQUEST_INFO, key)

    private fun createWeekRequestDataInstance(key: Int) = RequestData(WEEK_REQUEST_INFO, key)

    private fun createCourseRequestDataInstance(key: Int) = RequestData(COURSE_REQUEST_INFO, key)

    private fun createFacultyRequestDataInstance(key: Int) = RequestData(FACULTY_REQUEST_INFO, key)

    private fun createStudyGroupRequestDataInstance(key: Int) = RequestData(GROUP_REQUEST_INFO, key)

    private fun createTeacherRequestDataInstance(key: Int) = RequestData(TEACHER_REQUEST_INFO, key)

    private fun createSubjectLengthRequestDataInstance(key: Int) = RequestData(SUBJECT_LENGTH_REQUEST_INFO, key)

    fun getSubjectLengthFilterData(isFullSubjectName: Boolean): RequestData =
        createSubjectLengthRequestDataInstance(isFullSubjectName.toInt())

    fun getYearsFilterDataList(): List<RequestData> =
        listOf(createYearRequestDataInstance(currentDate.academicYear))

    fun getStudyGroupsFilterDataList(facultyKey: Int, courseKey: Int, year: Int = 0): List<RequestData> =
        listOf(
            createFacultyRequestDataInstance(facultyKey),
            createCourseRequestDataInstance(courseKey),
            // When passing 0, groups for all courses are returned
            createYearRequestDataInstance(year)
        )

    fun getStudyGroupRequestDataList(studyGroup: StudyGroup, weekKey: Int): List<RequestData> =
        listOf(
            createYearRequestDataInstance(studyGroup.year),
            createWeekRequestDataInstance(weekKey),
            createFacultyRequestDataInstance(studyGroup.faculty),
            createCourseRequestDataInstance(studyGroup.course),
            createStudyGroupRequestDataInstance(studyGroup.key.toInt())
        )

    fun getTeacherRequestDataList(teacher: Teacher, weekKey: Int): List<RequestData> =
        listOf(
            createYearRequestDataInstance(teacher.year),
            createWeekRequestDataInstance(weekKey),
            createTeacherRequestDataInstance(teacher.key.toInt())
        )

    fun parseDataFromHtmlBody(requestInfo: RequestInfo, htmlBody: String): Single<ScheduleFilter> {
        return try {
            val document = Jsoup.parse(htmlBody)
            checkYearValid(document)
            Single.just(parseResponse(document, requestInfo))
        } catch (e: Exception) {
            Single.error(e)
        }
    }

    fun parseDataFromJsonResponse(requestInfo: RequestInfo, jsonResponse: JsonBody): Single<ScheduleFilter> {
        val htmlFragment: String = when (requestInfo.requestName) {
            GROUP_REQUEST_INFO.requestName -> jsonResponse.studyGroupZone
            WEEK_REQUEST_INFO.requestName -> jsonResponse.studyWeekZone
            else -> String.DEFAULT
        }
        return try {
            val document = Jsoup.parse(htmlFragment)
            Single.just(parseResponse(document, requestInfo))
        } catch (e: Exception) {
            Single.error(e)
        }
    }

    private fun parseResponse(document: Document, requestInfo: RequestInfo): ScheduleFilter {
        val elements = document
            .select("select[id^=${requestInfo.requestName}]")
            .first()
            ?.children()
        val data = ScheduleFilter()
        elements
            ?.filter { element -> element.`val`().isNotBlank() }
            ?.forEach { element -> data.put(element.`val`(), element.text()) }
        return data
    }

    private fun checkYearValid(htmlBody: Document) {
        val elements = htmlBody
            .select("select[id^=${YEAR_REQUEST_INFO.requestName}]")
            .first()
            ?.children()

        val hasValidYear = elements
            ?.eachAttr("value")
            ?.filter { yearString -> yearString.isNotBlank() }
            ?.map { yearString -> Integer.parseInt(yearString) }
            ?.any { year -> year == currentDate.academicYear }

        if (hasValidYear != true) {
            throw InvalidYearException()
        }
    }

    fun getFormIdPair(scheduleType: String, requestData: RequestData): Pair<String, String> {
        var formID = ""
        var formComponentID = ""
        if (scheduleType == TEACHER_SCHEDULE) {
            if (requestData.requestName == TEACHER_REQUEST_INFO.requestName ||
                requestData.requestName == WEEK_REQUEST_INFO.requestName
            ) {
                formID = TEACHER_FORM_ID
                formComponentID = TEACHER_FORM_COMPONENT_ID
            } else if (requestData.requestName == YEAR_REQUEST_INFO.requestName) {
                formID = TEACHER_YEAR_FORM_ID
                formComponentID = TEACHER_YEAR_FORM_COMPOONENT_ID
            }
        } else if (scheduleType == GROUP_SCHEDULE) {
            formID = GROUP_FORM_ID
            formComponentID = GROUP_FORM_COMPONENT_ID
        }

        return Pair(formID, formComponentID)
    }

    companion object {
        const val GROUP_SCHEDULE = "schedulelistforgroupreport"
        const val TEACHER_SCHEDULE = "schedulelistteacherreport"

        private const val GROUP_FORM_ID = "printForm"
        private const val GROUP_FORM_COMPONENT_ID = "reports/publicreports/ScheduleListForGroupReport:printform"
        private const val TEACHER_FORM_ID = "buttonForm"
        private const val TEACHER_FORM_COMPONENT_ID = "reports/publicreports/ScheduleListTeacherReport:buttonForm"
        private const val TEACHER_YEAR_FORM_ID = "requiredFilters"
        private const val TEACHER_YEAR_FORM_COMPOONENT_ID =
            "reports/publicreports/ScheduleListTeacherReport:requiredfilters"
        private const val STUDY_GROUP_ZONE = "studyGroupZone"
        private const val BUTTON_ZONE = "buttonZone"
        private const val STUDY_WEEK_ZONE = "studyWeekZone"

        val FACULTY_REQUEST_INFO = RequestInfo("faculty", STUDY_GROUP_ZONE)
        val COURSE_REQUEST_INFO = RequestInfo("course", STUDY_WEEK_ZONE)
        val GROUP_REQUEST_INFO = RequestInfo("studygroups", BUTTON_ZONE)
        val WEEK_REQUEST_INFO = RequestInfo("studyweeks", BUTTON_ZONE)
        val TEACHER_REQUEST_INFO = RequestInfo("teachers", BUTTON_ZONE)

        private val YEAR_REQUEST_INFO = RequestInfo("studyyears", STUDY_WEEK_ZONE)
        private val SUBJECT_LENGTH_REQUEST_INFO = RequestInfo("printnamesubject", BUTTON_ZONE)
    }
}
