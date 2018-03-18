package by.ntnk.msluschedule.db.data

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey

@Entity(
        foreignKeys = [
            ForeignKey(
                    entity = Week::class,
                    parentColumns = ["id"],
                    childColumns = ["weekId"],
                    onDelete = ForeignKey.CASCADE)
        ],
        indices = [Index(value = ["weekId"])]
)
data class Weekday constructor(
        val value: String,
        val weekId: Int,
        @PrimaryKey(autoGenerate = true) val id: Int = 0
)
