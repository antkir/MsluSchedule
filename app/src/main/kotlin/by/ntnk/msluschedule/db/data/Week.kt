package by.ntnk.msluschedule.db.data

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey

@Entity(
        foreignKeys = [
            ForeignKey(
                    entity = ScheduleContainer::class,
                    parentColumns = ["id"],
                    childColumns = ["containerId"],
                    onDelete = ForeignKey.CASCADE
            )
        ],
        indices = [Index(value = ["containerId"])]
)
data class Week constructor(
        val key: Int,
        val value: String,
        val containerId: Int,
        @PrimaryKey(autoGenerate = true) val id: Int = 0
)
