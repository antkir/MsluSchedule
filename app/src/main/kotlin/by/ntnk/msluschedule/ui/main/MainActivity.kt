package by.ntnk.msluschedule.ui.main

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
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
import by.ntnk.msluschedule.utils.ScheduleType
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fam_main.*
import timber.log.Timber
import javax.inject.Inject

private const val ADD_GROUP_FRAGMENT = "AddGroupFragment"
private const val ADD_TEACHER_FRAGMENT = "AddTeacherFragment"

class MainActivity :
        MvpActivity<MainPresenter, MainView>(), MainView,
        NavigationView.OnNavigationItemSelectedListener,
        HasSupportFragmentInjector,
        AddContainerDialogListener {
    override val view: MainView
        get() = this

    private var isFamOpen = false

    @Inject
    lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    @Inject
    lateinit var injectedPresenter: MainPresenter

    override fun supportFragmentInjector(): AndroidInjector<Fragment> {
        return fragmentDispatchingAndroidInjector
    }

    override fun onCreatePresenter(): MainPresenter {
        return injectedPresenter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        presenter.initContainerListView()
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
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.isChecked) return true

        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_share -> {
            }
            R.id.nav_send -> {
            }
            else -> {
                val isStudyGroupItem = nav_view.menu
                        .findItem(getContainerMenuViewId(ScheduleType.STUDYGROUP))
                        .subMenu
                        .findItem(item.itemId) != null
                val type = if (isStudyGroupItem) ScheduleType.STUDYGROUP else ScheduleType.TEACHER
                supportActionBar?.title = item.title
                presenter.setSelectedSheduleContainer(item.itemId, item.title.toString(), type)
                initMainContent()
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    @Suppress("UNUSED_PARAMETER")
    fun onBaseFabClick(view: View) {
        toggleFloatingActionMenu(!isFamOpen)
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
        Timber.e("Not implemented")
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
}
