package by.ntnk.msluschedule.network

import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.data.WeekdayWithStudyGroupLessons
import by.ntnk.msluschedule.data.WeekdayWithTeacherLessons
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
import java.io.InputStream
import javax.inject.Inject

@PerApp
class NetworkRepository @Inject constructor(
        private val scheduleApi: ScheduleApi,
        private val networkHelper: NetworkHelper,
        private val localCookieJar: LocalCookieJar,
        private val xlsParser: XlsParser
) {
    fun getFaculties(): Single<ScheduleFilter> {
        return wrapRequest {
            getDataFromHtmlRequest(
                    NetworkHelper.groupSchedule,
                    networkHelper.facultyRequestInfo
            )
        }
    }

    fun getGroups(faculty: Int, course: Int): Single<ScheduleFilter> {
        val requestDataList = networkHelper.getStudyGroupsFilterDataList(faculty, course)
        return wrapRequest {
            getDataFromJsonRequest(
                    networkHelper.groupRequestInfo,
                    NetworkHelper.groupSchedule,
                    requestDataList
            )
        }
    }

    fun getTeachers(): Single<ScheduleFilter> {
        return wrapRequest {
            getDataFromHtmlRequest(
                    NetworkHelper.teacherSchedule,
                    networkHelper.teacherRequestInfo
            )
        }
    }

    fun getWeeks(): Single<ScheduleFilter> {
        val requestDataList = networkHelper.getYearsFilterDataList()
        return wrapRequest {
            getDataFromJsonRequest(
                    networkHelper.weekRequestInfo,
                    // Weeks are equal for groups and teachers, so we can use either request
                    NetworkHelper.teacherSchedule,
                    requestDataList
            )
        }
    }

    fun getSchedule(studyGroup: StudyGroup, weekKey: Int): Observable<WeekdayWithStudyGroupLessons> {
        val requestDataList = networkHelper.getStudyGroupRequestDataList(studyGroup, weekKey)
        return wrapRequest { getScheduleData(NetworkHelper.groupSchedule, requestDataList) }
                .flatMapObservable { xlsParser.parseStudyGroupXls(it) }
    }

    fun getSchedule(teacher: Teacher, weekKey: Int): Observable<WeekdayWithTeacherLessons> {
        val requestDataList = networkHelper.getTeacherRequestDataList(teacher, weekKey)
        return wrapRequest { getScheduleData(NetworkHelper.teacherSchedule, requestDataList) }
                .flatMapObservable { xlsParser.parseTeacherXls(it) }
    }

    /*
     * The process of giving/storing session ID is not consistent,
     * so we make sure everything will work by creating and closing
     * a session on every batch of related requests.
     */
    private inline fun <T> wrapRequest(request: () -> Single<T>): Single<T> {
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

    private fun getScheduleData(
            requestedScheduleType: String,
            requestDataList: List<RequestData>
    ): Single<InputStream> {
        return Observable.fromIterable(requestDataList)
                .flatMapSingle { changeScheduleFilter(requestedScheduleType, it) }
                .ignoreElements()
                .andThen(getScheduleInputStream(requestedScheduleType))
    }

    private fun initSession(): Completable {
        return scheduleApi
                .initSession()
                .doOnSuccess { checkErrors(it) }
                .ignoreElement()
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

    private fun getScheduleInputStream(scheduleType: String): Single<InputStream> {
        return scheduleApi
                .getSchedule(scheduleType)
                .doOnSuccess { checkErrors(it) }
                .map { it.body()?.byteStream() }
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
