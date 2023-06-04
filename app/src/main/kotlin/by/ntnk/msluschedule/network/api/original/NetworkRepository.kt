package by.ntnk.msluschedule.network.api.original

import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.data.WeekdayWithStudyGroupLessons
import by.ntnk.msluschedule.data.WeekdayWithTeacherLessons
import by.ntnk.msluschedule.di.PerApp
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.network.api.original.data.JsonBody
import by.ntnk.msluschedule.network.api.original.data.RequestData
import by.ntnk.msluschedule.network.api.original.data.RequestInfo
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.DEFAULT
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.jsoup.HttpStatusException
import retrofit2.Response
import java.io.InputStream
import javax.inject.Inject

@PerApp
class NetworkRepository @Inject constructor(
    private val localCookieJar: LocalCookieJar,
    private val networkHelper: NetworkHelper,
    private val sharedPreferencesRepository: SharedPreferencesRepository,
    private val scheduleService: ScheduleService,
    private val xlsParser: XlsParser
) : NetworkRepository {

    override fun getFaculties(): Single<ScheduleFilter> {
        return wrapRequest {
            getDataFromHtmlRequest(NetworkHelper.GROUP_SCHEDULE, NetworkHelper.FACULTY_REQUEST_INFO)
        }
    }

    override fun getCourses(facultyKey: Int): Single<ScheduleFilter> {
        return wrapRequest {
            getDataFromHtmlRequest(NetworkHelper.GROUP_SCHEDULE, NetworkHelper.COURSE_REQUEST_INFO)
        }
    }

    override fun getGroups(facultyKey: Int, courseKey: Int, yearKey: Int): Single<ScheduleFilter> {
        val requestDataList = networkHelper.getStudyGroupsFilterDataList(facultyKey, courseKey, yearKey)
        return wrapRequest {
            getDataFromJsonRequest(NetworkHelper.GROUP_REQUEST_INFO, NetworkHelper.GROUP_SCHEDULE, requestDataList)
        }
    }

    override fun getTeachers(): Single<ScheduleFilter> {
        return wrapRequest {
            getDataFromHtmlRequest(NetworkHelper.TEACHER_SCHEDULE, NetworkHelper.TEACHER_REQUEST_INFO)
        }
    }

    override fun getWeeks(): Single<ScheduleFilter> {
        val requestDataList = networkHelper.getYearsFilterDataList()
        return wrapRequest {
            getDataFromJsonRequest(
                NetworkHelper.WEEK_REQUEST_INFO,
                // Weeks are equal for groups and teachers, so we can use either request
                NetworkHelper.TEACHER_SCHEDULE,
                requestDataList
            )
        }
    }

    override fun getSchedule(studyGroup: StudyGroup, weekKey: Int): Observable<WeekdayWithStudyGroupLessons> {
        val requestDataList = networkHelper.getStudyGroupRequestDataList(studyGroup, weekKey)
        return wrapRequest { getScheduleData(NetworkHelper.GROUP_SCHEDULE, requestDataList) }
            .flatMapObservable { xlsStream -> xlsParser.parseStudyGroupXls(xlsStream) }
    }

    override fun getSchedule(teacher: Teacher, weekKey: Int): Observable<WeekdayWithTeacherLessons> {
        val requestDataList = networkHelper.getTeacherRequestDataList(teacher, weekKey)
        return wrapRequest { getScheduleData(NetworkHelper.TEACHER_SCHEDULE, requestDataList) }
            .flatMapObservable { xlsStream -> xlsParser.parseTeacherXls(xlsStream) }
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
            .flatMapSingle { requestData -> changeScheduleFilter(scheduleType, requestData) }
            .lastOrError()
            .flatMap { jsonResponse -> networkHelper.parseDataFromJsonResponse(requestInfo, jsonResponse) }
    }

    private fun getDataFromHtmlRequest(scheduleType: String, scheduleFilter: RequestInfo): Single<ScheduleFilter> {
        return getHtmlBody(scheduleType)
            .flatMap { htmlBody -> networkHelper.parseDataFromHtmlBody(scheduleFilter, htmlBody) }
    }

    private fun getScheduleData(
        requestedScheduleType: String,
        requestDataList: List<RequestData>
    ): Single<InputStream> {
        return Observable.fromIterable(requestDataList)
            .flatMapSingle { requestData -> changeScheduleFilter(requestedScheduleType, requestData) }
            .flatMapCompletable {
                if (sharedPreferencesRepository.isFullSubjectNameUsed()) {
                    val filterData = networkHelper.getSubjectLengthFilterData(isFullSubjectName = true)
                    changeScheduleFilter(NetworkHelper.GROUP_SCHEDULE, filterData)
                        .ignoreElement()
                } else {
                    Completable.complete()
                }
            }
            .andThen(getScheduleInputStream(requestedScheduleType))
    }

    private fun initSession(): Completable {
        return scheduleService
            .initSession()
            .doOnSuccess { response -> checkErrors(response) }
            .ignoreElement()
    }

    private fun changeScheduleFilter(scheduleType: String, requestData: RequestData): Single<JsonBody> {
        val formIds = networkHelper.getFormIdPair(scheduleType, requestData)
        return scheduleService
            .changeScheduleFilter(
                scheduleType,
                requestData.requestName,
                requestData.requestRelatedName,
                formIds.first,
                formIds.second,
                requestData.selectedValue
            )
            .doOnSuccess { response -> checkErrors(response) }
            .map { response -> response.body() }
    }

    private fun getHtmlBody(scheduleType: String): Single<String> {
        return scheduleService
            .getHtmlBody(scheduleType)
            .doOnSuccess { response -> checkErrors(response) }
            .map { response -> response.body()?.string() ?: String.DEFAULT }
    }

    private fun getScheduleInputStream(scheduleType: String): Single<InputStream> {
        return scheduleService
            .getSchedule(scheduleType)
            .doOnSuccess { response -> checkErrors(response) }
            .map { response -> response.body()?.byteStream() }
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
