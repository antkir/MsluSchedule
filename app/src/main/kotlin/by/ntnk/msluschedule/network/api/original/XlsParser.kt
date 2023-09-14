package by.ntnk.msluschedule.network.api.original

import by.ntnk.msluschedule.data.StudyGroupLesson
import by.ntnk.msluschedule.data.TeacherLesson
import by.ntnk.msluschedule.data.WeekdayWithStudyGroupLessons
import by.ntnk.msluschedule.data.WeekdayWithTeacherLessons
import by.ntnk.msluschedule.utils.Days
import by.ntnk.msluschedule.utils.EMPTY_STRING
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import io.reactivex.Observable
import org.apache.poi.hssf.usermodel.HSSFFont
import org.apache.poi.hssf.usermodel.HSSFRichTextString
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.poifs.filesystem.POIFSFileSystem
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import java.io.InputStream
import javax.inject.Inject

class XlsParser @Inject constructor(private val sharedPreferencesRepository: SharedPreferencesRepository) {

    fun parseStudyGroupXls(xlsStream: InputStream): Observable<WeekdayWithStudyGroupLessons> {
        val poifsFileSystem = POIFSFileSystem(xlsStream)
        val hssfWorkbook = HSSFWorkbook(poifsFileSystem)
        val hssfSheet = hssfWorkbook.getSheetAt(0)

        return Observable.fromIterable(hssfSheet)
            .filter { row -> row.rowNum > 1 }
            .flatMap { Observable.fromIterable(it) }
            .toList()
            .flatMapObservable { cells ->
                Observable.fromIterable(parseCellsToStudyGroupWeekdays(cells, hssfWorkbook::getFontAt))
            }
    }

