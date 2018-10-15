package by.ntnk.msluschedule.utils

import java.util.AbstractMap

const val EMPTY_STRING = ""

// Passing year = 0 and any valid course value to the server
// allows to get all groups of the selected faculty
const val COURSE_VALUE = 1

const val INVALID_VALUE = -1

const val PRESENTER_ID_KEY = "PresenterID"

const val MONDAY = "ПН"
const val TUESDAY = "ВТ"
const val WEDNESDAY = "СР"
const val THURSDAY = "ЧТ"
const val FRIDAY = "ПТ"
const val SATURDAY = "СБ"
const val SUNDAY = "ВС"

const val WEEKDAYS_NUMBER = 7

fun Boolean.toInt() = if (this) 1 else 0

typealias ImmutableEntry = AbstractMap.SimpleImmutableEntry<Int, String>

class InvalidYearException : Exception()

class HttpStatusException(message: String, statusCode: Int, url: String)
    : org.jsoup.HttpStatusException(message, statusCode, url)
