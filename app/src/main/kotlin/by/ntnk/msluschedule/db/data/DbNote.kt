package by.ntnk.msluschedule.db.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
        foreignKeys = [
            ForeignKey(
                    entity = Weekday::class,
                    parentColumns = ["id"],
                    childColumns = ["weekdayId"],
                    onDelete = ForeignKey.CASCADE)
        ],
        indices = [Index(value = ["weekdayId"])]
)
data class DbNote constructor(
        val text: String,
        val subject: String,
        val weekdayId: Int,
        @PrimaryKey(autoGenerate = true) val id: Int = 0
)
