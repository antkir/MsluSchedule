package by.ntnk.msluschedule.network

import by.ntnk.msluschedule.TestTree
import by.ntnk.msluschedule.data.StudyGroupLesson
import by.ntnk.msluschedule.data.TeacherLesson
import by.ntnk.msluschedule.data.WeekdayWithStudyGroupLessons
import by.ntnk.msluschedule.data.WeekdayWithTeacherLessons
import by.ntnk.msluschedule.utils.SATURDAY
import org.junit.Test

import org.junit.Before
import timber.log.Timber

class XlsParserTest {
    private lateinit var xlsParser: XlsParser

    @Before
    fun setUp() {
        xlsParser = XlsParser()
        Timber.plant(TestTree())
    }

    @Test
    fun `Check if saturday lessons for a studygroup schedule are parsed correctly`() {
        // given
        val weekdayWithStudyGroupLessons = WeekdayWithStudyGroupLessons(SATURDAY)
        val lesson1 = StudyGroupLesson(
                subject = "СОРО",
                teacher = "пр. Якушева  Н. В.",
                classroom = "",
                startTime = "8:15",
                endTime = "9:35"
        )
        val lesson2 = StudyGroupLesson(
                subject = "Практ. фон. (2)",
                teacher = "пр. Евдокимова В. В.",
                classroom = "В507",
                startTime = "9:45",
                endTime = "11:05"
        )
        val lesson3 = StudyGroupLesson(
                subject = "Спецмодуль: Логика / Этика",
                teacher = "",
                classroom = "",
                startTime = "11:15",
                endTime = "12:35"
        )
        weekdayWithStudyGroupLessons.lessons.add(lesson1)
        weekdayWithStudyGroupLessons.lessons.add(lesson2)
        weekdayWithStudyGroupLessons.lessons.add(lesson3)
        val weekdays = 6
        val xlsBody = this.javaClass.getResource("/sample_studygroup_schedule.xls").openStream()
        // when
        val observable = xlsParser.parseStudyGroupXls(xlsBody)
        // then
        observable.test().assertNoErrors()
        observable.test().assertValueCount(weekdays)
        observable.test().assertValueAt(weekdays - 1, weekdayWithStudyGroupLessons)
    }

    @Test
    fun `Check if saturday lessons for a teacher schedule are parsed correctly`() {
        // given
        val weekdayWithTeacherLessons = WeekdayWithTeacherLessons(SATURDAY)
        val lesson1 = TeacherLesson(
                subject = "Введ. в литер-ние",
                faculty = "ФРЯ",
                groups = "201 и., 202 и., 203 и., 204 и., 205 и., 206 ф., 207 ф., 208 ф., 209 ф.",
                type = "ЛК",
                classroom = "Б402",
                startTime = "11:15",
                endTime = "12:35"
        )
        val lesson2 = TeacherLesson(
                subject = "Практ. фон. (2)",
                faculty = "Англ.яз.",
                groups = "301 а.исп",
                type = "ПЗ",
                classroom = "",
                startTime = "13:00",
                endTime = "14:20"
        )
        weekdayWithTeacherLessons.lessons.add(lesson1)
        weekdayWithTeacherLessons.lessons.add(lesson2)
        val weekdays = 6
        val xlsBody = this.javaClass.getResource("/sample_teacher_schedule.xls").openStream()
        // when
        val observable = xlsParser.parseTeacherXls(xlsBody)
        // then
        observable.test().assertNoErrors()
        observable.test().assertValueCount(weekdays)
        observable.test().assertValueAt(weekdays - 1, weekdayWithTeacherLessons)
    }
}
