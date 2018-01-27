package by.ntnk.msluschedule.di

import by.ntnk.msluschedule.MsluScheduleApp
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule

@PerApp
@Component(modules = [AndroidSupportInjectionModule::class])
interface AppComponent : AndroidInjector<MsluScheduleApp> {
    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<MsluScheduleApp>()
}