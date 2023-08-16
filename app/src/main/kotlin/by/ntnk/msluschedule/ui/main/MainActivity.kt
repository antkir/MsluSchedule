package by.ntnk.msluschedule.ui.main

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PointF
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnPreDraw
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.ScheduleContainer
import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.databinding.ActivityMainBinding
import by.ntnk.msluschedule.mvp.views.MvpActivity
import by.ntnk.msluschedule.ui.addgroup.AddGroupFragment
import by.ntnk.msluschedule.ui.addteacher.AddTeacherFragment
import by.ntnk.msluschedule.ui.main.onboarding.ActionMenuCircle
import by.ntnk.msluschedule.ui.main.onboarding.OverlayLayout
import by.ntnk.msluschedule.ui.main.onboarding.SimpleCircle
import by.ntnk.msluschedule.ui.settings.SettingsActivity
import by.ntnk.msluschedule.ui.weekscontainer.WeeksContainerFragment
import by.ntnk.msluschedule.utils.AndroidUtils
import by.ntnk.msluschedule.utils.DEFAULT
import by.ntnk.msluschedule.utils.ScheduleType
import by.ntnk.msluschedule.utils.SimpleDrawerListener
import by.ntnk.msluschedule.utils.dipToPixels
import by.ntnk.msluschedule.utils.onThemeChanged
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.takusemba.spotlight.OnSpotlightListener
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.Target
import dagger.Lazy
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import javax.inject.Inject

