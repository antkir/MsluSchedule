package by.ntnk.msluschedule.di.modules

import by.ntnk.msluschedule.BuildConfig
import by.ntnk.msluschedule.di.PerApp
import by.ntnk.msluschedule.network.LocalCookieJar
import by.ntnk.msluschedule.network.ScheduleApi
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit

@Module
class NetworkModule {
    @Provides
    @PerApp
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        val logger: (message: String) -> Unit = {
            Timber.tag("OkHttp").d(it)
        }
        val loggerLevel = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        return HttpLoggingInterceptor(logger).setLevel(loggerLevel)
    }

    @Provides
    @PerApp
    fun provideLocalCookieJar(): LocalCookieJar {
        return LocalCookieJar()
    }

    @Provides
    @PerApp
    fun provideOkHttpClient(
            httpLoggingInterceptor: HttpLoggingInterceptor,
            localCookieJar: LocalCookieJar
    ): OkHttpClient {
        return OkHttpClient().newBuilder()
                .addNetworkInterceptor(httpLoggingInterceptor)
                .cookieJar(localCookieJar)
                .connectTimeout(2, TimeUnit.MINUTES)
                .readTimeout(2, TimeUnit.MINUTES)
                .writeTimeout(2, TimeUnit.MINUTES)
                .build()
    }

    @Provides
    @PerApp
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
                .baseUrl("http://raspisanie.mslu.by/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create())
                .client(okHttpClient)
                .build()
    }

    @Provides
    @PerApp
    fun provideScheduleApi(retrofit: Retrofit): ScheduleApi {
        return retrofit.create(ScheduleApi::class.java)
    }
}
