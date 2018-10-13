package by.ntnk.msluschedule.ui.main

import android.animation.Animator
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.view.ViewCompat
import android.support.v4.view.ViewPager
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.app.ActionBarDrawerToggle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.OvershootInterpolator
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.ScheduleContainerInfo
import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.mvp.views.MvpActivity
import by.ntnk.msluschedule.ui.addgroup.AddGroupFragment
import by.ntnk.msluschedule.ui.addteacher.AddTeacherFragment
import by.ntnk.msluschedule.ui.settings.SettingsActivity
import by.ntnk.msluschedule.ui.weekscontainer.WeeksContainerFragment
import by.ntnk.msluschedule.utils.*
import dagger.Lazy
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.fam_main.*
import javax.inject.Inject

private const val ADD_GROUP_FRAGMENT_TAG = "AddGroupFragment"
private const val ADD_TEACHER_FRAGMENT_TAG = "AddTeacherFragment"
private const val ARG_ACTIONBAR_TITLE = "ActionBarTitle"

class MainActivity : MvpActivity<MainPresenter, MainView>(), MainView,
        NavigationView.OnNavigationItemSelectedListener,
        SimpleDrawerListener,
        HasSupportFragmentInjector,
        AddGroupFragment.DialogListener,
        AddTeacherFragment.DialogListener,
        WeeksContainerFragment.OnScheduleContainerDeletedListener {
    private var isFamOpen = false
    private var isUpdatingWeeksContainer = false
    private lateinit var onThemeChangedDisposable: Disposable

    override val view: MainView
        get() = this

    @Inject
    lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    @Inject
    lateinit var injectedPresenter: Lazy<MainPresenter>

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentDispatchingAndroidInjector

    override fun onCreatePresenter(): MainPresenter = injectedPresenter.get()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        drawer_layout.addDrawerListener(this)

        nav_view.setNavigationItemSelectedListener(this)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            nav_layout_main.setOnApplyWindowInsetsListener { v, insets ->
                v!!.setPaddingRelative(v.paddingStart, insets!!.stableInsetTop, v.paddingEnd, v.paddingBottom)
                insets
            }
        }

        button_settings_main.setOnClickListener { SettingsActivity.startActivity(this) }

        supportActionBar?.title = savedInstanceState?.getString(ARG_ACTIONBAR_TITLE)

        onThemeChangedDisposable = onThemeChanged
                .filter { it }
                .subscribe { recreate() }
    }

    override fun onStart() {
        super.onStart()
        if (isContainerListViewEmpty()) {
            presenter.initContainerListView()
        }

        if (presenter.isSelectedContainerNull()) {
            image_main_smileyface.visibility = View.VISIBLE
            text_main_advice.visibility = View.VISIBLE
        } else {
            initMainContent()
        }
    }

    override fun onStop() {
        super.onStop()
        presenter.clearDisposables()
    }

    override fun onDestroy() {
        super.onDestroy()
        onThemeChangedDisposable.dispose()
    }

    private fun isContainerListViewEmpty(): Boolean {
        var size = 0
        for (type in ScheduleType.values()) {
            val subMenuSize = nav_view.menu
                    .findItem(getContainerMenuViewId(ScheduleType.STUDYGROUP))
                    .subMenu
                    .size()
            size += subMenuSize
        }
        return size == 0
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        if (!item.isChecked) {
            val isStudyGroupItem = nav_view.menu
                    .findItem(getContainerMenuViewId(ScheduleType.STUDYGROUP))
                    .subMenu
                    .findItem(item.itemId) != null
            val type = if (isStudyGroupItem) ScheduleType.STUDYGROUP else ScheduleType.TEACHER
            supportActionBar?.title = item.title
            presenter.setSelectedSheduleContainer(item.itemId, item.title.toString(), type)
            isUpdatingWeeksContainer = true
            drawer_layout.closeDrawer(GravityCompat.START)
        }
        return true
    }

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
        if (isUpdatingWeeksContainer) {
            var viewPagerWeeksContainer: ViewPager? = null
            val weeksContainerFragment = supportFragmentManager.findFragmentById(R.id.framelayout_main)
            if (weeksContainerFragment != null) {
                viewPagerWeeksContainer = weeksContainerFragment.view?.findViewById(R.id.viewpager_weekscontainer)
            }
            viewPagerWeeksContainer?.alpha = slideOffset
            progressbar_main.visibility = View.VISIBLE
            if (slideOffset < 0.03) {
                isUpdatingWeeksContainer = false
                viewPagerWeeksContainer?.visibility = View.INVISIBLE
                viewPagerWeeksContainer?.alpha = 1f
                swapMainContent()
            }
        }
    }

    private fun swapMainContent() {
        val weeksContainerFragment = supportFragmentManager.findFragmentById(R.id.framelayout_main)
        if (weeksContainerFragment == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.framelayout_main, WeeksContainerFragment())
                    .commit()
        } else {
            weeksContainerFragment as WeeksContainerFragment
            weeksContainerFragment.swapTabs()
        }

        image_main_smileyface.visibility = View.GONE
        text_main_advice.visibility = View.GONE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(ARG_ACTIONBAR_TITLE, supportActionBar?.title.toString())
        super.onSaveInstanceState(outState)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onBaseFabClick(view: View) {
        if (isNetworkAccessible(applicationContext)) {
            toggleFloatingActionMenu(!isFamOpen)
        } else {
            showSnackbarNetworkInaccessible(framelayout_main)
        }
    }

    private fun toggleFloatingActionMenu(enabled: Boolean) {
        if (enabled) expandFam() else collapseFam()
    }

    private fun expandFam() {
        isFamOpen = true

        ViewCompat.animate(basefab_main)
                .rotation(135f)
                .setDuration(300)
                .setInterpolator(OvershootInterpolator(2f))
                .start()

        with(groupfab_main) {
            visibility = View.VISIBLE
            scaleX = 0f
            scaleY = 0f
            translationY = height / 4f
            animate()
                    .setStartDelay(75)
                    .scaleY(1f)
                    .scaleX(1f)
                    .translationY(0f)
                    .setDuration(200)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .start()
        }
        with(teacherfab_main) {
            visibility = View.VISIBLE
            scaleX = 0f
            scaleY = 0f
            translationY = height / 4f
            animate()
                    .setStartDelay(0)
                    .scaleY(1f)
                    .scaleX(1f)
                    .translationY(0f)
                    .setDuration(200)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .start()
        }

        fam_layout_main.isFocusable = true
        fam_layout_main.isClickable = true
        fam_layout_main.setOnClickListener { collapseFam() }
    }

    private fun collapseFam() {
        isFamOpen = false

        groupfab_main.animate()
                .setStartDelay(0)
                .scaleY(0f)
                .scaleX(0f)
                .setDuration(100)
                .setInterpolator(FastOutSlowInInterpolator())
                .setListener(object : SimpleAnimatorListener {
                    override fun onAnimationEnd(animation: Animator?) {
                        groupfab_main?.visibility = View.INVISIBLE
                        groupfab_main?.animate()?.setListener(null)
                    }
                })
                .start()

        teacherfab_main.animate()
                .scaleY(0f)
                .scaleX(0f)
                .setStartDelay(0)
                .setDuration(100)
                .setInterpolator(FastOutSlowInInterpolator())
                .setListener(object : SimpleAnimatorListener {
                    override fun onAnimationEnd(animation: Animator?) {
                        teacherfab_main?.visibility = View.INVISIBLE
                        teacherfab_main?.animate()?.setListener(null)
                    }
                })
                .start()

        ViewCompat.animate(basefab_main)
                .rotation(0f)
                .setDuration(300)
                .setInterpolator(OvershootInterpolator(2f))
                .start()

        fam_layout_main.setOnClickListener(null)
        fam_layout_main.isClickable = false
        fam_layout_main.isFocusable = false
    }

    @Suppress("UNUSED_PARAMETER")
    fun onGroupFabClick(view: View) {
        AddGroupFragment().show(supportFragmentManager, ADD_GROUP_FRAGMENT_TAG)
        if (isFamOpen) collapseFam()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onTeacherFabClick(view: View) {
        AddTeacherFragment().show(supportFragmentManager, ADD_TEACHER_FRAGMENT_TAG)
        if (isFamOpen) collapseFam()
    }

    override fun onNewStudyGroup(studyGroup: StudyGroup) {
        presenter.addGroup(studyGroup)
    }

    override fun onNewTeacher(teacher: Teacher) {
        presenter.addTeacher(teacher)
    }

    override fun onError(t: Throwable) {
        Snackbar.make(framelayout_main, getErrorMessageResId(t), Snackbar.LENGTH_LONG).show()
    }

    override fun initMainContent() {
        val weeksContainerFragment = supportFragmentManager.findFragmentById(R.id.framelayout_main)
        if (weeksContainerFragment == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.framelayout_main, WeeksContainerFragment())
                    .commit()
        }

        image_main_smileyface.visibility = View.GONE
        text_main_advice.visibility = View.GONE
        progressbar_main.visibility = View.GONE
    }

    override fun addScheduleContainerMenuItem(scheduleContainerInfo: ScheduleContainerInfo) {
        nav_view.menu
                .findItem(getContainerMenuViewId(scheduleContainerInfo.type!!))
                .subMenu
                .add(Menu.NONE, scheduleContainerInfo.id, Menu.NONE, scheduleContainerInfo.value)
                .isCheckable = true
    }

    private fun getContainerMenuViewId(scheduleType: ScheduleType): Int {
        return when (scheduleType) {
            ScheduleType.STUDYGROUP -> R.id.group_submenu
            else -> R.id.teacher_submenu
        }
    }

    override fun checkScheduleContainerMenuItem(scheduleContainerInfo: ScheduleContainerInfo) {
        supportActionBar?.title = scheduleContainerInfo.value
        if (scheduleContainerInfo.type != null) {
            val subMenu = nav_view.menu
                    .findItem(getContainerMenuViewId(scheduleContainerInfo.type))
                    .subMenu
            for (i in 0 until subMenu.size()) {
                val item = subMenu.getItem(i)
                if (scheduleContainerInfo.id == item.itemId) {
                    nav_view.setCheckedItem(item.itemId)
                    break
                }
            }
        }
    }

    override fun showNewScheduleContainerLoading(scheduleContainerInfo: ScheduleContainerInfo) {
        supportActionBar?.title = scheduleContainerInfo.value
        removeWeeksContainerFragment()

        progressbar_main.visibility = View.VISIBLE
        image_main_smileyface.visibility = View.GONE
        text_main_advice.visibility = View.GONE
    }

    override fun showError() {
        supportActionBar?.title = EMPTY_STRING
        removeWeeksContainerFragment()

        progressbar_main.visibility = View.GONE
        image_main_smileyface.visibility = View.VISIBLE
        text_main_advice.visibility = View.VISIBLE

        Snackbar.make(framelayout_main, R.string.error_general, Snackbar.LENGTH_LONG).show()
    }

    override fun onScheduleContainerDeleted(info: ScheduleContainerInfo) {
        supportActionBar?.title = EMPTY_STRING
        removeWeeksContainerFragment()

        image_main_smileyface.visibility = View.VISIBLE
        text_main_advice.visibility = View.VISIBLE

        nav_view.menu
                .findItem(getContainerMenuViewId(info.type!!))
                .subMenu
                .removeItem(info.id)
    }

    private fun removeWeeksContainerFragment() {
        val fragment = supportFragmentManager.findFragmentById(R.id.framelayout_main)
        if (fragment != null) {
            supportFragmentManager
                    .beginTransaction()
                    .remove(fragment)
                    .commit()
        }
    }
}
