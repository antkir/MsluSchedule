package by.ntnk.msluschedule.network.api.myuniversity

import by.ntnk.msluschedule.network.api.myuniversity.data.JsonGroupClasses
import by.ntnk.msluschedule.network.api.myuniversity.data.JsonGroups
import by.ntnk.msluschedule.network.api.myuniversity.data.JsonTeacherClasses
import by.ntnk.msluschedule.network.api.myuniversity.data.JsonTeachers
import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ScheduleService {

    @GET(".")
    fun getStudyGroups(): Single<Response<JsonGroups>>

    @GET("teachers")
    fun getTeachers(): Single<Response<JsonTeachers>>

    @GET("{groupId}")
    fun getGroupClasses(@Path("groupId") groupId: String): Single<Response<JsonGroupClasses>>

    @GET("teacher")
    fun getTeacherClasses(@Query("teacher") teacherName: String): Single<Response<JsonTeacherClasses>>
}
