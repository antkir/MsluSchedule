package by.ntnk.msluschedule.di.modules

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import by.ntnk.msluschedule.di.Qualifiers.CacheDirectory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class UtilsModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext applicationContext: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }

    @Provides
    @CacheDirectory
    fun provideCacheFolder(@ApplicationContext applicationContext: Context): File {
        return applicationContext.cacheDir
    }
}
