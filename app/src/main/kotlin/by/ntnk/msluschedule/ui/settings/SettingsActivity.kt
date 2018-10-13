package by.ntnk.msluschedule.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.ListFragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import by.ntnk.msluschedule.BuildConfig
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import by.ntnk.msluschedule.utils.onThemeChanged
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.HasSupportFragmentInjector
import javax.inject.Inject

class SettingsActivity : AppCompatActivity(), HasSupportFragmentInjector {
    @Inject
    lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentDispatchingAndroidInjector

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        @Inject
        lateinit var sharedPreferencesRepository: SharedPreferencesRepository

        override fun onAttach(context: Context?) {
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

            val themePreference = findPreference(getString(R.string.key_theme))
            themePreference as ListPreference
            val values = arrayOf(AppCompatDelegate.MODE_NIGHT_NO.toString(),
                                 AppCompatDelegate.MODE_NIGHT_YES.toString(),
                                 AppCompatDelegate.MODE_NIGHT_AUTO.toString())
            themePreference.entryValues = values
            themePreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, value ->
                preference as ListPreference
                value as String
                val index = preference.findIndexOfValue(value)
                if (index in preference.entries.indices) {
                    preference.setSummary(preference.entries[index])
                }
                if (value != sharedPreferencesRepository.getThemeMode(getString(R.string.key_theme))) {
                    AppCompatDelegate.setDefaultNightMode(value.toInt())
                    onThemeChanged.onNext(true)
                    activity?.recreate()
                }
                return@OnPreferenceChangeListener true
            }
            // Trigger the listener immediately with the preference's current value.
            val value = sharedPreferencesRepository.getThemeMode(getString(R.string.key_theme))
            themePreference.onPreferenceChangeListener.onPreferenceChange(themePreference, value)

            val sendFeedback = findPreference(getString(R.string.key_send_feedback))
            sendFeedback.setOnPreferenceClickListener {
                val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse(getString(R.string.send_feedback_uri)))
                val text = String.format("App Version: %s\nDevice and Android Version: %s - %s\n\n",
                                         BuildConfig.VERSION_NAME, Build.MODEL, Build.VERSION.RELEASE)
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback")
                emailIntent.putExtra(Intent.EXTRA_TEXT, text)
                startActivity(Intent.createChooser(emailIntent, getString(R.string.send_feedback_title)))
                return@setOnPreferenceClickListener true
            }

            val libraries = findPreference(getString(R.string.key_libraries))
            libraries.setOnPreferenceClickListener {
                activity?.supportFragmentManager?.beginTransaction()
                        ?.setCustomAnimations(R.anim.dialog_zoom_in, 0, 0, R.anim.dialog_zoom_out)
                        ?.replace(android.R.id.content, SettingsLibrariesFragment())
                        ?.addToBackStack(null)
                        ?.commit()
                return@setOnPreferenceClickListener true
            }

            val aboutPreference = findPreference(getString(R.string.key_about))
            aboutPreference.summary = BuildConfig.VERSION_NAME
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                activity?.finish()
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
                        activity!!, R.array.library_list, android.R.layout.simple_list_item_1)
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
