package by.ntnk.msluschedule.di.modules

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

@Module
class NetworkModule {
    @Provides
    @PerApp
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
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
