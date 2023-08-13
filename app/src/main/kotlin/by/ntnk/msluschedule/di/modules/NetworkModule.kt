package by.ntnk.msluschedule.di.modules

import by.ntnk.msluschedule.BuildConfig
import by.ntnk.msluschedule.di.PerApp
import by.ntnk.msluschedule.network.LocalCookieJar
import by.ntnk.msluschedule.network.dns.MsluDns
import by.ntnk.msluschedule.network.ScheduleApi
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Named

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
        @Named("cache") cacheDir: File,
        httpLoggingInterceptor: HttpLoggingInterceptor,
        localCookieJar: LocalCookieJar
    ): OkHttpClient {
        // DNS name server setup is not correct for raspisanie.mslu.by and can return a local IP address.
        // Check which IP address is cached by Android and query correct name server if it's a local IP.
        val dns = MsluDns.Builder()
            .url("ns1.mslu.by")
            .cacheDir(cacheDir)
            .includeIPv6(false)
            .build()

        return OkHttpClient().newBuilder()
            .addNetworkInterceptor(httpLoggingInterceptor)
            .cookieJar(localCookieJar)
            .dns(dns)
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
