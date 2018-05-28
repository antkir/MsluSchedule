package by.ntnk.msluschedule.utils

import android.content.Context
import android.net.ConnectivityManager
import java.util.AbstractMap

const val EMPTY_STRING = ""

// Passing year = 0 and any valid course value to the server
// allows to get all groups of the selected faculty
const val COURSE_VALUE = 1

const val PRESENTER_ID_KEY = "PresenterID"

const val MONDAY = "ПН"
const val TUESDAY = "ВТ"
const val WEDNESDAY = "СР"
const val THURSDAY = "ЧТ"
const val FRIDAY = "ПТ"
const val SATURDAY = "СБ"
const val SUNDAY = "ВС"

typealias ImmutableEntry = AbstractMap.SimpleImmutableEntry<Int, String>

class InvalidYearException : Exception()

fun isNetworkAccessible(context: Context): Boolean {
    val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return connectivityManager.activeNetworkInfo != null &&
            connectivityManager.activeNetworkInfo.isConnected
}