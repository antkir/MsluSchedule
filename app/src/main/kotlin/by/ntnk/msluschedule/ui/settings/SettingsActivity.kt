package by.ntnk.msluschedule.ui.settings

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NavUtils
import androidx.core.content.ContextCompat
import androidx.fragment.app.ListFragment
import androidx.preference.*
import by.ntnk.msluschedule.BuildConfig
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import by.ntnk.msluschedule.utils.onThemeChanged
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class SettingsActivity : AppCompatActivity(), HasAndroidInjector {
    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> = dispatchingAndroidInjector

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (nightMode == Configuration.UI_MODE_NIGHT_NO) {
                val uiFlags = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                window.decorView.systemUiVisibility = uiFlags
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        @Inject
        lateinit var sharedPreferencesRepository: SharedPreferencesRepository

        override fun onAttach(context: Context) {
            AndroidSupportInjection.inject(this)
            super.onAttach(context)
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val view = super.onCreateView(inflater, container, savedInstanceState)
            view!!.setBackgroundColor(ContextCompat.getColor(container!!.context, R.color.surface))
            return view
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_settings)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setHasOptionsMenu(true)

            val themePreference = findPreference<ListPreference>(getString(R.string.key_theme))
            val values = arrayOf(AppCompatDelegate.MODE_NIGHT_NO.toString(),
                                 AppCompatDelegate.MODE_NIGHT_YES.toString(),
                                 AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM.toString())
            themePreference!!.entryValues = values
            themePreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, value ->
                preference as ListPreference
                value as String
                val index = preference.findIndexOfValue(value)
                if (index in preference.entries.indices) {
                    preference.setSummary(preference.entries[index])
                }
                if (value != sharedPreferencesRepository.getThemeMode()) {
                    AppCompatDelegate.setDefaultNightMode(value.toInt())
                    onThemeChanged.onNext(true)
                    activity?.recreate()
                }
                return@OnPreferenceChangeListener true
            }
            // Trigger the listener immediately with the preference's current value.
            val value = sharedPreferencesRepository.getThemeMode()
            themePreference.onPreferenceChangeListener!!.onPreferenceChange(themePreference, value)

            val keyShowAddSchedule = getString(R.string.key_show_add_schedule)
            findPreference<SwitchPreferenceCompat>(keyShowAddSchedule)!!.isSingleLineTitle = false

            val keyFullSubjects = getString(R.string.key_full_subjects)
            findPreference<CheckBoxPreference>(keyFullSubjects)!!.isSingleLineTitle = false
            val keyHidePEClasses = getString(R.string.key_hide_pe_classes)
            findPreference<CheckBoxPreference>(keyHidePEClasses)!!.isSingleLineTitle = false

            val sendFeedback = findPreference<Preference>(getString(R.string.key_send_feedback))
            sendFeedback!!.setOnPreferenceClickListener {
                try {
                    val uri = Uri.parse("market://details?id=${requireContext().packageName}")
                    val playStoreIntent = Intent(Intent.ACTION_VIEW, uri)
                    playStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or
                                                     Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    startActivity(playStoreIntent)
                } catch (e: Exception) {
                    val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse(getString(R.string.send_feedback_uri)))
                    val text = "App Version: ${BuildConfig.VERSION_NAME}\n" +
                            "Device and Android Version: ${Build.MODEL} - ${Build.VERSION.RELEASE}\n\n"
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback")
                    emailIntent.putExtra(Intent.EXTRA_TEXT, text)
                    startActivity(Intent.createChooser(emailIntent, getString(R.string.pref_send_feedback_title)))
                }
                return@setOnPreferenceClickListener true
            }

            val libraries = findPreference<Preference>(getString(R.string.key_libraries))
            libraries!!.setOnPreferenceClickListener {
                activity?.supportFragmentManager?.beginTransaction()
                        ?.setCustomAnimations(R.anim.dialog_zoom_in, 0, 0, R.anim.dialog_zoom_out)
                        ?.replace(android.R.id.content, SettingsLibrariesFragment())
                        ?.addToBackStack(null)
                        ?.commit()
                return@setOnPreferenceClickListener true
            }

            val aboutPreference = findPreference<Preference>(getString(R.string.key_about))
            aboutPreference!!.summary = BuildConfig.VERSION_NAME
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            if (item.itemId == android.R.id.home) {
                NavUtils.navigateUpFromSameTask(requireActivity())
                return true
            }
            return super.onOptionsItemSelected(item)
        }

        class SettingsLibrariesFragment : ListFragment(), AdapterView.OnItemClickListener {
            private lateinit var libraryUrls: Array<String>

            override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                      savedInstanceState: Bundle?): View? {
                return inflater.inflate(R.layout.fragment_libraries, container, false)
            }

            override fun onActivityCreated(savedInstanceState: Bundle?) {
                super.onActivityCreated(savedInstanceState)
                libraryUrls = resources.getStringArray(R.array.library_urls)
                val adapter = ArrayAdapter.createFromResource(
                        requireActivity(), R.array.library_list, android.R.layout.simple_list_item_1)
                listAdapter = adapter
                listView.onItemClickListener = this
            }

            override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(libraryUrls[position]))
                startActivity(browserIntent)
            }
        }
    }

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, SettingsActivity::class.java)
            context.startActivity(intent)
        }
    }
}
