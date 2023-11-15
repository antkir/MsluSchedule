package by.ntnk.msluschedule.di

import javax.inject.Qualifier

object Qualifiers {
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class CacheDirectory

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class OriginalNetworkApi

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class MyUniversityNetworkApi
}
