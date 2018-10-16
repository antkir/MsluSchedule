package by.ntnk.msluschedule.di.modules

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import by.ntnk.msluschedule.di.PerApp
import dagger.Module
import dagger.Provides

@Module
class UtilsModule {
    @Provides
    @PerApp
    fun provideSharedPreferences(applicationContext: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }
}