    private fun parseCellsToStudyGroupWeekdays(
        cells: List<Cell>,
        getWorkbookFontAt: (Short) -> HSSFFont
    ): List<WeekdayWithStudyGroupLessons> {
        val weekdaysWithLessons = ArrayList<WeekdayWithStudyGroupLessons>(Days.num())
        lateinit var weekday: WeekdayWithStudyGroupLessons
        lateinit var startTime: String
        lateinit var endTime: String
        lateinit var subject: String
        lateinit var type: String
        lateinit var teacher: String
        lateinit var classroom: String

        var prevTime: Pair<String, String> = Pair(EMPTY_STRING, EMPTY_STRING)
        var isPreviousLessonPhysEd = false

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
                    startTime = if (pair.first != EMPTY_STRING) pair.first else prevTime.first
                    endTime = if (pair.second != EMPTY_STRING) pair.second else prevTime.second

                    prevTime = Pair(startTime, endTime)
                }
                2 -> {
                    subject = EMPTY_STRING
                    type = EMPTY_STRING
                    teacher = EMPTY_STRING

                    val richTextString = cell.richStringCellValue as HSSFRichTextString
                    val valueString = richTextString.string
                    val substringLengths = getRichTextSubstringLengths(richTextString)

                    if (richTextString.numFormattingRuns() == 0) {
                        subject = valueString.trim()
                        type = EMPTY_STRING
                        teacher = EMPTY_STRING
                    }

                    for (i in 0 until richTextString.numFormattingRuns()) {
                        val startIdx = richTextString.getIndexOfFormattingRun(i)
                        val fontIdx = richTextString.getFontAtIndex(startIdx)
                        val font: HSSFFont = getWorkbookFontAt(fontIdx)

                        if (font.bold) {
                            val nextStartIdx = substringLengths[i]
                            val typeTeacherString = valueString.substring(nextStartIdx, richTextString.length()).trim()
                            val hasNewline = valueString.contains("\n")
                            val (lessonType, lessonTeacher) = getTypeTeacherPair(typeTeacherString, hasNewline)

                            subject = valueString.substring(0, nextStartIdx).trim()
                            type = lessonType
                            teacher = lessonTeacher
                        }
                    }
                }
                3 -> {
                    classroom = cell.stringCellValue.replace(" ", EMPTY_STRING)

                    if (startTime.isNotEmpty() || subject.isNotEmpty()) {
                        val lesson = StudyGroupLesson(subject, type, teacher, classroom, startTime, endTime)
                        if (sharedPreferencesRepository.isPhysEdClassHidden()) {
                            if (subject.contains("физ", ignoreCase = true) &&
                                subject.contains("культура", ignoreCase = true)
                            ) {
                                isPreviousLessonPhysEd = true
                            } else if (!(isPreviousLessonPhysEd && subject.isBlank())) {
                                isPreviousLessonPhysEd = false
                                weekday.lessons.add(lesson)
                            }
                        } else {
                            weekday.lessons.add(lesson)
                        }
                    }
                }
                else -> Unit
            }
        }

        // Remove empty lessons, if they are the last ones in the list
        for (weekdayWithLessons in weekdaysWithLessons) {
            val size = weekdayWithLessons.lessons.size
            for (i in weekdayWithLessons.lessons.indices) {
                if (weekdayWithLessons.lessons[size - 1 - i].subject.isBlank()) {
                    weekdayWithLessons.lessons.removeAt(size - 1 - i)
                } else {
                    break
                }
            }
        }

        if (weekdaysWithLessons.firstOrNull { it.weekday == Days.SUNDAY } == null) {
            weekday = WeekdayWithStudyGroupLessons(Days.SUNDAY)
            weekdaysWithLessons.add(weekday)
        }

        return weekdaysWithLessons
    }

    private fun getLessonTimePair(value: String): Pair<String, String> {
        return if (value.isNotEmpty()) {
            val values = value.split("-".toRegex())
            Pair(values[0].trim(), values[1].trim())
        } else {
            Pair(EMPTY_STRING, EMPTY_STRING)
        }
    }

    private fun getTypeTeacherPair(value: String, hasNewline: Boolean): Pair<String, String> {
        return if (value.isNotEmpty()) {
            if (hasNewline) {
                if (value.contains("\n")) {
                    val values = value.split("\n".toRegex())
                    Pair(values[0].substringAfter(',').trim(), values[1].trim())
                } else {
                    Pair(EMPTY_STRING, value.trim())
                }
            } else {
                Pair(value.substringAfter(',').trim(), EMPTY_STRING)
            }
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
                Observable.fromIterable(parseCellsToTeacherWeekdays(cells, hssfWorkbook::getFontAt))
            }
    }

    private fun parseCellsToTeacherWeekdays(
        hssfRows: List<Row>,
        getWorkbookFontAt: (Short) -> HSSFFont
    ): List<WeekdayWithTeacherLessons> {
        val weekdaysWithLessons: List<WeekdayWithTeacherLessons> = listOf(
            WeekdayWithTeacherLessons(Days.MONDAY),
            WeekdayWithTeacherLessons(Days.TUESDAY),
            WeekdayWithTeacherLessons(Days.WEDNESDAY),
            WeekdayWithTeacherLessons(Days.THURSDAY),
            WeekdayWithTeacherLessons(Days.FRIDAY),
            WeekdayWithTeacherLessons(Days.SATURDAY),
            WeekdayWithTeacherLessons(Days.SUNDAY)
        )
        var startTime = EMPTY_STRING
        var endTime = EMPTY_STRING

        for (rowIndex in hssfRows.indices) {
            for (columnIndex in 0 until hssfRows[rowIndex].lastCellNum) {
                val hssfCell = hssfRows[rowIndex].getCell(columnIndex)
                if (hssfCell.columnIndex == 1 && hssfCell.stringCellValue.isNotEmpty()) {
                    val values = getLessonTimePair(hssfCell.stringCellValue)
                    startTime = values.first
                    endTime = values.second
                } else if (hssfCell.columnIndex in 2..7) {
                    val weekdayWithLessons = weekdaysWithLessons[hssfCell.columnIndex - 2].lessons
                    if (hssfCell.stringCellValue.isNotEmpty()) {
                        val lessonEntity = parseCellToTeacherLesson(hssfCell, startTime, endTime, getWorkbookFontAt)
                        weekdayWithLessons.add(lessonEntity)
                    }
                }
            }
        }

        return weekdaysWithLessons
    }

    private fun parseCellToTeacherLesson(
        cell: Cell,
        startTime: String,
        endTime: String,
        getWorkbookFontAt: (Short) -> HSSFFont
    ): TeacherLesson {
        val richTextString = cell.richStringCellValue as HSSFRichTextString
        val valueString = richTextString.string
        val substringLengths = getRichTextSubstringLengths(richTextString)

        if (richTextString.numFormattingRuns() == 0) {
            return TeacherLesson(
                subject = valueString.trim(),
                faculty = EMPTY_STRING,
                groups = EMPTY_STRING,
                type = EMPTY_STRING,
                classroom = EMPTY_STRING,
                startTime,
                endTime
            )
        }

        for (i in 0 until richTextString.numFormattingRuns()) {
            val startIdx = richTextString.getIndexOfFormattingRun(i)
            val fontIdx = richTextString.getFontAtIndex(startIdx)
            val font: HSSFFont = getWorkbookFontAt(fontIdx)

            if (font.bold) {
                val nextStartIdx = startIdx + substringLengths[i]
                val subject = valueString.substring(startIdx, nextStartIdx).trim()
                val lessonType = valueString.substring(nextStartIdx, richTextString.length()).trim()

                val groupsFacultiesClassroom = valueString.substring(0, startIdx).trim()

                val groupsFaculties = with(groupsFacultiesClassroom.substringBeforeLast(')')) {
                    return@with if (groupsFacultiesClassroom.contains(')')) "$this)" else EMPTY_STRING
                }

                val splitIdx = findOpeningParenthesisIndex(groupsFaculties)

                val groups = groupsFaculties.substring(0, splitIdx).trim()

                val faculties = if (splitIdx != groupsFaculties.length) {
                    groupsFaculties.substring(splitIdx, groupsFaculties.length)
                        .drop(1)
                        .dropLast(1)
                        .trim()
                } else {
                    EMPTY_STRING
                }

                val classroom = groupsFacultiesClassroom
                    .substringAfterLast(')')
                    .filter { it != ' ' }

                return TeacherLesson(subject, faculties, groups, lessonType, classroom, startTime, endTime)
            }
        }

        return TeacherLesson(startTime, endTime)
    }

    private fun findOpeningParenthesisIndex(str: String): Int {
        if (str != EMPTY_STRING) {
            var closingParenthesisCnt = 0
            var openingParenthesisCnt = 0
            for (chIdx in str.indices) {
                val idx = str.lastIndex - chIdx
                if (str[idx] == ')') closingParenthesisCnt++
                if (str[idx] == '(') openingParenthesisCnt++

                if (closingParenthesisCnt != 0 && closingParenthesisCnt == openingParenthesisCnt) {
                    return idx
                }
            }
        }

        return str.length
    }

    private fun getRichTextSubstringLengths(richTextString: HSSFRichTextString): List<Int> {
        val substringLengths = mutableListOf<Int>()
        var prev = 0
        for (i in 1 until richTextString.numFormattingRuns()) {
            val startIdx = richTextString.getIndexOfFormattingRun(i)
            substringLengths.add(startIdx - prev)
            prev = startIdx
        }
        substringLengths.add(richTextString.length() - prev)
        return substringLengths
    }
}
