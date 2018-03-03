package by.ntnk.msluschedule.network.data

import java.util.AbstractMap.SimpleEntry

data class ScheduleFilter(val data: LinkedHashMap<Int, String>) {
    constructor() : this(LinkedHashMap<Int, String>())

    val size
        get() = data.size

    fun getEntry(key: Int): Map.Entry<Int, String> {
        val value = data.getValue(key)
        return SimpleEntry(key, value)
    }

    fun getValue(key: Int) = data.getValue(key)

    fun valueAt(position: Int): String = data.values.toList()[position]

    fun keyAt(position: Int): Int = data.keys.toList()[position]
}
