package by.ntnk.msluschedule.ui.main

import android.animation.Animator
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat
import androidx.core.text.TextUtilsCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.viewpager.widget.ViewPager
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.ScheduleContainerInfo
import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.mvp.views.MvpActivity
import by.ntnk.msluschedule.ui.addgroup.AddGroupFragment
import by.ntnk.msluschedule.ui.addteacher.AddTeacherFragment
import by.ntnk.msluschedule.ui.customviews.ActionMenuCircle
import by.ntnk.msluschedule.ui.settings.SettingsActivity
import by.ntnk.msluschedule.ui.weekscontainer.WeeksContainerFragment
import by.ntnk.msluschedule.utils.*
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.takusemba.spotlight.OnSpotlightStateChangedListener
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.shape.Circle
import com.takusemba.spotlight.target.SimpleTarget
import dagger.Lazy
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.fam_main.*
import java.lang.ref.WeakReference
import javax.inject.Inject
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import java.util.Locale

private const val ADD_GROUP_FRAGMENT_TAG = "AddGroupFragment"
private const val ADD_TEACHER_FRAGMENT_TAG = "AddTeacherFragment"
private const val ARG_ACTIONBAR_TITLE = "ActionBarTitle"

class MainActivity : MvpActivity<MainPresenter, MainView>(), MainView,
        NavigationView.OnNavigationItemSelectedListener,
        SimpleDrawerListener,
        HasAndroidInjector,
        AddGroupFragment.DialogListener,
        AddTeacherFragment.DialogListener,
        WeeksContainerFragment.OnScheduleContainerDeletedListener {
    private var isFamOpen = false
    private var isUpdatingWeeksContainer = false
    private lateinit var onThemeChangedDisposable: Disposable

    override val view: MainView
        get() = this

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var injectedPresenter: Lazy<MainPresenter>

    @Inject
    lateinit var sharedPreferencesRepository: SharedPreferencesRepository

    override fun androidInjector(): AndroidInjector<Any> = dispatchingAndroidInjector

    override fun onCreatePresenter(): MainPresenter = injectedPresenter.get()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        setTheme(R.style.MsluTheme_NoActionBarTransparentStatusBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar_main)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar_main, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        drawer_layout.addDrawerListener(this)

        nav_view.setNavigationItemSelectedListener(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WindowCompat.setDecorFitsSystemWindows(window, false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                if (nightMode == Configuration.UI_MODE_NIGHT_NO) {
                    val insetsController = WindowInsetsControllerCompat(window, window.decorView)
                    insetsController.isAppearanceLightNavigationBars = true
                }
            }

            ViewCompat.setOnApplyWindowInsetsListener(drawer_layout) { view, insets ->
                val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
                val insetsSystemBars = WindowInsetsCompat.Type.systemBars()
                layoutParams.bottomMargin = insets.getInsetsIgnoringVisibility(insetsSystemBars).bottom
                layoutParams.leftMargin = insets.getInsetsIgnoringVisibility(insetsSystemBars).left
                layoutParams.rightMargin = insets.getInsetsIgnoringVisibility(insetsSystemBars).right
                return@setOnApplyWindowInsetsListener insets
            }

            ViewCompat.setOnApplyWindowInsetsListener(nav_layout_main) { view, insets ->
                val insetsSystemBars = WindowInsetsCompat.Type.systemBars()
                view.setPadding(
                        view.paddingStart,
                        insets.getInsetsIgnoringVisibility(insetsSystemBars).top,
                        view.paddingEnd,
                        view.paddingBottom
                )
                return@setOnApplyWindowInsetsListener insets
            }

            ViewCompat.setOnApplyWindowInsetsListener(appbar_main) { view, insets ->
                val insetsSystemBars = WindowInsetsCompat.Type.systemBars()
                view.setPadding(
                        view.paddingStart,
                        insets.getInsetsIgnoringVisibility(insetsSystemBars).top,
                        view.paddingEnd,
                        view.paddingBottom
                )
                return@setOnApplyWindowInsetsListener insets
            }

            ViewCompat.setOnApplyWindowInsetsListener(framelayout_main) { view, insets ->
                val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
                val toolbarHeight = resources.getDimension(R.dimen.toolbar_height).toInt()
                val insetsSystemBars = WindowInsetsCompat.Type.systemBars()
                layoutParams.topMargin = toolbarHeight + insets.getInsetsIgnoringVisibility(insetsSystemBars).top
                return@setOnApplyWindowInsetsListener insets
            }
        }

        button_settings_main.setOnClickListener {
            SettingsActivity.startActivity(this)
            Handler(Looper.getMainLooper()).postDelayed(NavigationDrawerRunnable(drawer_layout), 500)
        }

        button_settings_main.setOnClickListener {
            SettingsActivity.startActivity(this)
            Handler(Looper.getMainLooper()).postDelayed(NavigationDrawerRunnable(drawer_layout), 500)
        }

        basefab_main.setOnClickListener {
            if (AndroidUtils.isNetworkAccessible(applicationContext)) {
                if (!isFamOpen) expandFam() else collapseFam()
            } else {
                AndroidUtils.showSnackbarNetworkInaccessible(framelayout_main)
            }
        }

        groupfab_main.setOnClickListener {
            AddGroupFragment().show(supportFragmentManager, ADD_GROUP_FRAGMENT_TAG)
            if (isFamOpen) collapseFam()
        }

        teacherfab_main.setOnClickListener {
            AddTeacherFragment().show(supportFragmentManager, ADD_TEACHER_FRAGMENT_TAG)
            if (isFamOpen) collapseFam()
        }

        supportActionBar?.title = savedInstanceState?.getString(ARG_ACTIONBAR_TITLE)

        onThemeChangedDisposable = onThemeChanged
                .filter { it }
                .subscribe { recreate() }

        if (sharedPreferencesRepository.isFirstAppLaunch) {
            showTutorial()
        }
    }

    private class NavigationDrawerRunnable(context: DrawerLayout?) : Runnable {
        private val drawerLayoutRef: WeakReference<DrawerLayout?> = WeakReference(context)

        override fun run() {
            drawerLayoutRef.get()?.closeDrawer(GravityCompat.START)
        }
    }

    private fun showTutorial() {
        drawer_layout.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                drawer_layout?.viewTreeObserver?.removeOnGlobalLayoutListener(this)

                val layoutDirection = TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault())
                val isRTL = layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL
                val insetsSystemBars = WindowInsetsCompat.Type.systemBars()
                val insetsTop = ViewCompat.getRootWindowInsets(window.decorView)?.getInsets(insetsSystemBars)?.top ?: 0

                val navIconOffsetX = dipToPixels(28f).toFloat()
                val navigationViewTargetX = if (isRTL) toolbar_main.right.toFloat() - navIconOffsetX else navIconOffsetX
                val navigationViewTargetY = insetsTop.toFloat() + toolbar_main.height / 2

                val actIconOffsetX = dipToPixels(22f).toFloat()
                val actionMenuTargetX = if (isRTL) actIconOffsetX else toolbar_main.right.toFloat() - actIconOffsetX
                val actionMenuTargetY = insetsTop.toFloat() + toolbar_main.height / 2

                val welcomeTarget = SimpleTarget.Builder(this@MainActivity)
                        .setPoint(image_main_smileyface)
                        .setShape(Circle(dipToPixels(60f).toFloat()))
                        .setTitle(getString(R.string.tutorial_welcome_title))
                        .setDescription(getString(R.string.tutorial_welcome_description))
                        .build()
                val addScheduleTarget = SimpleTarget.Builder(this@MainActivity)
                        .setPoint(basefab_main)
                        .setShape(Circle(dipToPixels(60f).toFloat()))
                        .setTitle(getString(R.string.tutorial_add_schedule_title))
                        .setDescription(getString(R.string.tutorial_add_schedule_description))
                        .build()
                val navigationViewTarget = SimpleTarget.Builder(this@MainActivity)
                        .setPoint(navigationViewTargetX, navigationViewTargetY)
                        .setShape(Circle(dipToPixels(120f).toFloat()))
                        .setTitle(getString(R.string.tutorial_navigation_view_title))
                        .setDescription(getString(R.string.tutorial_navigation_view_description))
                        .build()
                val actionMenuTarget = SimpleTarget.Builder(this@MainActivity)
                        .setPoint(actionMenuTargetX, actionMenuTargetY)
                        .setShape(ActionMenuCircle(dipToPixels(120f).toFloat(), this@MainActivity, toolbar_main))
                        .setTitle(getString(R.string.tutorial_action_menu_title))
                        .setDescription(getString(R.string.tutorial_action_menu_description))
                        .build()
                val finishTarget = SimpleTarget.Builder(this@MainActivity)
                        .setPoint(image_main_smileyface)
                        .setShape(Circle(dipToPixels(60f).toFloat()))
                        .setTitle(getString(R.string.tutorial_finish_title))
                        .setDescription(getString(R.string.tutorial_finish_description))
                        .build()

                Spotlight.with(this@MainActivity)
                        .setTargets(welcomeTarget, addScheduleTarget,
                                    navigationViewTarget, actionMenuTarget, finishTarget)
                        .setOnSpotlightStateListener(object : OnSpotlightStateChangedListener {
                            override fun onStarted() = Unit
                            override fun onEnded() {
                                sharedPreferencesRepository.isFirstAppLaunch = false
                            }
                        })
                        .start()
            }
        })
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

        fam_layout_main.visibility = if (sharedPreferencesRepository.isMainFabShown()) View.VISIBLE else View.GONE
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

    override fun onStop() {
        super.onStop()
        presenter.clearDisposables()
    }

    override fun onDestroy() {
        super.onDestroy()
        onThemeChangedDisposable.dispose()
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
            sharedPreferencesRepository.putSelectedScheduleContainer(item.itemId, item.title.toString(), type)
            isUpdatingWeeksContainer = true
            drawer_layout.closeDrawer(GravityCompat.START)
        }
        return true
    }

    override fun onDrawerOpened(drawerView: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (nightMode == Configuration.UI_MODE_NIGHT_NO) {
                window.statusBarColor = ContextCompat.getColor(this, R.color.statusbar_nav_drawer)
            }
        }
    }

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (nightMode == Configuration.UI_MODE_NIGHT_NO) {
                val color = ContextCompat.getColor(this, R.color.statusbar_nav_drawer)
                window.statusBarColor = Color.argb(
                        (slideOffset * Color.alpha(color)).toInt(),
                        Color.red(color), Color.green(color), Color.blue(color))
            }
        }

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
        super.onSaveInstanceState(outState)
        outState.putString(ARG_ACTIONBAR_TITLE, supportActionBar?.title.toString())
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

    override fun onNewStudyGroup(studyGroup: StudyGroup) {
        presenter.addGroup(studyGroup)
    }

    override fun onNewTeacher(teacher: Teacher) {
        presenter.addTeacher(teacher)
    }

    override fun onError(t: Throwable) {
        val snackbar = Snackbar.make(framelayout_main, AndroidUtils.getErrorMessageResId(t), Snackbar.LENGTH_LONG)
        ViewCompat.setOnApplyWindowInsetsListener(snackbar.view) { _, insets -> insets }
        snackbar.show()
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

        val snackbar = Snackbar.make(framelayout_main, R.string.error_general, Snackbar.LENGTH_LONG)
        ViewCompat.setOnApplyWindowInsetsListener(snackbar.view) { _, insets -> insets }
        snackbar.show()
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
