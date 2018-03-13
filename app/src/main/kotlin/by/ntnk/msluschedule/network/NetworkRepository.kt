package by.ntnk.msluschedule.network

import by.ntnk.msluschedule.di.PerApp
import by.ntnk.msluschedule.network.data.JsonBody
import by.ntnk.msluschedule.network.data.RequestData
import by.ntnk.msluschedule.network.data.RequestInfo
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.EMPTY_STRING
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.jsoup.HttpStatusException
import retrofit2.Response
import javax.inject.Inject

@PerApp
class NetworkRepository @Inject
constructor(
        private val scheduleApi: ScheduleApi,
        private val networkHelper: NetworkHelper,
        private val localCookieJar: LocalCookieJar
) {
    fun getFaculties(): Single<ScheduleFilter> {
        fun getFaculties() = getDataFromHtmlRequest(
                NetworkHelper.groupSchedule,
                networkHelper.facultyRequestInfo
        )
        return wrapRequest(::getFaculties)
    }

    fun getGroups(faculty: Int, course: Int): Single<ScheduleFilter> {
        val requestDataList = networkHelper.getStudyGroupsFilterDataList(faculty, course)
        fun getGroups() = getDataFromJsonRequest(
                networkHelper.groupRequestInfo,
                NetworkHelper.groupSchedule,
                requestDataList
        )
        return wrapRequest(::getGroups)
    }

    fun getTeachers(): Single<ScheduleFilter> {
        fun getTeachers() = getDataFromHtmlRequest(
                NetworkHelper.teacherSchedule,
                networkHelper.teacherRequestInfo
        )
        return wrapRequest(::getTeachers)
    }

    fun getWeeks(): Single<ScheduleFilter> {
        val requestDataList = networkHelper.getYearsFilterDataList()
        fun getWeeks() = getDataFromJsonRequest(
                networkHelper.weekRequestInfo,
                // Weeks are equal for groups and teachers, so we can use either request
                NetworkHelper.teacherSchedule,
                requestDataList
        )
        return wrapRequest(::getWeeks)
    }

    /*
     * The process of giving/storing session ID is not consistent,
     * so we make sure everything will work by creating and closing
     * a session on every batch of related requests.
     */
    private fun <T> wrapRequest(request: () -> Single<T>): Single<T> {
        return initSession()
                .andThen(request())
                .doOnEvent { _, _ -> closeSession() }
    }

    private fun closeSession() = localCookieJar.removeCookie()

    private fun getDataFromJsonRequest(
            requestInfo: RequestInfo,
            scheduleType: String,
            requestDataList: List<RequestData>
    ): Single<ScheduleFilter> {
        return Observable
                .fromIterable(requestDataList)
                .flatMapSingle { changeScheduleFilter(scheduleType, it) }
                .lastOrError()
                .flatMap { networkHelper.parseDataFromJsonResponse(requestInfo, it) }
    }

    private fun getDataFromHtmlRequest(
            scheduleType: String,
            scheduleFilter: RequestInfo
    ): Single<ScheduleFilter> {
        return getHtmlBody(scheduleType)
                .flatMap { networkHelper.parseDataFromHtmlBody(scheduleFilter, it) }
    }

    private fun initSession(): Completable {
        return scheduleApi
                .initSession()
                .doOnSuccess { checkErrors(it) }
                .toCompletable()
    }

    private fun changeScheduleFilter(
            scheduleType: String,
            requestData: RequestData
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
                .doOnSuccess { checkErrors(it) }
                .map { it.body() }
    }


    private fun getHtmlBody(scheduleType: String): Single<String> {
        return scheduleApi
                .getHtmlBody(scheduleType)
                .doOnSuccess { checkErrors(it) }
                .map { it.body()?.string() ?: EMPTY_STRING }
    }

    @Throws(HttpStatusException::class)
    private fun <T> checkErrors(response: Response<T>) {
        if (!response.isSuccessful) {
            throw HttpStatusException(
                    response.message(),
                    response.code(),
                    response.raw().request().url().toString()
            )
        }
    }
}
