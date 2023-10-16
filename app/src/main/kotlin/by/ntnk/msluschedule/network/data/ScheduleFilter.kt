package by.ntnk.msluschedule.network.data

import androidx.collection.SparseArrayCompat
import by.ntnk.msluschedule.utils.EMPTY_STRING

data class ScheduleFilter(private val data: SparseArrayCompat<String>) {
    val size: Int
        get() = data.size()

    constructor() : this(SparseArrayCompat<String>())

    fun getValueOrDefault(key: Int, defaultValue: String = EMPTY_STRING): String {
        return data.get(key, defaultValue)
    }

    fun valueAt(index: Int): String {
        return data.valueAt(index)
    }

    fun keyAt(index: Int): Int {
        return data.keyAt(index)
    }

    /**
     * Adds a mapping from the specified key to the specified value,
     * replacing the previous mapping from the specified key if there
     * was one.
     */
    fun put(key: Int, value: String) {
        data.put(key, value)
    }

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
        if (size != other.size) return false
        for (i in 0 until size) {
            if (data.get(i) != other.data.get(i)) return false
        }

        return true
    }

    override fun hashCode(): Int {
        return arrayOf(data).contentHashCode()
    }

    override fun toString(): String {
        return data.toString()
    }
}
