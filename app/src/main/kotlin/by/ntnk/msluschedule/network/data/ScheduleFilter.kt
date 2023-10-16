package by.ntnk.msluschedule.network.data

import by.ntnk.msluschedule.utils.EMPTY_STRING
import by.ntnk.msluschedule.utils.Entry

data class ScheduleFilter(private val data: MutableList<Entry<String, String>>) {
    val size: Int
        get() = data.size

    constructor() : this(mutableListOf())

    fun getValueOrDefault(key: String, defaultValue: String = EMPTY_STRING): String {
        return data.find { it.key == key }?.value ?: defaultValue
    }

    fun valueAt(index: Int): String {
        return data[index].value
    }

    fun keyAt(index: Int): String {
        return data[index].key
    }

    /**
     * Adds a mapping from the specified key to the specified value,
     * replacing the previous mapping from the specified key if there
     * was one.
     */
    fun put(key: String, value: String) {
        val iterator = data.listIterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key == key) {
                iterator.set(Entry(key, value))
                return
            }
        }
        data.add(Entry(key, value))
    }

    fun containsValue(value: String): Boolean {
        return data.any { it.value == value }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScheduleFilter
        if (size != other.size) return false
        for (i in 0 until size) {
            if (data[i] != other.data[i]) return false
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
