package by.ntnk.msluschedule.network.api.original

import by.ntnk.msluschedule.network.api.original.data.JsonBody
import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface ScheduleService {
    // scheduletype doesn't matter here, session ID is shared across the website
    @HEAD("schedule/reports/publicreports/schedulelistforgroupreport")
    fun initSession(): Single<Response<Void>>

    @GET("schedule/reports/publicreports/{scheduletype}")
    fun getHtmlBody(@Path("scheduletype") scheduleType: String): Single<Response<ResponseBody>>

    @FormUrlEncoded
    @Headers("X-Requested-With: XMLHttpRequest")
    @POST("schedule/reports/publicreports/{scheduletype}.{option}:change")
    fun changeScheduleFilter(
        @Path("scheduletype") scheduleType: String,
        @Path("option") option: String,
        @Field("t:zoneid") zoneId: String,
        @Field("t:formid") formId: String,
        @Field("t:formcomponentid") formComponentid: String,
        @Field("t:selectvalue") selectValue: Int
    ): Single<Response<JsonBody>>

    @GET("schedule/reports/publicreports/{scheduletype}.printreport")
    fun getSchedule(@Path("scheduletype") scheduleType: String): Single<Response<ResponseBody>>
}
