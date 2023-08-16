package by.ntnk.msluschedule.data

import by.ntnk.msluschedule.utils.DEFAULT
import by.ntnk.msluschedule.utils.ScheduleType

data class ScheduleContainer(val id: Int, val value: String, val type: ScheduleType, val year: Int) {
    companion object {
        val DEFAULT = ScheduleContainer(Int.DEFAULT, String.DEFAULT, ScheduleType.DEFAULT, Int.DEFAULT)
    }
}
