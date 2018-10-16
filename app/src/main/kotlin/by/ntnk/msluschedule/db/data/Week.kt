package by.ntnk.msluschedule.db.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
