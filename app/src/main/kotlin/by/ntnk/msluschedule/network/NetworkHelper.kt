package by.ntnk.msluschedule.network

import by.ntnk.msluschedule.di.PerApp
import by.ntnk.msluschedule.network.data.JsonBody
import by.ntnk.msluschedule.network.data.RequestData
import by.ntnk.msluschedule.network.data.RequestInfo
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.CurrentDate
import io.reactivex.Single
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

    fun parseDataFromJsonResponse(
            requestInfo: RequestInfo,
            jsonResponse: JsonBody
    ): Single<ScheduleFilter> {
        TODO("not implemented")
    }

    fun parseDataFromHtmlBody(
            requestInfo: RequestInfo,
            responseString: String
    ): Single<ScheduleFilter> {
        TODO("not implemented")
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
