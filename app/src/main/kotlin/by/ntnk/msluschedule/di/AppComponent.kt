package by.ntnk.msluschedule.di

import by.ntnk.msluschedule.MsluScheduleApp
import by.ntnk.msluschedule.di.modules.*
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector

@PerApp
@Component(modules = [
    AndroidInjectionModule::class,
    ContextModule::class,
    ContributorsModule::class,
    DatabaseModule::class,
    NetworkModule::class,
    UtilsModule::class
])
interface AppComponent : AndroidInjector<MsluScheduleApp> {
    @Component.Factory
    interface Factory : AndroidInjector.Factory<MsluScheduleApp>
}
