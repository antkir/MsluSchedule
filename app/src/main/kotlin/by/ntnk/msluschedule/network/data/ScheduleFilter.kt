package by.ntnk.msluschedule.network.data

import androidx.collection.SparseArrayCompat
import by.ntnk.msluschedule.utils.DEFAULT
import by.ntnk.msluschedule.utils.contentEquals
import by.ntnk.msluschedule.utils.contentHashCode

data class ScheduleFilter(private val data: SparseArrayCompat<String>) {
    val size: Int
        get() = data.size()

    constructor() : this(SparseArrayCompat<String>())

    fun getValueOrDefault(key: Int, defaultValue: String = String.DEFAULT): String {
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScheduleFilter) return false

        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int = data.contentHashCode()
}
