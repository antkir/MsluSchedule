package by.ntnk.msluschedule.di.modules

import by.ntnk.msluschedule.di.PerApp
import by.ntnk.msluschedule.utils.CurrentDate
import dagger.Module
import dagger.Provides

@Module
class UtilsModule {
    @Provides
    @PerApp
    fun provideCurrentDate(): CurrentDate {
        return CurrentDate()
    }
}
