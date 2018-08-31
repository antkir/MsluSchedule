package by.ntnk.msluschedule.network

import by.ntnk.msluschedule.data.StudyGroupLesson
import by.ntnk.msluschedule.data.TeacherLesson
import by.ntnk.msluschedule.data.WeekdayWithStudyGroupLessons
import by.ntnk.msluschedule.data.WeekdayWithTeacherLessons
import by.ntnk.msluschedule.utils.*
import io.reactivex.Observable
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.poifs.filesystem.POIFSFileSystem
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import java.io.InputStream
import javax.inject.Inject

class XlsParser @Inject constructor() {
    fun parseStudyGroupXls(xlsStream: InputStream): Observable<WeekdayWithStudyGroupLessons> {
        val poifsFileSystem = POIFSFileSystem(xlsStream)
        val hssfWorkbook = HSSFWorkbook(poifsFileSystem)
        val hssfSheet = hssfWorkbook.getSheetAt(0)

        return Observable.fromIterable(hssfSheet)
                .filter { row -> row.rowNum > 1 }
                .flatMap { Observable.fromIterable(it) }
                .toList()
                .flatMapObservable { cells ->
                    Observable.fromIterable(parseCellsToStudyGroupWeekdays(cells))
                }
    }

    private fun parseCellsToStudyGroupWeekdays(cells: List<Cell>): List<WeekdayWithStudyGroupLessons> {
        val weekdaysWithLessons = ArrayList<WeekdayWithStudyGroupLessons>()
        lateinit var weekday: WeekdayWithStudyGroupLessons
        lateinit var startTime: String
        lateinit var endTime: String
        lateinit var subject: String
        lateinit var teacher: String
        lateinit var classroom: String

        for (cell in cells) {
            when (cell.columnIndex) {
                0 -> {
                    if (cell.stringCellValue.isNotEmpty()) {
                        val weekdayValue = cell.stringCellValue
                        weekday = WeekdayWithStudyGroupLessons(weekdayValue)
                        weekdaysWithLessons.add(weekday)
                    }
                }
                1 -> {
                    val pair = getLessonTimePair(cell.stringCellValue)
                    startTime = pair.first
                    endTime = pair.second
                }
                2 -> {
                    val pair = getSubjectTeacherPair(cell.stringCellValue)
                    subject = pair.first
                    teacher = pair.second
                }
                3 -> {
                    classroom = cell.stringCellValue.replace(" ", EMPTY_STRING)

                    if (startTime.isNotEmpty()) {
                        val lesson = StudyGroupLesson(subject, teacher, classroom, startTime, endTime)
                        weekday.lessons.add(lesson)
                    }
                }
                else -> {
                }
            }
        }

        return weekdaysWithLessons
    }

    private fun getLessonTimePair(value: String): Pair<String, String> {
        return if (value.isNotEmpty()) {
            val values = value.split("-".toRegex())
            Pair(values[0], values[1])
        } else {
            Pair(EMPTY_STRING, EMPTY_STRING)
        }
    }

    private fun getSubjectTeacherPair(value: String): Pair<String, String> {
        return if (value.isNotEmpty()) {
            val values = value.split("\n".toRegex())
            var teacher = if (values.size > 1) values[1] else EMPTY_STRING
            teacher = teacher.dropLastWhile { it.isWhitespace() }
            Pair(values[0], teacher)
        } else {
            Pair(EMPTY_STRING, EMPTY_STRING)
        }
    }

    fun parseTeacherXls(xlsStream: InputStream): Observable<WeekdayWithTeacherLessons> {
        val poifsFileSystem = POIFSFileSystem(xlsStream)
        val hssfWorkbook = HSSFWorkbook(poifsFileSystem)
        val hssfSheet = hssfWorkbook.getSheetAt(0)

        return Observable.fromIterable(hssfSheet)
                .filter { row -> row.rowNum > 2 }
                .toList()
                .flatMapObservable { cells ->
                    Observable.fromIterable(parseCellsToTeacherWeekdays(cells))
                }
    }

    private fun parseCellsToTeacherWeekdays(hssfRows: List<Row>): List<WeekdayWithTeacherLessons> {
        val weekdaysWithLessons: List<WeekdayWithTeacherLessons> = listOf(
                WeekdayWithTeacherLessons(MONDAY),
                WeekdayWithTeacherLessons(TUESDAY),
                WeekdayWithTeacherLessons(WEDNESDAY),
                WeekdayWithTeacherLessons(THURSDAY),
                WeekdayWithTeacherLessons(FRIDAY),
                WeekdayWithTeacherLessons(SATURDAY),
                WeekdayWithTeacherLessons(SUNDAY)
        )
        var startTime = EMPTY_STRING
        var endTime = EMPTY_STRING

        for (rowIndex in hssfRows.indices) {
            for (columnIndex in 0 until hssfRows[rowIndex].count()) {
                val hssfCell = hssfRows[rowIndex].elementAt(columnIndex)
                if (hssfCell.columnIndex == 1 && hssfCell.stringCellValue.isNotEmpty()) {
                    val values = getLessonTimePair(hssfCell.stringCellValue)
                    startTime = values.first
                    endTime = values.second
                } else if (hssfCell.columnIndex in 2..7) {
                    val weekdayWithLessons = weekdaysWithLessons[hssfCell.columnIndex - 2].lessons
                    if (hssfCell.stringCellValue.isNotEmpty()) {
                        val lessonEntity = parseCellToTeacherLesson(hssfCell, startTime, endTime)
                        weekdayWithLessons.add(lessonEntity)
                    } else if (weekdayWithLessons.isNotEmpty() &&
                            dayHasMoreLessons(hssfRows, rowIndex, columnIndex)) {
                        val lessonEntity = TeacherLesson(startTime, endTime)
                        weekdayWithLessons.add(lessonEntity)
                    }
                }
            }
        }

        return weekdaysWithLessons
    }

    private fun parseCellToTeacherLesson(cell: Cell, startTime: String, endTime: String): TeacherLesson {
        val fullString = cell.stringCellValue.split("\\(".toRegex(), 2)

        val groups = fullString[0].dropLastWhile { it.isWhitespace() }

        val facultiesClassroomSubjectLessontype = fullString[1].split(" {2}".toRegex())

        val faculties = facultiesClassroomSubjectLessontype[0].substringBeforeLast(")")

        val classroom = facultiesClassroomSubjectLessontype[0]
                .substringAfterLast(")")
                .replace(" ", EMPTY_STRING)

        val subjectLessontype = facultiesClassroomSubjectLessontype[1].split(" ".toRegex())

        var subject = EMPTY_STRING
        var lessonType = EMPTY_STRING
        for (i in subjectLessontype.indices) {
            if (i == subjectLessontype.size - 1) {
                lessonType = subjectLessontype[i]
            } else {
                subject += (subjectLessontype[i] + " ")
            }
        }
        subject = subject.dropLastWhile { it.isWhitespace() }

        return TeacherLesson(subject, faculties, groups, lessonType, classroom, startTime, endTime)
    }

    private fun dayHasMoreLessons(hssfRows: List<Row>, rowIndex: Int, columnIndex: Int) =
            hssfRows
                    .map { it.elementAt(columnIndex) }
                    .slice(rowIndex until hssfRows.size)
                    .any { it.stringCellValue != EMPTY_STRING }
}
