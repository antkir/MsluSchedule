package by.ntnk.msluschedule.network.api.original

import by.ntnk.msluschedule.data.StudyGroupLesson
import by.ntnk.msluschedule.data.TeacherLesson
import by.ntnk.msluschedule.data.WeekdayWithStudyGroupLessons
import by.ntnk.msluschedule.data.WeekdayWithTeacherLessons
import by.ntnk.msluschedule.utils.AUTO_ID
import by.ntnk.msluschedule.utils.Days
import by.ntnk.msluschedule.utils.DEFAULT
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
        getWorkbookFontAt: (Int) -> HSSFFont
    ): List<WeekdayWithStudyGroupLessons> {
        val weekdays = ArrayList<WeekdayWithStudyGroupLessons>(Days.num())
        lateinit var weekdayData: WeekdayWithStudyGroupLessons
        lateinit var startTime: String
        lateinit var endTime: String
        lateinit var subject: String
        lateinit var type: String
        lateinit var teacher: String
        lateinit var classroom: String

        var prevTimePair = Pair(String.DEFAULT, String.DEFAULT)
        var isPreviousClassPhysEd = false

        for (cell in cells) {
            when (cell.columnIndex) {
                0 -> {
                    if (cell.stringCellValue.isNotEmpty()) {
                        weekdayData = WeekdayWithStudyGroupLessons(id = AUTO_ID, value = cell.stringCellValue)
                        weekdays.add(weekdayData)
                    }
                }

                1 -> {
                    val timePair = parseClassTimePair(cell.stringCellValue)
                    startTime = if (timePair.first != String.DEFAULT) timePair.first else prevTimePair.first
                    endTime = if (timePair.second != String.DEFAULT) timePair.second else prevTimePair.second

                    prevTimePair = Pair(startTime, endTime)
                }

                2 -> {
                    subject = String.DEFAULT
                    type = String.DEFAULT
                    teacher = String.DEFAULT

                    val valueRichString = cell.richStringCellValue as HSSFRichTextString
                    val substringLengths = getRichTextSubstringLengths(valueRichString)
                    val valueString = valueRichString.string

                    if (valueRichString.numFormattingRuns() == 0) {
                        subject = valueString.trim()
                        type = String.DEFAULT
                        teacher = String.DEFAULT
                    }

                    for (i in 0 until valueRichString.numFormattingRuns()) {
                        val startIdx = valueRichString.getIndexOfFormattingRun(i)
                        val fontIdx = valueRichString.getFontAtIndex(startIdx)
                        val font = getWorkbookFontAt(fontIdx.toInt())

                        if (font.bold) {
                            val nextStartIdx = substringLengths[i]
                            val typeTeacherString = valueString.substring(nextStartIdx, valueRichString.length()).trim()
                            val hasNewline = valueString.contains("\n")
                            val (classType, classTeacher) = parseTypeTeacherPair(typeTeacherString, hasNewline)

                            subject = valueString.substring(0, nextStartIdx).trim()
                            type = classType
                            teacher = classTeacher
                        }
                    }
                }

                3 -> {
                    classroom = cell.stringCellValue.replace(" ", String.DEFAULT)

                    if (startTime.isNotEmpty() || subject.isNotEmpty()) {
                        val classData =
                            StudyGroupLesson(
                                AUTO_ID,
                                subject,
                                type,
                                classroom,
                                startTime,
                                endTime,
                                teacher
                            )
                        if (sharedPreferencesRepository.isPhysEdClassHidden()) {
                            if (subject.contains("физ", ignoreCase = true) &&
                                subject.contains("культура", ignoreCase = true)
                            ) {
                                isPreviousClassPhysEd = true
                            } else if (!(isPreviousClassPhysEd && subject.isBlank())) {
                                isPreviousClassPhysEd = false
                                weekdayData.classes.add(classData)
                            }
                        } else {
                            weekdayData.classes.add(classData)
                        }
                    }
                }

                else -> Unit
            }
        }

        // Remove blank classes, if they are the last ones in the list
        for (weekday in weekdays) {
            val lastIndex = weekday.classes.lastIndex
            for (i in weekday.classes.indices) {
                if (weekday.classes[lastIndex - i].subject.isBlank()) {
                    weekday.classes.removeAt(lastIndex - i)
                } else {
                    break
                }
            }
        }

        if (weekdays.firstOrNull { it.value == Days.SUNDAY } == null) {
            weekdayData = WeekdayWithStudyGroupLessons(id = AUTO_ID, value = Days.SUNDAY)
            weekdays.add(weekdayData)
        }

        return weekdays
    }

    private fun parseClassTimePair(value: String): Pair<String, String> {
        return if (value.isNotEmpty()) {
            val values = value.split("-".toRegex())
            Pair(values[0].trim(), values[1].trim())
        } else {
            Pair(String.DEFAULT, String.DEFAULT)
        }
    }

    private fun parseTypeTeacherPair(value: String, hasNewline: Boolean): Pair<String, String> {
        return if (value.isNotEmpty()) {
            if (hasNewline) {
                if (value.contains("\n")) {
                    val values = value.split("\n".toRegex())
                    Pair(values[0].substringAfter(',').trim(), values[1].trim())
                } else {
                    Pair(String.DEFAULT, value.trim())
                }
            } else {
                Pair(value.substringAfter(',').trim(), String.DEFAULT)
            }
        } else {
            Pair(String.DEFAULT, String.DEFAULT)
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
        getWorkbookFontAt: (Int) -> HSSFFont
    ): List<WeekdayWithTeacherLessons> {
        val weekdays = listOf(
            WeekdayWithTeacherLessons(id = AUTO_ID, value = Days.MONDAY),
            WeekdayWithTeacherLessons(id = AUTO_ID, value = Days.TUESDAY),
            WeekdayWithTeacherLessons(id = AUTO_ID, value = Days.WEDNESDAY),
            WeekdayWithTeacherLessons(id = AUTO_ID, value = Days.THURSDAY),
            WeekdayWithTeacherLessons(id = AUTO_ID, value = Days.FRIDAY),
            WeekdayWithTeacherLessons(id = AUTO_ID, value = Days.SATURDAY),
            WeekdayWithTeacherLessons(id = AUTO_ID, value = Days.SUNDAY)
        )
        var startTime = String.DEFAULT
        var endTime = String.DEFAULT

        for (rowIndex in hssfRows.indices) {
            for (columnIndex in 0 until hssfRows[rowIndex].lastCellNum) {
                val hssfCell = hssfRows[rowIndex].getCell(columnIndex)
                if (hssfCell.columnIndex == 1 && hssfCell.stringCellValue.isNotEmpty()) {
                    val timePair = parseClassTimePair(hssfCell.stringCellValue)
                    startTime = timePair.first
                    endTime = timePair.second
                } else if (hssfCell.columnIndex in 2..7) {
                    val weekday = weekdays[hssfCell.columnIndex - 2].classes
                    if (hssfCell.stringCellValue.isNotEmpty()) {
                        val classData = parseCellToTeacherClass(hssfCell, startTime, endTime, getWorkbookFontAt)
                        weekday.add(classData)
                    }
                }
            }
        }

        return weekdays
    }

    private fun parseCellToTeacherClass(
        cell: Cell,
        startTime: String,
        endTime: String,
        getWorkbookFontAt: (Int) -> HSSFFont
    ): TeacherLesson {
        val valueRichString = cell.richStringCellValue as HSSFRichTextString
        val substringLengths = getRichTextSubstringLengths(valueRichString)
        val valueString = valueRichString.string

        if (valueRichString.numFormattingRuns() == 0) {
            return TeacherLesson(
                AUTO_ID,
                subject = valueString.trim(),
                type = String.DEFAULT,
                classroom = String.DEFAULT,
                startTime,
                endTime,
                faculty = String.DEFAULT,
                groups = String.DEFAULT
            )
        }

        for (i in 0 until valueRichString.numFormattingRuns()) {
            val startIdx = valueRichString.getIndexOfFormattingRun(i)
            val fontIdx = valueRichString.getFontAtIndex(startIdx)
            val font: HSSFFont = getWorkbookFontAt(fontIdx.toInt())

            if (font.bold) {
                val nextStartIdx = startIdx + substringLengths[i]
                val subject = valueString.substring(startIdx, nextStartIdx).trim()
                val classType = valueString.substring(nextStartIdx, valueRichString.length()).trim()

                val groupsFacultiesClassroom = valueString.substring(0, startIdx).trim()

                val groupsFaculties = with(groupsFacultiesClassroom.substringBeforeLast(')', String.DEFAULT)) {
                    if (this != String.DEFAULT) "$this)" else this
                }

                val groupsFacultiesSplitIdx = findOpeningParenthesisIndex(groupsFaculties)

                val groups = groupsFaculties.substring(0, groupsFacultiesSplitIdx).trim()

                val faculties = if (groupsFacultiesSplitIdx != groupsFaculties.length) {
                    groupsFaculties.substring(groupsFacultiesSplitIdx, groupsFaculties.length)
                        .drop(1)
                        .dropLast(1)
                        .trim()
                } else {
                    String.DEFAULT
                }

                val classroom = groupsFacultiesClassroom
                    .substringAfterLast(')')
                    .filter { it != ' ' }

                return TeacherLesson(AUTO_ID, subject, classType, classroom, startTime, endTime, faculties, groups)
            }
        }

        return TeacherLesson(
            AUTO_ID,
            subject = String.DEFAULT,
            type = String.DEFAULT,
            classroom = String.DEFAULT,
            startTime,
            endTime,
            faculty = String.DEFAULT,
            groups = String.DEFAULT
        )
    }

    private fun findOpeningParenthesisIndex(str: String): Int {
        if (str != String.DEFAULT) {
            var closingParenthesisCount = 0
            var openingParenthesisCount = 0
            for (index in str.indices.reversed()) {
                if (str[index] == ')') closingParenthesisCount++
                if (str[index] == '(') openingParenthesisCount++

                if (closingParenthesisCount != 0 && closingParenthesisCount == openingParenthesisCount) {
                    return index
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
