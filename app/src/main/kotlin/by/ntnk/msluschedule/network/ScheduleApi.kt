package by.ntnk.msluschedule.network

import by.ntnk.msluschedule.network.data.JsonBody
import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ScheduleApi {
    @HEAD("schedule/login")
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
            @Field("t:selectvalue") selectValue: Int): Single<Response<JsonBody>>

    @GET("schedule/reports/publicreports/{scheduletype}.printreport")
    fun getSchedule(@Path("scheduletype") scheduleType: String): Single<Response<ResponseBody>>
}
