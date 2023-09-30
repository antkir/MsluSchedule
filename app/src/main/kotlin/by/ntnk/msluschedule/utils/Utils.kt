package by.ntnk.msluschedule.utils

import java.util.AbstractMap

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

typealias ImmutableEntry = AbstractMap.SimpleImmutableEntry<Int, String>

class InvalidYearException : Exception()

class HttpStatusException(message: String, statusCode: Int, url: String) :
    org.jsoup.HttpStatusException(message, statusCode, url)
