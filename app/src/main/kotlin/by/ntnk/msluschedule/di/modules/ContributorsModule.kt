package by.ntnk.msluschedule.di.modules

import android.annotation.SuppressLint
import by.ntnk.msluschedule.ui.addgroup.AddGroupFragment
import by.ntnk.msluschedule.ui.addteacher.AddTeacherFragment
import by.ntnk.msluschedule.ui.lessoninfo.LessonInfoActivity
import by.ntnk.msluschedule.ui.main.MainActivity
import by.ntnk.msluschedule.ui.settings.SettingsActivity
import by.ntnk.msluschedule.ui.warningdialog.WarningDialogFragment
import by.ntnk.msluschedule.ui.week.WeekFragment
import by.ntnk.msluschedule.ui.weekday.WeekdayActivity
import by.ntnk.msluschedule.ui.weekscontainer.WeeksContainerFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
@Suppress("UNUSED")
@SuppressLint
abstract class ContributorsModule {
    @ContributesAndroidInjector
    abstract fun contributeMainActivity(): MainActivity

    @ContributesAndroidInjector
    abstract fun contributeAddGroupFragment(): AddGroupFragment

    @ContributesAndroidInjector
    abstract fun contributeAddTeacherFragment(): AddTeacherFragment

    @ContributesAndroidInjector
    abstract fun contributeWeeksContainerFragment(): WeeksContainerFragment

    @ContributesAndroidInjector
    abstract fun contributeWeekFragment(): WeekFragment

    @ContributesAndroidInjector
    abstract fun contributeWeekdayActivity(): WeekdayActivity

    @ContributesAndroidInjector
    abstract fun contributeLessonInfoActivity(): LessonInfoActivity

    @ContributesAndroidInjector
    abstract fun contributeSettingsActivity(): SettingsActivity

    @ContributesAndroidInjector
    abstract fun contributeSettingsFragment(): SettingsActivity.SettingsFragment

    @ContributesAndroidInjector
    abstract fun contributeWarningDialogFragment(): WarningDialogFragment
}
