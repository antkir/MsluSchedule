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
import java.lang.ref.WeakReference
import javax.inject.Inject
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import by.ntnk.msluschedule.databinding.ActivityMainBinding
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

    private lateinit var binding: ActivityMainBinding

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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val toggle = ActionBarDrawerToggle(
                this, binding.drawerLayout, binding.toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        binding.drawerLayout.addDrawerListener(this)

        binding.navView.setNavigationItemSelectedListener(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WindowCompat.setDecorFitsSystemWindows(window, false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                if (nightMode == Configuration.UI_MODE_NIGHT_NO) {
                    val insetsController = WindowInsetsControllerCompat(window, window.decorView)
                    insetsController.isAppearanceLightNavigationBars = true
                }
            }

            ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayout) { view, insets ->
                val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
                val insetsSystemBars = WindowInsetsCompat.Type.systemBars()
                layoutParams.bottomMargin = insets.getInsetsIgnoringVisibility(insetsSystemBars).bottom
                layoutParams.leftMargin = insets.getInsetsIgnoringVisibility(insetsSystemBars).left
                layoutParams.rightMargin = insets.getInsetsIgnoringVisibility(insetsSystemBars).right
                return@setOnApplyWindowInsetsListener insets
            }

            ViewCompat.setOnApplyWindowInsetsListener(binding.navDrawerLayout) { view, insets ->
                val insetsSystemBars = WindowInsetsCompat.Type.systemBars()
                view.setPadding(
                        view.paddingStart,
                        insets.getInsetsIgnoringVisibility(insetsSystemBars).top,
                        view.paddingEnd,
                        view.paddingBottom
                )
                return@setOnApplyWindowInsetsListener insets
            }

            ViewCompat.setOnApplyWindowInsetsListener(binding.appbar) { view, insets ->
                val insetsSystemBars = WindowInsetsCompat.Type.systemBars()
                view.setPadding(
                        view.paddingStart,
                        insets.getInsetsIgnoringVisibility(insetsSystemBars).top,
                        view.paddingEnd,
                        view.paddingBottom
                )
                return@setOnApplyWindowInsetsListener insets
            }

            ViewCompat.setOnApplyWindowInsetsListener(binding.content.constraintLayout) { view, insets ->
                val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
                val toolbarHeight = resources.getDimension(R.dimen.toolbar_height).toInt()
                val insetsSystemBars = WindowInsetsCompat.Type.systemBars()
                layoutParams.topMargin = toolbarHeight + insets.getInsetsIgnoringVisibility(insetsSystemBars).top
                return@setOnApplyWindowInsetsListener insets
            }
        }

        binding.buttonSettings.setOnClickListener {
            SettingsActivity.startActivity(this)
            Handler(Looper.getMainLooper()).postDelayed(NavigationDrawerRunnable(binding.drawerLayout), 500)
        }

        binding.buttonSettings.setOnClickListener {
            SettingsActivity.startActivity(this)
            Handler(Looper.getMainLooper()).postDelayed(NavigationDrawerRunnable(binding.drawerLayout), 500)
        }

        binding.fam.fabBase.setOnClickListener {
            if (AndroidUtils.isNetworkAccessible(applicationContext)) {
                if (!isFamOpen) expandFam() else collapseFam()
            } else {
                AndroidUtils.showSnackbarNetworkInaccessible(it)
            }
        }

        binding.fam.fabGroup.setOnClickListener {
            AddGroupFragment().show(supportFragmentManager, ADD_GROUP_FRAGMENT_TAG)
            if (isFamOpen) collapseFam()
        }

        binding.fam.fabTeacher.setOnClickListener {
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
        binding.drawerLayout.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.drawerLayout.viewTreeObserver?.removeOnGlobalLayoutListener(this)

                val layoutDirection = TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault())
                val isRTL = layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL
                val insetsSystemBars = WindowInsetsCompat.Type.systemBars()
                val insetsTop = ViewCompat.getRootWindowInsets(window.decorView)?.getInsets(insetsSystemBars)?.top ?: 0

                val navIconOffsetX = dipToPixels(28f).toFloat()
                val navigationViewTargetX = if (isRTL) binding.toolbar.right.toFloat() - navIconOffsetX else navIconOffsetX
                val navigationViewTargetY = insetsTop.toFloat() + binding.toolbar.height / 2

                val actIconOffsetX = dipToPixels(22f).toFloat()
                val actionMenuTargetX = if (isRTL) actIconOffsetX else binding.toolbar.right.toFloat() - actIconOffsetX
                val actionMenuTargetY = insetsTop.toFloat() + binding.toolbar.height / 2

                val welcomeTarget = SimpleTarget.Builder(this@MainActivity)
                        .setPoint(binding.content.imageSmile)
                        .setShape(Circle(dipToPixels(60f).toFloat()))
                        .setTitle(getString(R.string.tutorial_welcome_title))
                        .setDescription(getString(R.string.tutorial_welcome_description))
                        .build()
                val addScheduleTarget = SimpleTarget.Builder(this@MainActivity)
                        .setPoint(binding.fam.fabBase)
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
                        .setShape(ActionMenuCircle(dipToPixels(120f).toFloat(), this@MainActivity, binding.toolbar))
                        .setTitle(getString(R.string.tutorial_action_menu_title))
                        .setDescription(getString(R.string.tutorial_action_menu_description))
                        .build()
                val finishTarget = SimpleTarget.Builder(this@MainActivity)
                        .setPoint(binding.content.imageSmile)
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
            binding.content.imageSmile.visibility = View.VISIBLE
            binding.content.textHint.visibility = View.VISIBLE
        } else {
            initMainContent()
        }

        binding.fam.relativeLayout.visibility = if (sharedPreferencesRepository.isMainFabShown()) View.VISIBLE else View.GONE
    }

    private fun isContainerListViewEmpty(): Boolean {
        var size = 0
        for (type in ScheduleType.values()) {
            val subMenuSize = binding.navView.menu
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
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        if (!item.isChecked) {
            val isStudyGroupItem = binding.navView.menu
                    .findItem(getContainerMenuViewId(ScheduleType.STUDYGROUP))
                    .subMenu
                    .findItem(item.itemId) != null
            val type = if (isStudyGroupItem) ScheduleType.STUDYGROUP else ScheduleType.TEACHER
            supportActionBar?.title = item.title
            sharedPreferencesRepository.putSelectedScheduleContainer(item.itemId, item.title.toString(), type)
            isUpdatingWeeksContainer = true
            binding.drawerLayout.closeDrawer(GravityCompat.START)
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
            val weeksContainerFragment = supportFragmentManager.findFragmentById(R.id.content)
            if (weeksContainerFragment != null) {
                viewPagerWeeksContainer = weeksContainerFragment.view?.findViewById(R.id.viewpager_weeks)
            }
            viewPagerWeeksContainer?.alpha = slideOffset
            binding.content.progressbar.visibility = View.VISIBLE
            if (slideOffset < 0.03) {
                isUpdatingWeeksContainer = false
                viewPagerWeeksContainer?.visibility = View.INVISIBLE
                viewPagerWeeksContainer?.alpha = 1f
                swapMainContent()
            }
        }
    }

    private fun swapMainContent() {
        val weeksContainerFragment = supportFragmentManager.findFragmentById(R.id.content)
        if (weeksContainerFragment == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.content, WeeksContainerFragment())
                    .commit()
        } else {
            weeksContainerFragment as WeeksContainerFragment
            weeksContainerFragment.swapTabs()
        }

        binding.content.imageSmile.visibility = View.GONE
        binding.content.textHint.visibility = View.GONE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_ACTIONBAR_TITLE, supportActionBar?.title.toString())
    }

    private fun expandFam() {
        isFamOpen = true

        ViewCompat.animate(binding.fam.fabBase)
                .rotation(135f)
                .setDuration(300)
                .setInterpolator(OvershootInterpolator(2f))
                .start()

        with(binding.fam.fabGroup) {
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
        with(binding.fam.fabTeacher) {
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

        binding.fam.relativeLayout.isFocusable = true
        binding.fam.relativeLayout.isClickable = true
        binding.fam.relativeLayout.setOnClickListener { collapseFam() }
    }

    private fun collapseFam() {
        isFamOpen = false

        binding.fam.fabGroup.animate()
                .setStartDelay(0)
                .scaleY(0f)
                .scaleX(0f)
                .setDuration(100)
                .setInterpolator(FastOutSlowInInterpolator())
                .setListener(object : SimpleAnimatorListener {
                    override fun onAnimationEnd(animation: Animator?) {
                        binding.fam.fabGroup.visibility = View.INVISIBLE
                        binding.fam.fabGroup.animate()?.setListener(null)
                    }
                })
                .start()

        binding.fam.fabTeacher.animate()
                .scaleY(0f)
                .scaleX(0f)
                .setStartDelay(0)
                .setDuration(100)
                .setInterpolator(FastOutSlowInInterpolator())
                .setListener(object : SimpleAnimatorListener {
                    override fun onAnimationEnd(animation: Animator?) {
                        binding.fam.fabTeacher.visibility = View.INVISIBLE
                        binding.fam.fabTeacher.animate()?.setListener(null)
                    }
                })
                .start()

        ViewCompat.animate(binding.fam.fabBase)
                .rotation(0f)
                .setDuration(300)
                .setInterpolator(OvershootInterpolator(2f))
                .start()

        binding.fam.relativeLayout.setOnClickListener(null)
        binding.fam.relativeLayout.isClickable = false
        binding.fam.relativeLayout.isFocusable = false
    }

    override fun onNewStudyGroup(studyGroup: StudyGroup) {
        presenter.addGroup(studyGroup)
    }

    override fun onNewTeacher(teacher: Teacher) {
        presenter.addTeacher(teacher)
    }

    override fun onError(t: Throwable) {
        val snackbar = Snackbar.make(binding.content.constraintLayout, AndroidUtils.getErrorMessageResId(t), Snackbar.LENGTH_LONG)
        snackbar.anchorView = binding.fam.fabBase
        ViewCompat.setOnApplyWindowInsetsListener(snackbar.view) { _, insets -> insets }
        snackbar.show()
    }

    override fun initMainContent() {
        val weeksContainerFragment = supportFragmentManager.findFragmentById(R.id.content)
        if (weeksContainerFragment == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.content, WeeksContainerFragment())
                    .commit()
        }

        binding.content.imageSmile.visibility = View.GONE
        binding.content.textHint.visibility = View.GONE
        binding.content.progressbar.visibility = View.GONE
    }

    override fun addScheduleContainerMenuItem(scheduleContainerInfo: ScheduleContainerInfo) {
        binding.navView.menu
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
            val subMenu = binding.navView.menu
                    .findItem(getContainerMenuViewId(scheduleContainerInfo.type))
                    .subMenu
            for (i in 0 until subMenu.size()) {
                val item = subMenu.getItem(i)
                if (scheduleContainerInfo.id == item.itemId) {
                    binding.navView.setCheckedItem(item.itemId)
                    break
                }
            }
        }
    }

    override fun showNewScheduleContainerLoading(scheduleContainerInfo: ScheduleContainerInfo) {
        supportActionBar?.title = scheduleContainerInfo.value
        removeWeeksContainerFragment()

        binding.content.progressbar.visibility = View.VISIBLE
        binding.content.imageSmile.visibility = View.GONE
        binding.content.textHint.visibility = View.GONE
    }

    override fun showError() {
        supportActionBar?.title = EMPTY_STRING
        removeWeeksContainerFragment()

        binding.content.progressbar.visibility = View.GONE
        binding.content.imageSmile.visibility = View.VISIBLE
        binding.content.textHint.visibility = View.VISIBLE

        val snackbar = Snackbar.make(binding.content.constraintLayout, R.string.error_general, Snackbar.LENGTH_LONG)
        snackbar.anchorView = binding.fam.fabBase
        ViewCompat.setOnApplyWindowInsetsListener(snackbar.view) { _, insets -> insets }
        snackbar.show()
    }

    override fun onScheduleContainerDeleted(info: ScheduleContainerInfo) {
        supportActionBar?.title = EMPTY_STRING
        removeWeeksContainerFragment()

        binding.content.imageSmile.visibility = View.VISIBLE
        binding.content.textHint.visibility = View.VISIBLE

        binding.navView.menu
                .findItem(getContainerMenuViewId(info.type!!))
                .subMenu
                .removeItem(info.id)
    }

    private fun removeWeeksContainerFragment() {
        val fragment = supportFragmentManager.findFragmentById(R.id.content)
        if (fragment != null) {
            supportFragmentManager
                    .beginTransaction()
                    .remove(fragment)
                    .commit()
        }
    }
}
