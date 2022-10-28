package by.ntnk.msluschedule.db.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Week::class,
            parentColumns = ["id"],
            childColumns = ["weekId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["weekId"])]
)
data class Weekday constructor(
    val value: String,
    val weekId: Int,
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)
