package by.ntnk.msluschedule.di.modules

import android.content.Context
import by.ntnk.msluschedule.MsluScheduleApp
import dagger.Binds
import dagger.Module

@Module
abstract class ContextModule {
    @Suppress("UNUSED")
    @Binds
    abstract fun provideApplicationContext(application: MsluScheduleApp): Context
}
