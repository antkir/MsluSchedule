package by.ntnk.msluschedule.network.data

import android.util.SparseArray
import by.ntnk.msluschedule.utils.ImmutableEntry
import java.util.Arrays

data class ScheduleFilter(private val data: SparseArray<String>) {
    val size
        get() = data.size()

    constructor() : this(SparseArray<String>())

    fun getEntry(key: Int): Map.Entry<Int, String> {
        val value = getValue(key)
        return ImmutableEntry(key, value)
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScheduleFilter
        if (data.size() != other.data.size()) return false
        for (i in 0 until size) {
            if (data.get(i) != other.data.get(i)) return false
        }

        return true
    }

    override fun hashCode(): Int = Arrays.hashCode(arrayOf(data))
}
