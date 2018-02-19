package by.ntnk.msluschedule.network

import by.ntnk.msluschedule.di.PerApp
import by.ntnk.msluschedule.network.data.JsonBody
import by.ntnk.msluschedule.network.data.RequestData
import by.ntnk.msluschedule.network.data.RequestInfo
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.EMPTY_STRING
import by.ntnk.msluschedule.utils.InvalidYearException
import io.reactivex.Single
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.*
import javax.inject.Inject

@PerApp
class NetworkHelper @Inject constructor(private val currentDate: CurrentDate) {
    val facultyRequestInfo: RequestInfo =
            RequestInfo("faculty", studyGroupZone)
    val groupRequestInfo: RequestInfo =
            RequestInfo("studygroups", buttonZone)
    val weekRequestInfo: RequestInfo =
            RequestInfo("studyweeks", buttonZone)
    val teacherRequestInfo: RequestInfo =
            RequestInfo("teachers", buttonZone)
    private val yearRequestInfo: RequestInfo =
            RequestInfo("studyyears", studyWeekZone)
    private val courseRequestInfo: RequestInfo =
            RequestInfo("course", studyWeekZone)

    private fun createYearRequestDataInstance(value: Int): RequestData =
            RequestData(yearRequestInfo, value)

    private fun createWeekRequestDataInstance(value: Int): RequestData =
            RequestData(weekRequestInfo, value)

    private fun createCourseRequestDataInstance(value: Int): RequestData =
            RequestData(courseRequestInfo, value)

    private fun createFacultyRequestDataInstance(value: Int): RequestData =
            RequestData(facultyRequestInfo, value)

    fun getYearsFilterDataList(): List<RequestData> =
            Collections.singletonList(createYearRequestDataInstance(currentDate.academicYear))

    fun getStudyGroupsFilterDataList(faculty: Int, course: Int): List<RequestData> =
            Arrays.asList(
                    createFacultyRequestDataInstance(faculty),
                    createCourseRequestDataInstance(course),
                    // When passing 0, groups for all courses are returned
                    createYearRequestDataInstance(0)
            )

    fun parseDataFromHtmlBody(
            requestInfo: RequestInfo,
            response: String
    ): Single<ScheduleFilter> {
        return try {
            val document = Jsoup.parse(response)
            checkYearValid(document)
            Single.just(parseResponse(document, requestInfo))
        } catch (e: Exception) {
            Single.error(e)
        }
    }

    fun parseDataFromJsonResponse(
            requestInfo: RequestInfo,
            jsonBody: JsonBody
    ): Single<ScheduleFilter> {
        val htmlFragment: String = when (requestInfo.requestName) {
            groupRequestInfo.requestName -> jsonBody.studyGroupZone
            weekRequestInfo.requestName -> jsonBody.studyWeekZone
            else -> EMPTY_STRING
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
                .select("select[id^=" + requestInfo.requestName + "]")
                .first()
                .children()
        val data = LinkedHashMap<Int, String>()
        elements
                .filter { it.`val`().isNotBlank() && it.text().length > 1 }
                .forEach { data[it.`val`().toInt()] = it.text() }
        return ScheduleFilter(data)
    }

    @Throws(NullPointerException::class, InvalidYearException::class)
    private fun checkYearValid(htmlBody: Document) {
        val elements = htmlBody
                .select("select[id^=" + yearRequestInfo.requestName + "]")
                .first()
                .children()

        val hasValidYear = elements
                .map { it.`val`() }
                .filter { it.isNotBlank() }
                .map { Integer.parseInt(it) }
                .filter { it == currentDate.academicYear }
                .any()

        if (!hasValidYear) throw InvalidYearException()
    }

    fun getFormIdPair(scheduleType: String, requestData: RequestData): Pair<String, String> {
        var formID = ""
        var formComponentID = ""
        if (scheduleType == teacherSchedule) {
            if (requestData.requestName == teacherRequestInfo.requestName ||
                    requestData.requestName == weekRequestInfo.requestName) {
                formID = teacherFormID
                formComponentID = teacherFormComponentID
            } else if (requestData.requestName == yearRequestInfo.requestName) {
                formID = teacherYearFormID
                formComponentID = teacherYearFormComponentID
            }
        } else if (scheduleType == groupSchedule) {
            formID = groupFormID
            formComponentID = groupFormComponentID
        }

        return Pair(formID, formComponentID)
    }

    companion object {
        const val groupSchedule = "schedulelistforgroupreport"
        const val teacherSchedule = "schedulelistteacherreport"
        private const val groupFormID = "printForm"
        private const val groupFormComponentID =
                "reports/publicreports/ScheduleListForGroupReport:printform"
        private const val teacherFormID = "buttonForm"
        private const val teacherFormComponentID =
                "reports/publicreports/ScheduleListTeacherReport:buttonForm"
        private const val teacherYearFormID = "requiredFilters"
        private const val teacherYearFormComponentID =
                "reports/publicreports/ScheduleListTeacherReport:requiredfilters"
        private const val studyGroupZone = "studyGroupZone"
        private const val buttonZone = "buttonZone"
        private const val studyWeekZone = "studyWeekZone"
    }
}
