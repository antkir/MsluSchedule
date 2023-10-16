package by.ntnk.msluschedule.utils

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

const val EMPTY_STRING = ""

const val INVALID_VALUE = -1

const val PRESENTER_ID_KEY = "PresenterID"

object Days {
    const val MONDAY = "ПН"
    const val TUESDAY = "ВТ"
    const val WEDNESDAY = "СР"
    const val THURSDAY = "ЧТ"
    const val FRIDAY = "ПТ"
    const val SATURDAY = "СБ"
    const val SUNDAY = "ВС"

    fun list() = listOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY)
    fun num() = list().size
}

fun Boolean.toInt() = if (this) 1 else 0

inline fun <T> T.takeIfOrDefault(predicate: (T) -> Boolean, defaultValue: T): T {
    return takeIf(predicate) ?: defaultValue
}

inline fun <T> T.takeUnlessOrDefault(predicate: (T) -> Boolean, defaultValue: T): T {
    return takeUnless(predicate) ?: defaultValue
}

inline fun <T> Iterable<T>.firstOrDefault(predicate: (T) -> Boolean, defaultValue: T): T {
    return firstOrNull(predicate) ?: defaultValue
}

data class Entry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V>

class InvalidYearException : RuntimeException()

class NetworkApiVersionException : RuntimeException()

class NoDataOnServerException : RuntimeException()

class HttpStatusException(message: String, statusCode: Int, url: String) :
    org.jsoup.HttpStatusException(message, statusCode, url)

/*
 * The week key values for the original schedule API started at about 400 in
 * the app's release year (2018) and were incremented every year by about 50.
 * Let's just assume that newer schedule APIs will have their week keys set in 0-399 range
 * instead of adding a networkApiVersion field to database entities.
 */
fun getNetworkApiVersionFromWeekKey(weekKey: Int): NetworkApiVersion {
    return if (weekKey < 400) NetworkApiVersion.MYUNIVERSITY else NetworkApiVersion.ORIGINAL
}

fun isUnexpectedException(t: Throwable): Boolean {
    return t !is ConnectException &&
        t !is SocketTimeoutException &&
        t !is UnknownHostException &&
        t !is HttpStatusException &&
        t !is InvalidYearException &&
        t !is NetworkApiVersionException &&
        t !is NoDataOnServerException
}
