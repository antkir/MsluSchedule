package by.ntnk.msluschedule.network

import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.di.PerApp
import by.ntnk.msluschedule.network.data.JsonBody
import by.ntnk.msluschedule.network.data.RequestData
import by.ntnk.msluschedule.network.data.RequestInfo
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.EMPTY_STRING
import by.ntnk.msluschedule.utils.InvalidYearException
import by.ntnk.msluschedule.utils.toInt
import io.reactivex.Single
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.inject.Inject

@PerApp
class NetworkHelper @Inject constructor(private val currentDate: CurrentDate) {
    val facultyRequestInfo = RequestInfo("faculty", studyGroupZone)
    val courseRequestInfo = RequestInfo("course", studyWeekZone)
    val groupRequestInfo = RequestInfo("studygroups", buttonZone)
    val weekRequestInfo = RequestInfo("studyweeks", buttonZone)
    val teacherRequestInfo = RequestInfo("teachers", buttonZone)
    private val yearRequestInfo = RequestInfo("studyyears", studyWeekZone)
    private val subjectLengthRequestInfo = RequestInfo("printnamesubject", buttonZone)

    private fun createYearRequestDataInstance(value: Int) = RequestData(yearRequestInfo, value)

    private fun createWeekRequestDataInstance(value: Int) = RequestData(weekRequestInfo, value)

    private fun createCourseRequestDataInstance(value: Int) = RequestData(courseRequestInfo, value)

    private fun createFacultyRequestDataInstance(value: Int) = RequestData(facultyRequestInfo, value)

    private fun createStudyGroupRequestDataInstance(value: Int) = RequestData(groupRequestInfo, value)

    private fun createTeacherRequestDataInstance(value: Int) = RequestData(teacherRequestInfo, value)

    private fun createSubjectLengthRequestDataInstance(value: Int) = RequestData(subjectLengthRequestInfo, value)

    fun getSubjectLengthFilterData(isFullSubjectName: Boolean): RequestData =
        createSubjectLengthRequestDataInstance(isFullSubjectName.toInt())

    fun getYearsFilterDataList(): List<RequestData> =
        listOf(createYearRequestDataInstance(currentDate.academicYear))

    fun getStudyGroupsFilterDataList(faculty: Int, course: Int, year: Int = 0): List<RequestData> =
        listOf(
            createFacultyRequestDataInstance(faculty),
            createCourseRequestDataInstance(course),
            // When passing 0, groups for all courses are returned
            createYearRequestDataInstance(year)
        )

    fun getStudyGroupRequestDataList(studyGroup: StudyGroup, weekKey: Int): List<RequestData> =
        listOf(
            createYearRequestDataInstance(studyGroup.year),
            createWeekRequestDataInstance(weekKey),
            createFacultyRequestDataInstance(studyGroup.faculty),
            createCourseRequestDataInstance(studyGroup.course),
            createStudyGroupRequestDataInstance(studyGroup.key)
        )

    fun getTeacherRequestDataList(teacher: Teacher, weekKey: Int): List<RequestData> =
        listOf(
            createYearRequestDataInstance(teacher.year),
            createWeekRequestDataInstance(weekKey),
            createTeacherRequestDataInstance(teacher.key)
        )

    fun parseDataFromHtmlBody(requestInfo: RequestInfo, response: String): Single<ScheduleFilter> {
        return try {
            val document = Jsoup.parse(response)
            checkYearValid(document)
            Single.just(parseResponse(document, requestInfo))
        } catch (e: Exception) {
            Single.error(e)
        }
    }

    fun parseDataFromJsonResponse(requestInfo: RequestInfo, jsonBody: JsonBody): Single<ScheduleFilter> {
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
            .select("select[id^=${requestInfo.requestName}]")
            .first()
            ?.children()
        val data = ScheduleFilter()
        elements
            ?.filter { element -> element.`val`().isNotBlank() }
            ?.forEach { data.put(it.`val`().toInt(), it.text()) }
        return data
    }

    @Throws(InvalidYearException::class)
    private fun checkYearValid(htmlBody: Document) {
        val elements = htmlBody
            .select("select[id^=${yearRequestInfo.requestName}]")
            .first()
            ?.children()

        val hasValidYear = elements
            ?.eachAttr("value")
            ?.filter { it.isNotBlank() }
            ?.map { Integer.parseInt(it) }
            ?.any { it == currentDate.academicYear }

        if (hasValidYear != true) throw InvalidYearException()
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
        private const val groupFormComponentID = "reports/publicreports/ScheduleListForGroupReport:printform"
        private const val teacherFormID = "buttonForm"
        private const val teacherFormComponentID = "reports/publicreports/ScheduleListTeacherReport:buttonForm"
        private const val teacherYearFormID = "requiredFilters"
        private const val teacherYearFormComponentID = "reports/publicreports/ScheduleListTeacherReport:requiredfilters"
        private const val studyGroupZone = "studyGroupZone"
        private const val buttonZone = "buttonZone"
        private const val studyWeekZone = "studyWeekZone"
    }
}
