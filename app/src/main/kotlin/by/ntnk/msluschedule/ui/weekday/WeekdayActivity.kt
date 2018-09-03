package by.ntnk.msluschedule.ui.weekday

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.mvp.views.MvpActivity
import dagger.Lazy
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import timber.log.Timber
import javax.inject.Inject

class WeekdayActivity : MvpActivity<WeekdayPresenter, WeekdayView>(),
        WeekdayView,
        HasSupportFragmentInjector {
    private var weekdayId: Int = -1

    override val view: WeekdayView
        get() = this

    @Inject
    lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    @Inject
    lateinit var injectedPresenter: Lazy<WeekdayPresenter>

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentDispatchingAndroidInjector

    override fun onCreatePresenter(): WeekdayPresenter = injectedPresenter.get()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weekday)
        weekdayId = intent?.getIntExtra(ARG_WEEKDAY_ID, -1) ?: -1
        Timber.d("%d", weekdayId)
    }

    companion object {
        private const val ARG_WEEKDAY_ID = "weekdayId"

        fun startActivity(context: Context, weekdayId: Int) {
            val intent = Intent(context.applicationContext, WeekdayActivity::class.java).apply {
                putExtra(ARG_WEEKDAY_ID, weekdayId)
            }
            context.startActivity(intent)
        }
    }
}
