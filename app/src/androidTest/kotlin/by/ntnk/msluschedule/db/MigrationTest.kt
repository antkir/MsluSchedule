package by.ntnk.msluschedule.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    @Throws(IOException::class)
    fun migrate10To11() {
        // Arrange
        val scheduleContainerValuesList = mutableListOf<ContentValues>()
        scheduleContainerValuesList.add(ContentValues()
            .apply {
                put("key", 18)
                put("name", "Русецкая Ирина Леонидовна")
                put("type", "TEACHER")
                put("year", 2022)
                put("faculty", 0)
                put("course", 0)
                put("id", 22)
            }
        )
        scheduleContainerValuesList.add(ContentValues()
            .apply {
                put("key", 391)
                put("name", "Прусс Анна Яковлевна")
                put("type", "TEACHER")
                put("year", 2022)
                put("faculty", -2147483648)
                put("course", -2147483648)
                put("id", 54)
            }
        )
        scheduleContainerValuesList.add(ContentValues()
            .apply {
                put("key", 1665)
                put("name", "401/1 (Переводческий)")
                put("type", "STUDYGROUP")
                put("year", 2023)
                put("faculty", 7)
                put("course", 4)
                put("id", 84)
            }
        )
        scheduleContainerValuesList.add(
            ContentValues()
                .apply {
                    put("key", -208703)
                    put("name", "104/1 (Романских языков)")
                    put("type", "STUDYGROUP")
                    put("year", 2023)
                    put("faculty", 3)
                    put("course", 1)
                    put("id", 86)
                }
        )

        helper.createDatabase(TEST_DATABASE, 10).apply {
            execSQL("DROP TABLE IF EXISTS ScheduleContainer")
            execSQL(
                """
                CREATE TABLE ScheduleContainer (
                    key INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    type TEXT NOT NULL,
                    year INTEGER NOT NULL,
                    faculty INTEGER NOT NULL,
                    course INTEGER NOT NULL,
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
                )
                """.trimIndent()
            )

            for (scheduleContainerValues in scheduleContainerValuesList) {
                insert("ScheduleContainer", SQLiteDatabase.CONFLICT_REPLACE, scheduleContainerValues)
            }

            close()
        }

        // Act
        val db = helper.runMigrationsAndValidate(TEST_DATABASE, 11, true, Migrations.MIGRATION_10_11)

        // Assert
        val cursor = db.query("SELECT * FROM ScheduleContainer ORDER BY id")
        assertEquals(cursor.count, scheduleContainerValuesList.size)
        for (i in 0..<cursor.count) {
            cursor.moveToPosition(i)
            val expectedscheduleContainerValues = scheduleContainerValuesList[i]

            val keyIndex = cursor.getColumnIndex("key")
            val nameIndex = cursor.getColumnIndex("name")
            val typeIndex = cursor.getColumnIndex("type")
            val yearIndex = cursor.getColumnIndex("year")
            val facultyIndex = cursor.getColumnIndex("faculty")
            val courseIndex = cursor.getColumnIndex("course")
            val idIndex = cursor.getColumnIndex("id")

            val expectedKey = scheduleContainerKeyToString(expectedscheduleContainerValues.getAsInteger("key"))
            assertEquals(cursor.getString(keyIndex), expectedKey)
            assertEquals(cursor.getString(nameIndex), expectedscheduleContainerValues.getAsString("name"))
            assertEquals(cursor.getString(typeIndex), expectedscheduleContainerValues.getAsString("type"))
            assertEquals(cursor.getInt(yearIndex), expectedscheduleContainerValues.getAsInteger("year"))
            assertEquals(cursor.getInt(facultyIndex), expectedscheduleContainerValues.getAsInteger("faculty"))
            assertEquals(cursor.getInt(courseIndex), expectedscheduleContainerValues.getAsInteger("course"))
            assertEquals(cursor.getInt(idIndex), expectedscheduleContainerValues.getAsInteger("id"))
        }
        cursor.close()

        db.close()
    }

    private companion object {
        private const val TEST_DATABASE = "test-db"

        private const val GROUP_CODE_OFFSET = 100
        private fun scheduleContainerKeyToString(groupKey: Int): String {
            return if (groupKey >= 0) {
                groupKey.toString(10)
            } else {
                val groupCode = -groupKey
                val code = groupCode / GROUP_CODE_OFFSET
                val subcode = groupCode % GROUP_CODE_OFFSET
                "$code-$subcode"
            }
        }
    }
}