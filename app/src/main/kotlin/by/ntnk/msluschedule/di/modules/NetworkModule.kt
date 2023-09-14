package by.ntnk.msluschedule.di.modules

import by.ntnk.msluschedule.BuildConfig
import by.ntnk.msluschedule.di.PerApp
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.network.api.original.LocalCookieJar
import by.ntnk.msluschedule.network.api.original.dns.MsluDns
import by.ntnk.msluschedule.utils.NetworkApiVersion
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import dagger.Lazy
import by.ntnk.msluschedule.network.api.original.NetworkRepository as NetworkRepositoryOriginal
import by.ntnk.msluschedule.network.api.original.ScheduleService as ScheduleServiceOriginal
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
import javax.inject.Qualifier

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
    @Api(NetworkApiVersion.ORIGINAL.name)
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
    @Api(NetworkApiVersion.ORIGINAL.name)
    fun provideRetrofit(@Api(NetworkApiVersion.ORIGINAL.name) okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://raspisanie.mslu.by/")
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    @Provides
    @PerApp
    fun provideScheduleServiceOriginal(
        @Api(NetworkApiVersion.ORIGINAL.name) retrofit: Retrofit
    ): ScheduleServiceOriginal {
        return retrofit.create(ScheduleServiceOriginal::class.java)
    }

    @Provides
    @PerApp
    fun provideNetworkRepository(
        sharedPreferencesRepository: SharedPreferencesRepository,
        networkRepositoryOriginal: Lazy<NetworkRepositoryOriginal>
    ): NetworkRepository {
        return when (sharedPreferencesRepository.getCurrentNetworkApiVersion()) {
            NetworkApiVersion.ORIGINAL -> networkRepositoryOriginal.get()
        }
    }

    private companion object {
        @Qualifier
        @Retention(AnnotationRetention.RUNTIME)
        annotation class Api(val value: String)
    }
}
