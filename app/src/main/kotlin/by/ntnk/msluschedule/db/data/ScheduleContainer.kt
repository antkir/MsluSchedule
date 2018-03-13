package by.ntnk.msluschedule.db.data

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
data class ScheduleContainer constructor(
        @PrimaryKey val id: Int,
        val name: String,
        val type: String,
        val faculty: Int,
        val course: Int,
        val year: Int
)
