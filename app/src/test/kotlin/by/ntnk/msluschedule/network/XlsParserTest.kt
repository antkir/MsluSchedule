package by.ntnk.msluschedule.network

import by.ntnk.msluschedule.TestTree
import by.ntnk.msluschedule.data.StudyGroupLesson
import by.ntnk.msluschedule.data.TeacherLesson
import by.ntnk.msluschedule.data.WeekdayWithStudyGroupLessons
import by.ntnk.msluschedule.data.WeekdayWithTeacherLessons
import by.ntnk.msluschedule.utils.Days
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import timber.log.Timber

class XlsParserTest {

    @Mock
    private lateinit var sharedPreferencesRepository: SharedPreferencesRepository
    private lateinit var xlsParser: XlsParser

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        xlsParser = XlsParser(sharedPreferencesRepository)
        Timber.plant(TestTree())
    }

    @Test
    fun `Check if saturday lessons for a studygroup schedule are parsed correctly`() {
        // given
        val weekdayWithStudyGroupLessons = WeekdayWithStudyGroupLessons(Days.SATURDAY)
        val lesson1 = StudyGroupLesson(
            subject = "СОРО",
            type = "",
            teacher = "пр. Якушева  Н. В.",
            classroom = "",
            startTime = "8:15",
            endTime = "9:35"
        )
        val lesson2 = StudyGroupLesson(
            subject = "Практ. фон. (2)",
            type = "",
            teacher = "пр. Евдокимова В. В.",
            classroom = "В507",
            startTime = "9:45",
            endTime = "11:05"
        )
        val lesson3 = StudyGroupLesson(
            subject = "Спецмодуль: Логика / Этика",
            type = "",
            teacher = "",
            classroom = "",
            startTime = "11:15",
            endTime = "12:35"
        )
        weekdayWithStudyGroupLessons.lessons.add(lesson1)
        weekdayWithStudyGroupLessons.lessons.add(lesson2)
        weekdayWithStudyGroupLessons.lessons.add(lesson3)
        val weekdays = 7
        val xlsBody = javaClass.getResource("/sample_studygroup_schedule.xls")!!.openStream()
        // when
        val observable = xlsParser.parseStudyGroupXls(xlsBody)
        // then
        observable.test().assertNoErrors()
        observable.test().assertValueCount(weekdays)
        observable.test().assertValueAt(weekdays - 2, weekdayWithStudyGroupLessons)
    }

    @Test
    fun `Check if saturday lessons for a teacher schedule are parsed correctly`() {
        // given
        val weekdayWithTeacherLessons = WeekdayWithTeacherLessons(Days.SATURDAY)
        val lesson1 = TeacherLesson(
            subject = "ПППТР",
            faculty = "Англ.яз., Нем.яз., ФРЯ (исп.), ФРЯ (фр.)",
            groups = "521 а.исп, 522 а.фр, 523 а.нем, 524 а.нем, 502 н- кл, 505 и., 509 фр.",
            type = "ЛК",
            classroom = "",
            startTime = "8:15",
            endTime = "9:35"
        )
        val lesson2 = TeacherLesson(
            subject = "Стр. и содерж. рекл. текста",
            faculty = "Меж.ком.",
            groups = "316 мк_а_н, 317 мк_а-ис, 318 мк_а_ис",
            type = "ЛК",
            classroom = "",
            startTime = "9:45",
            endTime = "11:05"
        )
        val lesson3 = TeacherLesson(
            subject = "Введ. в литер-ние",
            faculty = "ФРЯ",
            groups = "201 и., 202 и., 203 и., 204 и., 205 и., 206 ф., 207 ф., 208 ф., 209 ф.",
            type = "ЛК",
            classroom = "Б402",
            startTime = "11:15",
            endTime = "12:35"
        )
        val lesson4 = TeacherLesson(
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
        weekdayWithTeacherLessons.lessons.add(lesson3)
        weekdayWithTeacherLessons.lessons.add(lesson4)
        val weekdays = 7
        val xlsBody = javaClass.getResource("/sample_teacher_schedule.xls")!!.openStream()
        // when
        val observable = xlsParser.parseTeacherXls(xlsBody)
        // then
        observable.test().assertNoErrors()
        observable.test().assertValueCount(weekdays)
        observable.test().assertValueAt(weekdays - 2, weekdayWithTeacherLessons)
    }

    @Test
    fun `Check if lessons with incomplete data for a studygroup schedule for 2020 are parsed correctly`() {
        // given
        val fridayWithStudyGroupLessons = WeekdayWithStudyGroupLessons(Days.FRIDAY)
        val fridayLesson1 = StudyGroupLesson(
            subject = "Физ. культура, ЛК\nпреп. Пронский А.",
            type = "",
            teacher = "",
            classroom = "",
            startTime = "11:15",
            endTime = "12:35"
        )
        val fridayLesson2 = StudyGroupLesson(
            subject = "Безоп. жизнедеят. чел-ка",
            type = "ЛК",
            teacher = "",
            classroom = "",
            startTime = "14:30",
            endTime = "15:50"
        )
        val fridayLesson3 = StudyGroupLesson(
            subject = "Практ. фонетика (1 ин. яз.)",
            type = "",
            teacher = "ст.пр. Абламейко  В. С.",
            classroom = "А300",
            startTime = "16:00",
            endTime = "17:20"
        )
        val fridayLesson4 = StudyGroupLesson(
            subject = "История: Ист. Беларуси",
            type = "",
            teacher = "доц. Цымбал  А. Г.",
            classroom = "А320",
            startTime = "17:30",
            endTime = "18:50"
        )
        fridayWithStudyGroupLessons.lessons.add(fridayLesson1)
        fridayWithStudyGroupLessons.lessons.add(fridayLesson2)
        fridayWithStudyGroupLessons.lessons.add(fridayLesson3)
        fridayWithStudyGroupLessons.lessons.add(fridayLesson4)

        val saturdayWithStudyGroupLessons = WeekdayWithStudyGroupLessons(Days.SATURDAY)
        val saturdayLesson1 = StudyGroupLesson(
            subject = "Практ. фонетика (1 ин. яз.)",
            type = "ПЗ",
            teacher = "ст.пр. Абламейко  В. С.",
            classroom = "",
            startTime = "13:00",
            endTime = "14:20"
        )
        val saturdayLesson2 = StudyGroupLesson(
            subject = "История: Ист. Беларуси",
            type = "",
            teacher = "доц. Цымбал  А. Г.",
            classroom = "А417",
            startTime = "14:30",
            endTime = "15:50"
        )
        val saturdayLesson3 = StudyGroupLesson(
            subject = "Практ. фонетика (1 ин. яз.)",
            type = "",
            teacher = "",
            classroom = "",
            startTime = "16:00",
            endTime = "17:20"
        )
        saturdayWithStudyGroupLessons.lessons.add(saturdayLesson1)
        saturdayWithStudyGroupLessons.lessons.add(saturdayLesson2)
        saturdayWithStudyGroupLessons.lessons.add(saturdayLesson3)
        val weekdays = 7
        val xlsBody = javaClass.getResource("/sample_studygroup_schedule_2020.xls")!!.openStream()
        // when
        val observable = xlsParser.parseStudyGroupXls(xlsBody)
        // then
        observable.test().assertNoErrors()
        observable.test().assertValueCount(weekdays)
        observable.test().assertValueAt(weekdays - 3, fridayWithStudyGroupLessons)
        observable.test().assertValueAt(weekdays - 2, saturdayWithStudyGroupLessons)
    }

    @Test
    fun `Check if lesson with multiple parentheses for a teacher schedule is parsed correctly`() {
        // given
        val wednesdayWithTeacherLessons = WeekdayWithTeacherLessons(Days.WEDNESDAY)

        val wednesdayLesson1 = TeacherLesson(
            subject = "Психология",
            faculty = "ФРЯ (фр.)",
            groups = "104/1 (фр.), 104/2 (фр.), 104/3 (фр.)",
            type = "СМ",
            classroom = "А417",
            startTime = "8:15",
            endTime = "9:35"
        )

        wednesdayWithTeacherLessons.lessons.add(wednesdayLesson1)

        val weekdays = 7
        val xlsBody = javaClass.getResource("/sample_teacher_schedule.xls")!!.openStream()
        // when
        val observable = xlsParser.parseTeacherXls(xlsBody)
        // then
        observable.test().assertNoErrors()
        observable.test().assertValueCount(weekdays)
        observable.map { it.lessons[0] }.test().assertValueAt(weekdays - 5, wednesdayWithTeacherLessons.lessons[0])
    }

    @Test
    fun `Check if lessons with the same start time are parsed correctly`() {
        // given
        val tuesdayWithStudyGroupLessons = WeekdayWithStudyGroupLessons(Days.TUESDAY)

        val lesson1 = StudyGroupLesson(
            subject = "Просодия речи",
            type = "ПЗ",
            teacher = "пр. Репина К. П.",
            classroom = "",
            startTime = "8:15",
            endTime = "9:35"
        )

        val lesson2 = StudyGroupLesson(
            subject = "МСЛБДЗ",
            type = "ЛК",
            teacher = "проф. Зубов  А. В.",
            classroom = "",
            startTime = "9:45",
            endTime = "11:05"
        )

        val lesson3 = StudyGroupLesson(
            subject = "Практическая грамматика (2)",
            type = "",
            teacher = "пр. Евдокимова В. В.",
            classroom = "А507",
            startTime = "9:45",
            endTime = "11:05"
        )

        val lesson4 = StudyGroupLesson(
            subject = "СОРО",
            type = "",
            teacher = "пр. Якушева  Н. В.",
            classroom = "В512",
            startTime = "13:00",
            endTime = "14:20"
        )

        tuesdayWithStudyGroupLessons.lessons.add(lesson1)
        tuesdayWithStudyGroupLessons.lessons.add(lesson2)
        tuesdayWithStudyGroupLessons.lessons.add(lesson3)
        tuesdayWithStudyGroupLessons.lessons.add(lesson4)

        val weekdays = 7
        val xlsBody = javaClass.getResource("/sample_studygroup_schedule.xls")!!.openStream()
        // when
        val observable = xlsParser.parseStudyGroupXls(xlsBody)
        // then
        observable.test().assertNoErrors()
        observable.test().assertValueCount(weekdays)
        observable.test().assertValueAt(weekdays - 6, tuesdayWithStudyGroupLessons)
    }
}
