package by.ntnk.msluschedule.network.data

import androidx.collection.SparseArrayCompat
import by.ntnk.msluschedule.utils.DEFAULT

data class ScheduleFilter(private val data: SparseArrayCompat<String>) {

    /**
     * A boolean flag to differentiate between a default (uninitialized)
     * ScheduleFilter object and a ScheduleFilter object with empty data container.
     */
    private var default: Boolean = false

    val size: Int
        get() = data.size()

    constructor() : this(SparseArrayCompat<String>())

    private constructor(isDefault: Boolean) : this(SparseArrayCompat<String>()) {
        default = isDefault
    }

    fun getValue(key: Int): String = data.get(key, String.DEFAULT)

    fun valueAt(index: Int): String = data.valueAt(index)

    fun keyAt(index: Int): Int = data.keyAt(index)

    fun put(key: Int, value: String) = data.put(key, value)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScheduleFilter
        if (default != other.default) return false
        if (data.size() != other.data.size()) return false
        for (i in 0 until size) {
            if (data.get(i) != other.data.get(i)) return false
        }

        return true
    }

    override fun hashCode(): Int = arrayOf(data).contentHashCode()

    override fun toString(): String = data.toString()

    companion object {
        val DEFAULT: ScheduleFilter = ScheduleFilter(isDefault = true)
    }
}
