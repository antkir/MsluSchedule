/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package by.ntnk.msluschedule.ui.adapters

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.viewpager.widget.PagerAdapter
import timber.log.Timber

/**
 * Implementation of [PagerAdapter] that uses a [Fragment] to manage each page.
 * This class also handles saving and restoring of fragment's state.
 *
 * This class differs from the original one in that it:
 *  - is converted to Kotlin.
 *  - does not save [Fragment] states.
 *  - in [restoreState] removes fragments that does not belong to this adapter from the passed [FragmentManager].
 *  - has a new method [getFragment].
 *
 * This version of the pager is more useful when there are a large number
 * of pages, working more like a list view. When pages are not visible to
 * the user, their entire fragment may be destroyed, only keeping the saved
 * state of that fragment. This allows the pager to hold on to much less
 * memory associated with each visited page as compared to
 * [androidx.fragment.app.FragmentPagerAdapter] at the cost of potentially more overhead when
 * switching between pages.
 *
 * When using [FragmentStatePagerAdapter]
 * the host [androidx.viewpager.widget.ViewPager] must have a valid ID set.
 *
 * Subclasses only need to implement [getItem] and [getCount] to have a working adapter.
 */
abstract class FragmentStatePagerAdapter(private val mFragmentManager: FragmentManager) : PagerAdapter() {

    private var mCurTransaction: FragmentTransaction? = null

    private val mFragments = mutableListOf<Fragment?>()
    private var mCurrentPrimaryItem: Fragment? = null
    private var mExecutingFinishUpdate = false

    /**
     * Return the Fragment associated with a specified position.
     */
    abstract fun getItem(position: Int): Fragment

    override fun startUpdate(container: ViewGroup) {
        check(container.id != View.NO_ID) {
            "ViewPager with adapter $this requires a view id"
        }
    }

    /**
     * Returns the [Fragment] associated with a specified position from the initiated fragments list or
     * null if the [Fragment] for such position doesn't exist.
     */
    open fun getFragment(position: Int): Fragment? {
        if (position >= 0 && position < mFragments.size) {
            return mFragments[position]
        }
        return null
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        // If we already have this item instantiated, there is nothing
        // to do.  This can happen when we are restoring the entire pager
        // from its saved state, where the fragment manager has already
        // taken care of restoring the fragments we previously had instantiated.
        if (mFragments.size > position) {
            val f = mFragments[position]
            if (f != null) {
                return f
            }
        }

        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction()
        }

        val fragment = getItem(position)
        Timber.v("Adding item #$position: fragment=$fragment")
        while (mFragments.size <= position) {
            mFragments.add(null)
        }
        fragment.setMenuVisibility(false)

        mFragments[position] = fragment
        mCurTransaction!!.add(container.id, fragment)
        mCurTransaction!!.setMaxLifecycle(fragment, Lifecycle.State.STARTED)

        return fragment
    }

    override fun destroyItem(container: ViewGroup, position: Int, any: Any) {
        val fragment = any as Fragment

        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction()
        }
        Timber.v("Removing item #$position: fragment=$fragment, view=${fragment.view}")
        mFragments[position] = null
        mCurTransaction!!.remove(fragment)
        if (fragment == mCurrentPrimaryItem) {
            mCurrentPrimaryItem = null
        }
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, any: Any) {
        val fragment = any as Fragment
        if (fragment != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                mCurrentPrimaryItem!!.setMenuVisibility(false)

                if (mCurTransaction == null) {
                    mCurTransaction = mFragmentManager.beginTransaction()
                }
                mCurTransaction!!.setMaxLifecycle(mCurrentPrimaryItem!!, Lifecycle.State.STARTED)
            }

            fragment.setMenuVisibility(true)

            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction()
            }
            mCurTransaction!!.setMaxLifecycle(fragment, Lifecycle.State.RESUMED)

            mCurrentPrimaryItem = fragment
        }
    }

    override fun finishUpdate(container: ViewGroup) {
        if (mCurTransaction != null) {
            // We drop any transactions that attempt to be committed
            // from a re-entrant call to finishUpdate(). We need to
            // do this as a workaround for Robolectric running measure/layout
            // calls inline rather than allowing them to be posted
            // as they would on a real device.
            if (!mExecutingFinishUpdate) {
                try {
                    mExecutingFinishUpdate = true
                    mCurTransaction!!.commitNowAllowingStateLoss()
                } finally {
                    mExecutingFinishUpdate = false
                }
            }
            mCurTransaction = null
        }
    }

    override fun isViewFromObject(view: View, any: Any): Boolean {
        return (any as Fragment).view === view
    }

    override fun saveState(): Parcelable? {
        var state: Bundle? = null
        for (i in mFragments.indices) {
            val f = mFragments[i]
            if (f != null && f.isAdded) {
                if (state == null) {
                    state = Bundle()
                }
                val key = "f$i"
                mFragmentManager.putFragment(state, key, f)
            }
        }
        return state
    }

    override fun restoreState(state: Parcelable?, loader: ClassLoader?) {
        if (state != null) {
            state as Bundle?
            state.classLoader = loader
            mFragments.clear()
            for (key in state.keySet()) {
                if (key.startsWith("f")) {
                    val index = Integer.parseInt(key.substring(1))
                    val f = mFragmentManager.getFragment(state, key)
                    if (f != null) {
                        while (mFragments.size <= index) {
                            mFragments.add(null)
                        }
                        f.setMenuVisibility(false)
                        mFragments[index] = f
                    } else {
                        Timber.w("Bad fragment at key $key")
                    }
                }
            }
        }

        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction()
        }

        if (mFragmentManager.fragments.isNotEmpty()) {
            for (f in mFragmentManager.fragments) {
                if (!mFragments.contains(f)) {
                    mCurTransaction!!.remove(f)
                }
            }
        }
    }
}
