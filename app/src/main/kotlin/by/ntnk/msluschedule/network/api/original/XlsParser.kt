package by.ntnk.msluschedule.network.api.original

import by.ntnk.msluschedule.data.StudyGroupLesson
import by.ntnk.msluschedule.data.TeacherLesson
import by.ntnk.msluschedule.data.WeekdayWithStudyGroupLessons
import by.ntnk.msluschedule.data.WeekdayWithTeacherLessons
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
        getWorkbookFontAt: (Short) -> HSSFFont
    ): List<WeekdayWithStudyGroupLessons> {
        val weekdays = ArrayList<WeekdayWithStudyGroupLessons>(Days.num())
        lateinit var weekdayDescriptor: WeekdayWithStudyGroupLessons
        lateinit var startTime: String
        lateinit var endTime: String
        lateinit var subject: String
        lateinit var type: String
        lateinit var teacher: String
        lateinit var classroom: String

        var prevTime: Pair<String, String> = Pair(String.DEFAULT, String.DEFAULT)
        var isPreviousClassPhysEd = false

        for (cell in cells) {
            when (cell.columnIndex) {
                0 -> {
                    if (cell.stringCellValue.isNotEmpty()) {
                        weekdayDescriptor = WeekdayWithStudyGroupLessons(weekday = cell.stringCellValue)
                        weekdays.add(weekdayDescriptor)
                    }
                }

                1 -> {
                    val time = getClassTimePair(cell.stringCellValue)
                    startTime = if (time.first != String.DEFAULT) time.first else prevTime.first
                    endTime = if (time.second != String.DEFAULT) time.second else prevTime.second

                    prevTime = Pair(startTime, endTime)
                }

                2 -> {
                    subject = String.DEFAULT
                    type = String.DEFAULT
                    teacher = String.DEFAULT

                    val richTextString = cell.richStringCellValue as HSSFRichTextString
                    val valueString = richTextString.string
                    val substringLengths = getRichTextSubstringLengths(richTextString)

                    if (richTextString.numFormattingRuns() == 0) {
                        subject = valueString.trim()
                        type = String.DEFAULT
                        teacher = String.DEFAULT
                    }

                    for (i in 0 until richTextString.numFormattingRuns()) {
                        val startIdx = richTextString.getIndexOfFormattingRun(i)
                        val fontIdx = richTextString.getFontAtIndex(startIdx)
                        val font: HSSFFont = getWorkbookFontAt(fontIdx)

                        if (font.bold) {
                            val nextStartIdx = substringLengths[i]
                            val typeTeacherString = valueString.substring(nextStartIdx, richTextString.length()).trim()
                            val hasNewline = valueString.contains("\n")
                            val (classType, classTeacher) = getTypeTeacherPair(typeTeacherString, hasNewline)

                            subject = valueString.substring(0, nextStartIdx).trim()
                            type = classType
                            teacher = classTeacher
                        }
                    }
                }

                3 -> {
                    classroom = cell.stringCellValue.replace(" ", String.DEFAULT)

                    if (startTime.isNotEmpty() || subject.isNotEmpty()) {
                        val classDescriptor = StudyGroupLesson(subject, type, teacher, classroom, startTime, endTime)
                        if (sharedPreferencesRepository.isPhysEdClassHidden()) {
                            if (subject.contains("физ", ignoreCase = true) &&
                                subject.contains("культура", ignoreCase = true)
                            ) {
                                isPreviousClassPhysEd = true
                            } else if (!(isPreviousClassPhysEd && subject.isBlank())) {
                                isPreviousClassPhysEd = false
                                weekdayDescriptor.classes.add(classDescriptor)
                            }
                        } else {
                            weekdayDescriptor.classes.add(classDescriptor)
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

        if (weekdays.firstOrNull { it.weekday == Days.SUNDAY } == null) {
            weekdayDescriptor = WeekdayWithStudyGroupLessons(weekday = Days.SUNDAY)
            weekdays.add(weekdayDescriptor)
        }

        return weekdays
    }

    private fun getClassTimePair(value: String): Pair<String, String> {
        return if (value.isNotEmpty()) {
            val values = value.split("-".toRegex())
            Pair(values[0].trim(), values[1].trim())
        } else {
            Pair(String.DEFAULT, String.DEFAULT)
        }
    }

    private fun getTypeTeacherPair(value: String, hasNewline: Boolean): Pair<String, String> {
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
        getWorkbookFontAt: (Short) -> HSSFFont
    ): List<WeekdayWithTeacherLessons> {
        val weekdays = listOf(
            WeekdayWithTeacherLessons(weekday = Days.MONDAY),
            WeekdayWithTeacherLessons(weekday = Days.TUESDAY),
            WeekdayWithTeacherLessons(weekday = Days.WEDNESDAY),
            WeekdayWithTeacherLessons(weekday = Days.THURSDAY),
            WeekdayWithTeacherLessons(weekday = Days.FRIDAY),
            WeekdayWithTeacherLessons(weekday = Days.SATURDAY),
            WeekdayWithTeacherLessons(weekday = Days.SUNDAY)
        )
        var startTime = String.DEFAULT
        var endTime = String.DEFAULT

        for (rowIndex in hssfRows.indices) {
            for (columnIndex in 0 until hssfRows[rowIndex].lastCellNum) {
                val hssfCell = hssfRows[rowIndex].getCell(columnIndex)
                if (hssfCell.columnIndex == 1 && hssfCell.stringCellValue.isNotEmpty()) {
                    val values = getClassTimePair(hssfCell.stringCellValue)
                    startTime = values.first
                    endTime = values.second
                } else if (hssfCell.columnIndex in 2..7) {
                    val weekday = weekdays[hssfCell.columnIndex - 2].classes
                    if (hssfCell.stringCellValue.isNotEmpty()) {
                        val classDescriptor = parseCellToTeacherClass(hssfCell, startTime, endTime, getWorkbookFontAt)
                        weekday.add(classDescriptor)
                    } else if (weekday.isNotEmpty() &&
                        dayHasMoreClasses(hssfRows, rowIndex, columnIndex)
                    ) {
                        val classDescriptor = TeacherLesson(startTime, endTime)
                        weekday.add(classDescriptor)
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
        getWorkbookFontAt: (Short) -> HSSFFont
    ): TeacherLesson {
        val richTextString = cell.richStringCellValue as HSSFRichTextString
        val valueString = richTextString.string
        val substringLengths = getRichTextSubstringLengths(richTextString)

        if (richTextString.numFormattingRuns() == 0) {
            return TeacherLesson(
                subject = valueString.trim(),
                faculty = String.DEFAULT,
                groups = String.DEFAULT,
                type = String.DEFAULT,
                classroom = String.DEFAULT,
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
                val classType = valueString.substring(nextStartIdx, richTextString.length()).trim()

                val groupsFacultiesClassroom = valueString.substring(0, startIdx).trim()

                val groupsFaculties = with(groupsFacultiesClassroom.substringBeforeLast(')')) {
                    if (groupsFacultiesClassroom.contains(')')) "$this)" else String.DEFAULT
                }

                val splitIdx = findOpeningParenthesisIndex(groupsFaculties)

                val groups = groupsFaculties.substring(0, splitIdx).trim()

                val faculties = if (splitIdx != groupsFaculties.length) {
                    groupsFaculties.substring(splitIdx, groupsFaculties.length)
                        .drop(1)
                        .dropLast(1)
                        .trim()
                } else {
                    String.DEFAULT
                }

                val classroom = groupsFacultiesClassroom
                    .substringAfterLast(')')
                    .filter { it != ' ' }

                return TeacherLesson(subject, faculties, groups, classType, classroom, startTime, endTime)
            }
        }

        return TeacherLesson(startTime, endTime)
    }

    private fun findOpeningParenthesisIndex(str: String): Int {
        if (str != String.DEFAULT) {
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

    private fun dayHasMoreClasses(hssfRows: List<Row>, rowIndex: Int, columnIndex: Int) =
        hssfRows
            .map { it.elementAt(columnIndex) }
            .slice(rowIndex until hssfRows.size)
            .any { it.stringCellValue != String.DEFAULT }
}
