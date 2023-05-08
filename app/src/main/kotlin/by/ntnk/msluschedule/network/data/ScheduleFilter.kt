package by.ntnk.msluschedule.network.data

import androidx.collection.SparseArrayCompat
import by.ntnk.msluschedule.utils.DEFAULT
import by.ntnk.msluschedule.utils.contentEquals
import by.ntnk.msluschedule.utils.contentHashCode

data class ScheduleFilter(private val data: SparseArrayCompat<String>) {

    val size: Int
        get() = data.size()

    constructor() : this(SparseArrayCompat<String>())

    fun getValue(key: Int): String = data.get(key, String.DEFAULT)

    fun valueAt(index: Int): String = data.valueAt(index)

    fun keyAt(index: Int): Int = data.keyAt(index)

    fun put(key: Int, value: String) = data.put(key, value)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScheduleFilter) return false

        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int = data.contentHashCode()
}
