package by.ntnk.msluschedule.network.data

import android.util.SparseArray
import java.util.AbstractMap.SimpleImmutableEntry

data class ScheduleFilter(private val data: SparseArray<String>) {
    constructor() : this(SparseArray<String>())

    val size
        get() = data.size()

    fun getEntry(key: Int): Map.Entry<Int, String> {
        val value = getValue(key)
        return SimpleImmutableEntry(key, value)
    }

    fun getValue(key: Int): String = data.get(key)

    fun valueAt(index: Int): String = data.valueAt(index)

    fun keyAt(index: Int): Int = data.keyAt(index)

    fun put(key: Int, value: String) = data.put(key, value)

    fun containsValue(value: String): Boolean {
        var ret = false
        for (i in 0 until size) {
            if (valueAt(i) == value) ret = true
        }
        return ret
    }
}
