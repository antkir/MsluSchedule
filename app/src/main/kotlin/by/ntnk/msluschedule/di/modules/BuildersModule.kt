package by.ntnk.msluschedule.di.modules

import by.ntnk.msluschedule.ui.addgroup.AddGroupFragment
import by.ntnk.msluschedule.ui.addteacher.AddTeacherFragment
import by.ntnk.msluschedule.ui.main.MainActivity
import by.ntnk.msluschedule.ui.week.WeekFragment
import by.ntnk.msluschedule.ui.weekscontainer.WeeksContainerFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class BuildersModule {
    @ContributesAndroidInjector
    abstract fun bindMainActivity(): MainActivity

    @ContributesAndroidInjector
    abstract fun bindAddGroupFragment(): AddGroupFragment

    @ContributesAndroidInjector
    abstract fun bindAddTeacherFragment(): AddTeacherFragment

    @ContributesAndroidInjector
    abstract fun bindWeeksContainerFragment(): WeeksContainerFragment

    @ContributesAndroidInjector
    abstract fun bindWeekFragment(): WeekFragment
}
