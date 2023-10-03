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
import by.ntnk.msluschedule.utils.EMPTY_STRING
import by.ntnk.msluschedule.utils.HttpStatusException
import by.ntnk.msluschedule.utils.NetworkApiVersion
import by.ntnk.msluschedule.utils.NetworkApiVersionException
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import by.ntnk.msluschedule.utils.getNetworkApiVersionFromWeekKey
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
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
            getDataFromHtmlRequest(NetworkHelper.groupSchedule, networkHelper.facultyRequestInfo)
        }
    }

    override fun getCourses(facultyKey: Int): Single<ScheduleFilter> {
        return wrapRequest {
            getDataFromHtmlRequest(NetworkHelper.groupSchedule, networkHelper.courseRequestInfo)
        }
    }

    override fun getGroups(facultyKey: Int, courseKey: Int, yearKey: Int): Single<ScheduleFilter> {
        val requestDataList = networkHelper.getStudyGroupsFilterDataList(facultyKey, courseKey, yearKey)
        return wrapRequest {
            getDataFromJsonRequest(networkHelper.groupRequestInfo, NetworkHelper.groupSchedule, requestDataList)
        }
    }

    override fun getTeachers(): Single<ScheduleFilter> {
        return wrapRequest {
            getDataFromHtmlRequest(NetworkHelper.teacherSchedule, networkHelper.teacherRequestInfo)
        }
    }

    override fun getWeeks(): Single<ScheduleFilter> {
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

    override fun getSchedule(studyGroup: StudyGroup, weekKey: Int): Observable<WeekdayWithStudyGroupLessons> {
        if (getNetworkApiVersionFromWeekKey(weekKey) != NetworkApiVersion.ORIGINAL) {
            throw NetworkApiVersionException()
        }

        val requestDataList = networkHelper.getStudyGroupRequestDataList(studyGroup, weekKey)
        return wrapRequest { getScheduleData(NetworkHelper.groupSchedule, requestDataList) }
            .flatMapObservable { xlsParser.parseStudyGroupXls(it) }
    }

    override fun getSchedule(teacher: Teacher, weekKey: Int): Observable<WeekdayWithTeacherLessons> {
        if (getNetworkApiVersionFromWeekKey(weekKey) != NetworkApiVersion.ORIGINAL) {
            throw NetworkApiVersionException()
        }

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

    private fun getDataFromHtmlRequest(scheduleType: String, scheduleFilter: RequestInfo): Single<ScheduleFilter> {
        return getHtmlBody(scheduleType)
            .flatMap { networkHelper.parseDataFromHtmlBody(scheduleFilter, it) }
    }

    private fun getScheduleData(
        requestedScheduleType: String,
        requestDataList: List<RequestData>
    ): Single<InputStream> {
        return Observable.fromIterable(requestDataList)
            .flatMapSingle { changeScheduleFilter(requestedScheduleType, it) }
            .flatMapCompletable {
                return@flatMapCompletable if (sharedPreferencesRepository.isFullSubjectNameUsed()) {
                    val filterData = networkHelper.getSubjectLengthFilterData(isFullSubjectName = true)
                    changeScheduleFilter(NetworkHelper.groupSchedule, filterData).ignoreElement()
                } else {
                    Completable.complete()
                }
            }
            .andThen(getScheduleInputStream(requestedScheduleType))
    }

    private fun initSession(): Completable {
        return scheduleService
            .initSession()
            .doOnSuccess { checkErrors(it) }
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
            .doOnSuccess { checkErrors(it) }
            .map { it.body() }
    }

    private fun getHtmlBody(scheduleType: String): Single<String> {
        return scheduleService
            .getHtmlBody(scheduleType)
            .doOnSuccess { checkErrors(it) }
            .map { it.body()?.string() ?: EMPTY_STRING }
    }

    private fun getScheduleInputStream(scheduleType: String): Single<InputStream> {
        return scheduleService
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
