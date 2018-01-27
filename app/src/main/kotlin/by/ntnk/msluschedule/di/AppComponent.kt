package by.ntnk.msluschedule.di

import by.ntnk.msluschedule.MsluScheduleApp
import by.ntnk.msluschedule.di.modules.PresenterManagerModule
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule

@PerApp
@Component(modules = [AndroidSupportInjectionModule::class, PresenterManagerModule::class])
interface AppComponent : AndroidInjector<MsluScheduleApp> {
    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<MsluScheduleApp>()
}