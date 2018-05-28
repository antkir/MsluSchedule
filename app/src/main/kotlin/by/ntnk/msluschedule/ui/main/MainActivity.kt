package by.ntnk.msluschedule.ui.main

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.view.Menu
import android.view.MenuItem
import android.view.View
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

        presenter.initContainerListView()
        if (savedInstanceState == null && !presenter.isSelectedContainerNull()) initMainContent()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.isChecked) return true
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_settings -> {
            }
            else -> {
                val isStudyGroupItem = nav_view.menu
                        .findItem(getContainerMenuViewId(ScheduleType.STUDYGROUP))
                        .subMenu
                        .findItem(item.itemId) != null
                val type = if (isStudyGroupItem) ScheduleType.STUDYGROUP else ScheduleType.TEACHER
                supportActionBar?.title = item.title
                presenter.setSelectedSheduleContainer(item.itemId, item.title.toString(), type)
                isUpdatingWeeksContainer = true
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onDrawerStateChanged(newState: Int) {
    }

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
        if (isUpdatingWeeksContainer) {
            viewpager_weekscontainer.alpha = slideOffset
            if (slideOffset < 0.03) {
                isUpdatingWeeksContainer = false
                viewpager_weekscontainer.visibility = View.INVISIBLE
                viewpager_weekscontainer.alpha = 1f
                swapWeeksContainerData()
            }
        }
    }

    private fun swapWeeksContainerData() {
        val weeksContainerFragment = supportFragmentManager.findFragmentById(R.id.framelayout_main)
        weeksContainerFragment ?: return
        weeksContainerFragment as WeeksContainerFragment
        weeksContainerFragment.swapTabs()
    }

    override fun onDrawerClosed(drawerView: View) {
    }

    override fun onDrawerOpened(drawerView: View) {
    }

    @Suppress("UNUSED_PARAMETER")
    fun onBaseFabClick(view: View) {
        if (isNetworkAccessible(applicationContext)) {
            toggleFloatingActionMenu(!isFamOpen)
        } else {
            Snackbar.make(
                    framelayout_main, R.string.snackbar_internet_unavailable, Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun toggleFloatingActionMenu(enabled: Boolean) {
        if (enabled) expandFam() else collapseFam()
    }

    private fun expandFam() {
        isFamOpen = true
        groupfab_main.visibility = View.VISIBLE
        teacherfab_main.visibility = View.VISIBLE
    }

    private fun collapseFam() {
        isFamOpen = false
        groupfab_main.visibility = View.INVISIBLE
        teacherfab_main.visibility = View.INVISIBLE
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
        supportFragmentManager.beginTransaction()
            .replace(R.id.framelayout_main, WeeksContainerFragment())
            .commit()
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

    override fun onScheduleContainerDeleted(info: ScheduleContainerInfo) {
        supportActionBar?.title = EMPTY_STRING
        val fragmentManager = supportFragmentManager
        val fragment = fragmentManager.findFragmentById(R.id.framelayout_main)
        if (fragment != null) {
            fragmentManager
                    .beginTransaction()
                    .remove(fragment)
                    .commit()
        }
        nav_view.menu
                .findItem(getContainerMenuViewId(info.type!!))
                .subMenu
                .removeItem(info.id)
    }
}
