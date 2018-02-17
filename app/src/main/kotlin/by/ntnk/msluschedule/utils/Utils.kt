package by.ntnk.msluschedule.utils

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers

const val EMPTY_STRING = ""

// Passing year = 0 and any valid course value to the server
// allows to get all groups of the selected faculty
const val COURSE_VALUE = 1

val uiScheduler: Scheduler = AndroidSchedulers.mainThread()
