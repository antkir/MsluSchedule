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
data class Week constructor(val containerId: Int, val key: Int, val value: String) {
    @PrimaryKey(autoGenerate = true) var id: Int = 0
}
