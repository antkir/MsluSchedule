package by.ntnk.msluschedule.di.modules

import android.content.SharedPreferences
import android.support.v7.preference.PreferenceManager
import by.ntnk.msluschedule.MsluScheduleApp
import by.ntnk.msluschedule.di.PerApp
import dagger.Module
import dagger.Provides

@Module
class UtilsModule {
    @Provides
    @PerApp
    fun provideSharedPreferences(application: MsluScheduleApp): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(application.applicationContext)
    }
}
