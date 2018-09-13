package by.ntnk.msluschedule.db.data

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey

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
        val weekdayId: Int,
        @PrimaryKey(autoGenerate = true) val id: Int = 0
)
