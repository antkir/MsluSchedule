package by.ntnk.msluschedule.network

import by.ntnk.msluschedule.di.PerApp
import by.ntnk.msluschedule.network.data.JsonBody
import by.ntnk.msluschedule.network.data.RequestData
import by.ntnk.msluschedule.network.data.RequestInfo
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.EMPTY_STRING
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

@PerApp
class NetworkRepository @Inject
constructor(
        private val scheduleApi: ScheduleApi,
        private val networkHelper: NetworkHelper
) {
    private val mainBackgroundScheduler = Schedulers.single()

    fun getFaculties(): Single<ScheduleFilter> {
        return getDataFromHtmlRequest(
                NetworkHelper.groupSchedule,
                networkHelper.facultyRequestInfo,
                mainBackgroundScheduler
        )
    }

    fun getGroups(faculty: Int, course: Int): Single<ScheduleFilter> {
        val requestDataList = networkHelper.getStudyGroupsFilterDataList(faculty, course)
        return getDataFromJsonRequest(
                networkHelper.groupRequestInfo,
                NetworkHelper.groupSchedule,
                requestDataList,
                mainBackgroundScheduler
        )
    }

    fun getTeachers(): Single<ScheduleFilter> {
        return getDataFromHtmlRequest(
                NetworkHelper.teacherSchedule,
                networkHelper.teacherRequestInfo,
                mainBackgroundScheduler
        )
    }

    fun getWeeks(): Single<ScheduleFilter> {
        val requestDataList = networkHelper.getYearsFilterDataList()
        return getDataFromJsonRequest(
                networkHelper.weekRequestInfo,
                // Weeks are equal for groups and teachers, so we can use either request
                NetworkHelper.teacherSchedule,
                requestDataList,
                mainBackgroundScheduler
        )
    }

    private fun getDataFromJsonRequest(
            requestInfo: RequestInfo,
            scheduleType: String,
            requestDataList: List<RequestData>,
            scheduler: Scheduler
    ): Single<ScheduleFilter> {
        return Observable
                .fromIterable(requestDataList)
                .flatMapSingle { changeScheduleFilter(scheduleType, it, scheduler) }
                .lastOrError()
                .flatMap { networkHelper.parseDataFromJsonResponse(requestInfo, it) }
    }

    private fun getDataFromHtmlRequest(
            scheduleType: String,
            scheduleFilter: RequestInfo,
            scheduler: Scheduler
    ): Single<ScheduleFilter> {
        return getHtmlBody(scheduleType, scheduler)
                .flatMap { networkHelper.parseDataFromHtmlBody(scheduleFilter, it) }
    }

    private fun changeScheduleFilter(
            scheduleType: String,
            requestData: RequestData,
            scheduler: Scheduler
    ): Single<JsonBody> {
        val formIds = networkHelper.getFormIdPair(scheduleType, requestData)
        return scheduleApi
                .changeScheduleFilter(
                        scheduleType,
                        requestData.requestName,
                        requestData.requestRelatedName,
                        formIds.first,
                        formIds.second,
                        requestData.selectedValue)
                .subscribeOn(scheduler)
                .map { it.body() }
    }


    private fun getHtmlBody(
            scheduleType: String,
            scheduler: Scheduler
    ): Single<String> {
        return scheduleApi
                .getHtmlBody(scheduleType)
                .subscribeOn(scheduler)
                .map { it.body()?.string() ?: EMPTY_STRING }
    }
}
