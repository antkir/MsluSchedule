package by.ntnk.msluschedule.ui.main

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.view.ViewCompat
import android.support.v4.widget.DrawerLayout
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
import by.ntnk.msluschedule.ui.weekscontainer.WeeksContainerFragment
import by.ntnk.msluschedule.utils.EMPTY_STRING
import by.ntnk.msluschedule.utils.ScheduleType
import by.ntnk.msluschedule.utils.isNetworkAccessible
import by.ntnk.msluschedule.utils.showSnackbarNetworkInaccessible
import dagger.Lazy
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.fam_main.*
import kotlinx.android.synthetic.main.fragment_weekscontainer.*
import javax.inject.Inject

private const val ADD_GROUP_FRAGMENT = "AddGroupFragment"
private const val ADD_TEACHER_FRAGMENT = "AddTeacherFragment"

class MainActivity :
        MvpActivity<MainPresenter, MainView>(), MainView,
        NavigationView.OnNavigationItemSelectedListener,
        DrawerLayout.DrawerListener,
        HasSupportFragmentInjector,
        AddGroupFragment.OnPositiveButtonListener,
        AddTeacherFragment.OnPositiveButtonListener,
        WeeksContainerFragment.OnScheduleContainerDeletedListener {
    override val view: MainView
        get() = this

    private var isFamOpen = false
    private var isUpdatingWeeksContainer = false

    @Inject
    lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    @Inject
    lateinit var injectedPresenter: Lazy<MainPresenter>

    override fun supportFragmentInjector(): AndroidInjector<Fragment> {
        return fragmentDispatchingAndroidInjector
    }

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

        presenter.initContainerListView()
        if (savedInstanceState == null && !presenter.isSelectedContainerNull()) {
            initMainContent()
        } else if (presenter.isSelectedContainerNull()) {
            image_main_smileyface.visibility = View.VISIBLE
            text_main_advice.visibility = View.VISIBLE
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.isChecked) return true
        // Handle navigation view item clicks here.
        val isStudyGroupItem = nav_view.menu
                .findItem(getContainerMenuViewId(ScheduleType.STUDYGROUP))
                .subMenu
                .findItem(item.itemId) != null
        val type = if (isStudyGroupItem) ScheduleType.STUDYGROUP else ScheduleType.TEACHER
        supportActionBar?.title = item.title
        presenter.setSelectedSheduleContainer(item.itemId, item.title.toString(), type)
        isUpdatingWeeksContainer = true
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
        if (isUpdatingWeeksContainer) {
            viewpager_weekscontainer?.alpha = slideOffset
            if (slideOffset < 0.03) {
                isUpdatingWeeksContainer = false
                viewpager_weekscontainer?.visibility = View.INVISIBLE
                viewpager_weekscontainer?.alpha = 1f
                initMainContent()
            }
        }
    }

    override fun onDrawerOpened(drawerView: View) = Unit

    override fun onDrawerClosed(drawerView: View) = Unit

    override fun onDrawerStateChanged(newState: Int) = Unit

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

        groupfab_main.visibility = View.VISIBLE
        groupfab_main.y = basefab_main.y
        groupfab_main.animate().translationY(0f).setDuration(200).start()

        teacherfab_main.visibility = View.VISIBLE
        teacherfab_main.y = basefab_main.y
        teacherfab_main.animate().translationY(0f).setDuration(200).start()
    }

    private fun collapseFam() {
        isFamOpen = false

        groupfab_main.animate().y(basefab_main.y).setDuration(200).start()
        teacherfab_main.animate().y(basefab_main.y).setDuration(200).start()

        ViewCompat.animate(basefab_main)
                .rotation(0f)
                .setDuration(300)
                .setInterpolator(OvershootInterpolator(2f))
                .start()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onGroupFabClick(view: View) {
        val addGroupFragment = AddGroupFragment()
        addGroupFragment.isCancelable = false
        addGroupFragment.show(supportFragmentManager, ADD_GROUP_FRAGMENT)
        if (isFamOpen) collapseFam()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onTeacherFabClick(view: View) {
        val addTeacherFragment = AddTeacherFragment()
        addTeacherFragment.isCancelable = false
        addTeacherFragment.show(supportFragmentManager, ADD_TEACHER_FRAGMENT)
        if (isFamOpen) collapseFam()
    }

    override fun onPositiveButtonGroup(studyGroup: StudyGroup) {
        presenter.addGroup(studyGroup)
    }

    override fun onPositiveButtonTeacher(teacher: Teacher) {
        presenter.addTeacher(teacher)
    }

    override fun initMainContent() {
        val weeksContainerFragment = supportFragmentManager.findFragmentById(R.id.framelayout_main)
        if (weeksContainerFragment == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.framelayout_main, WeeksContainerFragment())
                    .commit()
        } else {
            weeksContainerFragment as WeeksContainerFragment
            weeksContainerFragment.swapTabs()
        }

        progressbar_main.visibility = View.INVISIBLE
        image_main_smileyface.visibility = View.INVISIBLE
        text_main_advice.visibility = View.INVISIBLE
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
                    nav_view!!.setCheckedItem(item.itemId)
                    break
                }
            }
        }
    }

    override fun showNewScheduleContainerLoading(scheduleContainerInfo: ScheduleContainerInfo) {
        supportActionBar?.title = scheduleContainerInfo.value
        removeWeeksContainerFragment()

        progressbar_main.visibility = View.VISIBLE
        image_main_smileyface.visibility = View.INVISIBLE
        text_main_advice.visibility = View.INVISIBLE
    }

    override fun showError() {
        supportActionBar?.title = EMPTY_STRING
        removeWeeksContainerFragment()

        progressbar_main.visibility = View.INVISIBLE
        image_main_smileyface.visibility = View.VISIBLE
        text_main_advice.visibility = View.VISIBLE

        Snackbar.make(framelayout_main, R.string.error_init_schedule, Snackbar.LENGTH_LONG).show()
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
