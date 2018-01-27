package by.ntnk.msluschedule.di.modules

import by.ntnk.msluschedule.di.PerApp
import by.ntnk.msluschedule.mvp.PresenterManager
import dagger.Module
import dagger.Provides

@Module
class PresenterManagerModule {
    @Provides
    @PerApp
    fun providePresenterManager(): PresenterManager {
        return PresenterManager()
    }
}