class MainActivity :
    MvpActivity<MainPresenter, MainView>(),
    MainView,
    NavigationView.OnNavigationItemSelectedListener,
    SimpleDrawerListener,
    HasAndroidInjector,
    AddGroupFragment.DialogListener,
    AddTeacherFragment.DialogListener,
    WeeksContainerFragment.OnScheduleContainerDeletedListener {

    private var isFamOpen = false
    private var pendingScheduleContainerId = Int.DEFAULT
    private lateinit var onThemeChangedDisposable: Disposable
    private val disposables: CompositeDisposable = CompositeDisposable()

    private lateinit var binding: ActivityMainBinding

    override val view: MainView
        get() = this

    private val onDrawerOpenBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var injectedPresenter: Lazy<MainPresenter>

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
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
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
                val insetsSystemBars = WindowInsetsCompat.Type.systemBars()
                val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
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
                val insetsSystemBars = WindowInsetsCompat.Type.systemBars()
                val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
                val toolbarHeight = resources.getDimension(R.dimen.toolbar_height).toInt()
                layoutParams.topMargin = toolbarHeight + insets.getInsetsIgnoringVisibility(insetsSystemBars).top
                return@setOnApplyWindowInsetsListener insets
            }
        }

        binding.buttonSettings.setOnClickListener {
            SettingsActivity.startActivity(this)
            Handler(Looper.getMainLooper()).postDelayed({
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }, SETTINGS_CLOSE_NAVIGATION_DRAWER_DELAY)
        }

        binding.fam.fabBase.setOnClickListener {
            if (AndroidUtils.isNetworkAccessible(applicationContext)) {
                if (!isFamOpen) expandFam() else collapseFam()
            } else {
                AndroidUtils.showSnackbarNetworkInaccessible(it)
            }
        }

        binding.fam.fabGroup.setOnClickListener {
            AddGroupFragment().show(supportFragmentManager, AddGroupFragment.FRAGMENT_TAG)
            if (isFamOpen) collapseFam()
        }

        binding.fam.fabTeacher.setOnClickListener {
            AddTeacherFragment().show(supportFragmentManager, AddTeacherFragment.FRAGMENT_TAG)
            if (isFamOpen) collapseFam()
        }

        supportActionBar?.title = savedInstanceState?.getString(ARG_TOOLBAR_TITLE) ?: String.DEFAULT

        pendingScheduleContainerId = savedInstanceState?.getInt(ARG_PENDING_SCHEDULE_CONTAINER_ID) ?: Int.DEFAULT

        onThemeChangedDisposable = onThemeChanged
            .filter { it }
            .subscribe { recreate() }

        if (presenter.isOnboardingNeeded()) {
            binding.root.doOnPreDraw { startOnboarding() }
        }

        onBackPressedDispatcher.addCallback(this, onDrawerOpenBackPressedCallback)
    }

    private fun getViewLocationInWindow(view: View): PointF {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        val x = location[0].toFloat() + view.width / 2f
        val y = location[1].toFloat() + view.height / 2f
        return PointF(x, y)
    }

    private fun startOnboarding() {
        val insetsSystemBars = WindowInsetsCompat.Type.systemBars()
        val insetTop = ViewCompat.getRootWindowInsets(window.decorView)?.getInsets(insetsSystemBars)?.top ?: 0

        val navigationViewTargetX = dipToPixels(28f).toFloat()
        val navigationViewTargetY = insetTop.toFloat() + binding.toolbar.height / 2

        val actIconOffsetX = dipToPixels(22f).toFloat()
        val actionMenuTargetX = binding.toolbar.right.toFloat() - actIconOffsetX
        val actionMenuTargetY = insetTop.toFloat() + binding.toolbar.height / 2

        val welcomeAnchorPoint = getViewLocationInWindow(binding.content.imageSmile)
        val welcomeShapeRadius = dipToPixels(60f).toFloat()
        val welcomeOverlay = OverlayLayout(
            this@MainActivity,
            welcomeAnchorPoint,
            welcomeShapeRadius,
            getString(R.string.tutorial_welcome_title),
            getString(R.string.tutorial_welcome_description)
        )
        val welcomeTarget = Target.Builder()
            .setAnchor(welcomeAnchorPoint)
            .setShape(SimpleCircle(welcomeShapeRadius))
            .setOverlay(welcomeOverlay)
            .build()

        val addScheduleAnchorPoint = getViewLocationInWindow(binding.fam.fabBase)
        val addScheduleShapeRadius = dipToPixels(60f).toFloat()
        val addScheduleOverlay = OverlayLayout(
            this@MainActivity,
            addScheduleAnchorPoint,
            addScheduleShapeRadius,
            getString(R.string.tutorial_add_schedule_title),
            getString(R.string.tutorial_add_schedule_description)
        )
        val addScheduleTarget = Target.Builder()
            .setAnchor(addScheduleAnchorPoint)
            .setShape(SimpleCircle(addScheduleShapeRadius))
            .setOverlay(addScheduleOverlay)
            .build()

        val navigationViewAnchorPoint = PointF(navigationViewTargetX, navigationViewTargetY)
        val navigationViewShapeRadius = dipToPixels(120f).toFloat()
        val navigationViewOverlay = OverlayLayout(
            this@MainActivity,
            navigationViewAnchorPoint,
            navigationViewShapeRadius,
            getString(R.string.tutorial_navigation_view_title),
            getString(R.string.tutorial_navigation_view_description)
        )
        val navigationViewTarget = Target.Builder()
            .setAnchor(navigationViewAnchorPoint)
            .setShape(SimpleCircle(navigationViewShapeRadius))
            .setOverlay(navigationViewOverlay)
            .build()

        val actionMenuAnchorPoint = PointF(actionMenuTargetX, actionMenuTargetY)
        val actionMenuShapeRadius = dipToPixels(120f).toFloat()
        val actionMenuOverlay = OverlayLayout(
            this@MainActivity,
            actionMenuAnchorPoint,
            actionMenuShapeRadius,
            getString(R.string.tutorial_action_menu_title),
            getString(R.string.tutorial_action_menu_description)
        )
        val actionMenuTarget = Target.Builder()
            .setAnchor(actionMenuAnchorPoint)
            .setShape(ActionMenuCircle(this@MainActivity, binding.toolbar, actionMenuShapeRadius))
            .setOverlay(actionMenuOverlay)
            .build()

        val finishAnchorPoint = getViewLocationInWindow(binding.content.imageSmile)
        val finishShapeRadius = dipToPixels(60f).toFloat()
        val finishOverlay = OverlayLayout(
            this@MainActivity,
            finishAnchorPoint,
            finishShapeRadius,
            getString(R.string.tutorial_finish_title),
            getString(R.string.tutorial_finish_description)
        )
        val finishTarget = Target.Builder()
            .setAnchor(finishAnchorPoint)
            .setShape(SimpleCircle(finishShapeRadius))
            .setOverlay(finishOverlay)
            .build()

        val targets = listOf(
            welcomeTarget,
            addScheduleTarget,
            navigationViewTarget,
            actionMenuTarget,
            finishTarget
        )
        val overlayColor = ContextCompat.getColor(this@MainActivity, R.color.onboarding_overlay)
        val spotlight = Spotlight.Builder(this@MainActivity)
            .setTargets(targets)
            .setBackgroundColor(overlayColor)
            .setOnSpotlightListener(object : OnSpotlightListener {
                override fun onStarted() = Unit
                override fun onEnded() {
                    presenter.completeOnboarding()
                }
            })
            .build()

        for (target in targets) {
            target.overlay?.setOnClickListener { spotlight.next() }
        }

        spotlight.start()
    }

    override fun onStart() {
        super.onStart()

        if (pendingScheduleContainerId != Int.DEFAULT) {
            consumePendingScheduleContainerId()
        }

        disposables += presenter.mainUiStateObservable
            .map { it.scheduleContainers }
            .distinctUntilChanged()
            .subscribeBy(
                onNext = { scheduleContainers ->
                    clearScheduleContainersMenu()
                    for (scheduleContainer in scheduleContainers) {
                        addScheduleContainerMenuItem(scheduleContainer)
                    }

                    val selectedScheduleContainer = presenter.getSelectedScheduleContainer()
                    selectScheduleContainerMenuItem(selectedScheduleContainer)
                }
            )

        disposables += presenter.mainUiStateObservable
            .map { it.selectedScheduleContainer }
            .distinctUntilChanged()
            .subscribeBy(
                onNext = { selectedScheduleContainer ->
                    if (isScheduleContainerDefault(selectedScheduleContainer)) {
                        clearScheduleContainer()
                        binding.content.imageSmile.visibility = View.VISIBLE
                        binding.content.textHint.visibility = View.VISIBLE

                        if (binding.fam.container.visibility == View.INVISIBLE) {
                            binding.fam.container.visibility = View.VISIBLE
                        }

                        binding.content.progressindicator.visibility = View.GONE
                    } else {
                        changeScheduleContainer(selectedScheduleContainer)
                        binding.content.imageSmile.visibility = View.GONE
                        binding.content.textHint.visibility = View.GONE

                        selectScheduleContainerMenuItem(selectedScheduleContainer)
                    }
                }
            )

        disposables += presenter.mainUiStateObservable
            .map { it.errorUiState }
            .distinctUntilChanged()
            .subscribeBy(
                onNext = { errorUiState ->
                    if (errorUiState.errorType != null) {
                        showError()
                        presenter.errorShown()
                    }

                    binding.content.progressindicator.visibility = View.GONE
                }
            )

        disposables += presenter.mainUiStateObservable
            .map { it.isLoading }
            .distinctUntilChanged()
            .subscribeBy(
                onNext = { isLoading ->
                    if (isLoading) {
                        binding.content.progressindicator.visibility = View.VISIBLE
                        setWeeksContainerViewTransparent()

                        if (isScheduleContainerDefault(presenter.getSelectedScheduleContainer())) {
                            binding.content.imageSmile.visibility = View.GONE
                            binding.content.textHint.visibility = View.GONE
                        }
                    }

                    // When isLoading flag is cleared, we don't hide progress indicator here.
                    // Instead we do it from current week fragment when its data is ready for display,
                    // so switching between schedule containers is visually as seamless as possible.
                    // In case of switching to default schedule container or receiving an error,
                    // we hide progress indicator in selectedScheduleContainer or errorUiState subscibers.
                    // We don't need to set weeks container view alpha back to normal in these cases,
                    // because weeks container fragment is assumed to be destroyed.
                }
            )

        binding.fam.container.visibility = if (presenter.isFabShown()) View.VISIBLE else View.GONE
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            presenter.clear()
        }
        onThemeChangedDisposable.dispose()
        binding.fam.fabGroup.animate().setListener(null)
        binding.fam.fabTeacher.animate().setListener(null)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (!item.isChecked) {
            for (scheduleType in ScheduleType.values()) {
                val menuItem = binding.navView.menu
                    .findItem(getContainerMenuViewId(scheduleType))
                    ?.subMenu
                    ?.findItem(item.itemId)

                if (menuItem != null) {
                    pendingScheduleContainerId = item.itemId
                    binding.drawerLayout.closeDrawer(GravityCompat.START, true)

                    binding.content.progressindicator.visibility = View.VISIBLE
                    setWeeksContainerViewTransparent()

                    binding.content.imageSmile.visibility = View.GONE
                    binding.content.textHint.visibility = View.GONE
                }
            }
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

        onDrawerOpenBackPressedCallback.isEnabled = true
    }

    override fun onDrawerClosed(drawerView: View) {
        if (pendingScheduleContainerId != Int.DEFAULT) {
            consumePendingScheduleContainerId()
        }

        onDrawerOpenBackPressedCallback.isEnabled = false
    }

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (nightMode == Configuration.UI_MODE_NIGHT_NO) {
                val color = ContextCompat.getColor(this, R.color.statusbar_nav_drawer)
                window.statusBarColor = Color.argb(
                    (slideOffset * Color.alpha(color)).toInt(),
                    Color.red(color),
                    Color.green(color),
                    Color.blue(color)
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_TOOLBAR_TITLE, supportActionBar?.title.toString())
        outState.putInt(ARG_PENDING_SCHEDULE_CONTAINER_ID, pendingScheduleContainerId)
    }

    private fun expandFam() {
        isFamOpen = true

        ViewCompat.animate(binding.fam.fabBase)
            .setDuration(ANIMATION_ROTATE_BASE_FAB_DURATION)
            .rotation(135f)
            .setInterpolator(OvershootInterpolator(2f))
            .start()

        with(binding.fam.fabGroup) {
            visibility = View.VISIBLE
            scaleX = 0f
            scaleY = 0f
            translationY = height / 4f
            animate()
                .setStartDelay(ANIMATION_SHOW_GROUP_FAB_DELAY)
                .setDuration(ANIMATION_SHOW_CHILD_FAB_DURATION)
                .scaleY(1f)
                .scaleX(1f)
                .translationY(0f)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        }
        with(binding.fam.fabTeacher) {
            visibility = View.VISIBLE
            scaleX = 0f
            scaleY = 0f
            translationY = height / 4f
            animate()
                .setStartDelay(ANIMATION_SHOW_TEACHER_FAB_DELAY)
                .setDuration(ANIMATION_SHOW_CHILD_FAB_DURATION)
                .scaleY(1f)
                .scaleX(1f)
                .translationY(0f)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        }

        binding.fam.container.isFocusable = true
        binding.fam.container.isClickable = true
        binding.fam.container.setOnClickListener { collapseFam() }
    }

    private fun collapseFam() {
        isFamOpen = false

        binding.fam.fabGroup.animate()
            .setStartDelay(ANIMATION_HIDE_CHILD_FAB_DELAY)
            .setDuration(ANIMATION_HIDE_CHILD_FAB_DURATION)
            .scaleY(0f)
            .scaleX(0f)
            .setInterpolator(FastOutSlowInInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.fam.fabGroup.animate()?.setListener(null)
                    binding.fam.fabGroup.visibility = View.INVISIBLE
                }
            })
            .start()

        binding.fam.fabTeacher.animate()
            .setStartDelay(ANIMATION_HIDE_CHILD_FAB_DELAY)
            .setDuration(ANIMATION_HIDE_CHILD_FAB_DURATION)
            .scaleY(0f)
            .scaleX(0f)
            .setInterpolator(FastOutSlowInInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.fam.fabTeacher.animate()?.setListener(null)
                    binding.fam.fabTeacher.visibility = View.INVISIBLE
                }
            })
            .start()

        ViewCompat.animate(binding.fam.fabBase)
            .setDuration(ANIMATION_ROTATE_BASE_FAB_DURATION)
            .rotation(0f)
            .setInterpolator(OvershootInterpolator(2f))
            .start()

        binding.fam.container.setOnClickListener(null)
        binding.fam.container.isClickable = false
        binding.fam.container.isFocusable = false
    }

    override fun onNewStudyGroup(studyGroup: StudyGroup) {
        presenter.clearSelectedScheduleContainer()
        presenter.addGroup(studyGroup)
    }

    override fun onNewTeacher(teacher: Teacher) {
        presenter.clearSelectedScheduleContainer()
        presenter.addTeacher(teacher)
    }

    override fun onDialogFragmentError(throwable: Throwable) {
        val errorMessageResource = AndroidUtils.getErrorMessageResId(throwable)
        val snackbar = Snackbar.make(binding.content.constraintLayout, errorMessageResource, Snackbar.LENGTH_LONG)
        snackbar.anchorView = binding.fam.fabBase
        ViewCompat.setOnApplyWindowInsetsListener(snackbar.view) { _, insets -> insets }
        snackbar.show()
    }

    private fun changeScheduleContainer(scheduleContainer: ScheduleContainer) {
        supportActionBar?.title = scheduleContainer.value

        val weeksContainerFragment = supportFragmentManager.findFragmentById(R.id.content)
        if (weeksContainerFragment == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.content, WeeksContainerFragment())
                .commit()
        } else {
            weeksContainerFragment as WeeksContainerFragment
            weeksContainerFragment.swapData()
        }
    }

    private fun clearScheduleContainer() {
        supportActionBar?.title = String.DEFAULT

        val scheduleContainerFragment = supportFragmentManager.findFragmentById(R.id.content)
        if (scheduleContainerFragment != null) {
            supportFragmentManager
                .beginTransaction()
                .remove(scheduleContainerFragment)
                .commit()
        }
    }

    private fun addScheduleContainerMenuItem(scheduleContainer: ScheduleContainer) {
        val subMenu = binding.navView.menu
            .findItem(getContainerMenuViewId(scheduleContainer.type))
            ?.subMenu
        val menuItem = subMenu?.add(Menu.NONE, scheduleContainer.id, Menu.NONE, scheduleContainer.value)
        menuItem?.isCheckable = true
    }

    private fun selectScheduleContainerMenuItem(scheduleContainer: ScheduleContainer) {
        val subMenu = binding.navView.menu
            .findItem(getContainerMenuViewId(scheduleContainer.type))
            ?.subMenu
        if (subMenu != null) {
            for (i in 0 until subMenu.size()) {
                val item = subMenu.getItem(i)
                if (scheduleContainer.id == item.itemId) {
                    binding.navView.setCheckedItem(item.itemId)
                    break
                }
            }
        }
    }

    private fun clearScheduleContainersMenu() {
        for (type in ScheduleType.values()) {
            binding.navView.menu
                .findItem(getContainerMenuViewId(type))
                ?.subMenu
                ?.clear()
        }
    }

    private fun getContainerMenuViewId(scheduleType: ScheduleType): Int {
        return when (scheduleType) {
            ScheduleType.STUDYGROUP -> R.id.group_submenu
            ScheduleType.TEACHER -> R.id.teacher_submenu
            else -> ResourcesCompat.ID_NULL
        }
    }

    private fun setWeeksContainerViewTransparent() {
        val weeksContainerView = supportFragmentManager.findFragmentById(R.id.content)
            ?.view
            ?.findViewById<View>(R.id.viewpager_weeks)
        weeksContainerView?.alpha = 0f
    }

    private fun isScheduleContainerDefault(scheduleContainer: ScheduleContainer): Boolean {
        return scheduleContainer.id == Int.DEFAULT
    }

    private fun showError() {
        val snackbar = Snackbar.make(binding.content.constraintLayout, R.string.error_general, Snackbar.LENGTH_LONG)
        snackbar.anchorView = binding.fam.fabBase
        ViewCompat.setOnApplyWindowInsetsListener(snackbar.view) { _, insets -> insets }
        snackbar.show()
    }

    private fun consumePendingScheduleContainerId() {
        presenter.changeScheduleContainer(pendingScheduleContainerId)
        pendingScheduleContainerId = Int.DEFAULT
    }

    override fun onScheduleContainerDeleted(scheduleContainer: ScheduleContainer) {
        presenter.clearSelectedScheduleContainer()
    }

    companion object {
        private const val ARG_TOOLBAR_TITLE = "toolbarTitle"
        private const val ARG_PENDING_SCHEDULE_CONTAINER_ID = "pendingScheduleContainerId"

        private const val ANIMATION_ROTATE_BASE_FAB_DURATION = 300L
        private const val ANIMATION_SHOW_CHILD_FAB_DURATION = 200L
        private const val ANIMATION_SHOW_GROUP_FAB_DELAY = 75L
        private const val ANIMATION_SHOW_TEACHER_FAB_DELAY = 0L
        private const val ANIMATION_HIDE_CHILD_FAB_DURATION = 100L
        private const val ANIMATION_HIDE_CHILD_FAB_DELAY = 0L

        private const val SETTINGS_CLOSE_NAVIGATION_DRAWER_DELAY = 500L
    }
}
