package by.ntnk.msluschedule.network

import by.ntnk.msluschedule.TestTree
import by.ntnk.msluschedule.data.StudyGroupLesson
import by.ntnk.msluschedule.data.TeacherLesson
import by.ntnk.msluschedule.data.WeekdayWithStudyGroupLessons
import by.ntnk.msluschedule.data.WeekdayWithTeacherLessons
import by.ntnk.msluschedule.network.api.original.XlsParser
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
    fun `Check if saturday classes for a studygroup schedule are parsed correctly`() {
        // given
        val weekday = WeekdayWithStudyGroupLessons(weekday = Days.SATURDAY)
        val class1 = StudyGroupLesson(
            subject = "СОРО",
            type = "",
            teacher = "пр. Якушева  Н. В.",
            classroom = "",
            startTime = "8:15",
            endTime = "9:35"
        )
        val class2 = StudyGroupLesson(
            subject = "Практ. фон. (2)",
            type = "",
            teacher = "пр. Евдокимова В. В.",
            classroom = "В507",
            startTime = "9:45",
            endTime = "11:05"
        )
        val class3 = StudyGroupLesson(
            subject = "Спецмодуль: Логика / Этика",
            type = "",
            teacher = "",
            classroom = "",
            startTime = "11:15",
            endTime = "12:35"
        )
        weekday.classes.add(class1)
        weekday.classes.add(class2)
        weekday.classes.add(class3)
        val weekdays = 7
        val xlsBody = javaClass.getResource("/sample_studygroup_schedule.xls")!!.openStream()
        // when
        val observable = xlsParser.parseStudyGroupXls(xlsBody)
        // then
        observable.test().assertNoErrors()
        observable.test().assertValueCount(weekdays)
        observable.test().assertValueAt(weekdays - 2, weekday)
    }

    @Test
    fun `Check if saturday classes for a teacher schedule are parsed correctly`() {
        // given
        val weekday = WeekdayWithTeacherLessons(weekday = Days.SATURDAY)
        val class1 = TeacherLesson(
            subject = "ПППТР",
            faculty = "Англ.яз., Нем.яз., ФРЯ (исп.), ФРЯ (фр.)",
            groups = "521 а.исп, 522 а.фр, 523 а.нем, 524 а.нем, 502 н- кл, 505 и., 509 фр.",
            type = "ЛК",
            classroom = "",
            startTime = "8:15",
            endTime = "9:35"
        )
        val class2 = TeacherLesson(
            subject = "Стр. и содерж. рекл. текста",
            faculty = "Меж.ком.",
            groups = "316 мк_а_н, 317 мк_а-ис, 318 мк_а_ис",
            type = "ЛК",
            classroom = "",
            startTime = "9:45",
            endTime = "11:05"
        )
        val class3 = TeacherLesson(
            subject = "Введ. в литер-ние",
            faculty = "ФРЯ",
            groups = "201 и., 202 и., 203 и., 204 и., 205 и., 206 ф., 207 ф., 208 ф., 209 ф.",
            type = "ЛК",
            classroom = "Б402",
            startTime = "11:15",
            endTime = "12:35"
        )
        val class4 = TeacherLesson(
            subject = "Практ. фон. (2)",
            faculty = "Англ.яз.",
            groups = "301 а.исп",
            type = "ПЗ",
            classroom = "",
            startTime = "13:00",
            endTime = "14:20"
        )
        weekday.classes.add(class1)
        weekday.classes.add(class2)
        weekday.classes.add(class3)
        weekday.classes.add(class4)
        val weekdays = 7
        val xlsBody = javaClass.getResource("/sample_teacher_schedule.xls")!!.openStream()
        // when
        val observable = xlsParser.parseTeacherXls(xlsBody)
        // then
        observable.test().assertNoErrors()
        observable.test().assertValueCount(weekdays)
        observable.test().assertValueAt(weekdays - 2, weekday)
    }

    @Test
    fun `Check if classes with incomplete data for a studygroup schedule for 2020 are parsed correctly`() {
        // given
        val friday = WeekdayWithStudyGroupLessons(weekday = Days.FRIDAY)
        val fridayClass1 = StudyGroupLesson(
            subject = "Физ. культура, ЛК\nпреп. Пронский А.",
            type = "",
            teacher = "",
            classroom = "",
            startTime = "11:15",
            endTime = "12:35"
        )
        val fridayClass2 = StudyGroupLesson(
            subject = "Безоп. жизнедеят. чел-ка",
            type = "ЛК",
            teacher = "",
            classroom = "",
            startTime = "14:30",
            endTime = "15:50"
        )
        val fridayClass3 = StudyGroupLesson(
            subject = "Практ. фонетика (1 ин. яз.)",
            type = "",
            teacher = "ст.пр. Абламейко  В. С.",
            classroom = "А300",
            startTime = "16:00",
            endTime = "17:20"
        )
        val fridayClass4 = StudyGroupLesson(
            subject = "История: Ист. Беларуси",
            type = "",
            teacher = "доц. Цымбал  А. Г.",
            classroom = "А320",
            startTime = "17:30",
            endTime = "18:50"
        )
        friday.classes.add(fridayClass1)
        friday.classes.add(fridayClass2)
        friday.classes.add(fridayClass3)
        friday.classes.add(fridayClass4)

        val saturday = WeekdayWithStudyGroupLessons(weekday = Days.SATURDAY)
        val saturdayClass1 = StudyGroupLesson(
            subject = "Практ. фонетика (1 ин. яз.)",
            type = "ПЗ",
            teacher = "ст.пр. Абламейко  В. С.",
            classroom = "",
            startTime = "13:00",
            endTime = "14:20"
        )
        val saturdayClass2 = StudyGroupLesson(
            subject = "История: Ист. Беларуси",
            type = "",
            teacher = "доц. Цымбал  А. Г.",
            classroom = "А417",
            startTime = "14:30",
            endTime = "15:50"
        )
        val saturdayClass3 = StudyGroupLesson(
            subject = "Практ. фонетика (1 ин. яз.)",
            type = "",
            teacher = "",
            classroom = "",
            startTime = "16:00",
            endTime = "17:20"
        )
        saturday.classes.add(saturdayClass1)
        saturday.classes.add(saturdayClass2)
        saturday.classes.add(saturdayClass3)
        val weekdays = 7
        val xlsBody = javaClass.getResource("/sample_studygroup_schedule_2020.xls")!!.openStream()
        // when
        val observable = xlsParser.parseStudyGroupXls(xlsBody)
        // then
        observable.test().assertNoErrors()
        observable.test().assertValueCount(weekdays)
        observable.test().assertValueAt(weekdays - 3, friday)
        observable.test().assertValueAt(weekdays - 2, saturday)
    }

    @Test
    fun `Check if class with multiple parentheses for a teacher schedule is parsed correctly`() {
        // given
        val wednesday = WeekdayWithTeacherLessons(weekday = Days.WEDNESDAY)

        val wednesdayClass1 = TeacherLesson(
            subject = "Психология",
            faculty = "ФРЯ (фр.)",
            groups = "104/1 (фр.), 104/2 (фр.), 104/3 (фр.)",
            type = "СМ",
            classroom = "А417",
            startTime = "8:15",
            endTime = "9:35"
        )

        wednesday.classes.add(wednesdayClass1)

        val weekdays = 7
        val xlsBody = javaClass.getResource("/sample_teacher_schedule.xls")!!.openStream()
        // when
        val observable = xlsParser.parseTeacherXls(xlsBody)
        // then
        observable.test().assertNoErrors()
        observable.test().assertValueCount(weekdays)
        observable.map { it.classes[0] }.test().assertValueAt(weekdays - 5, wednesday.classes[0])
    }

    @Test
    fun `Check if classes with the same start time are parsed correctly`() {
        // given
        val tuesday = WeekdayWithStudyGroupLessons(weekday = Days.TUESDAY)

        val class1 = StudyGroupLesson(
            subject = "Просодия речи",
            type = "ПЗ",
            teacher = "пр. Репина К. П.",
            classroom = "",
            startTime = "8:15",
            endTime = "9:35"
        )

        val class2 = StudyGroupLesson(
            subject = "МСЛБДЗ",
            type = "ЛК",
            teacher = "проф. Зубов  А. В.",
            classroom = "",
            startTime = "9:45",
            endTime = "11:05"
        )

        val class3 = StudyGroupLesson(
            subject = "Практическая грамматика (2)",
            type = "",
            teacher = "пр. Евдокимова В. В.",
            classroom = "А507",
            startTime = "9:45",
            endTime = "11:05"
        )

        val class4 = StudyGroupLesson(
            subject = "СОРО",
            type = "",
            teacher = "пр. Якушева  Н. В.",
            classroom = "В512",
            startTime = "13:00",
            endTime = "14:20"
        )

        tuesday.classes.add(class1)
        tuesday.classes.add(class2)
        tuesday.classes.add(class3)
        tuesday.classes.add(class4)

        val weekdays = 7
        val xlsBody = javaClass.getResource("/sample_studygroup_schedule.xls")!!.openStream()
        // when
        val observable = xlsParser.parseStudyGroupXls(xlsBody)
        // then
        observable.test().assertNoErrors()
        observable.test().assertValueCount(weekdays)
        observable.test().assertValueAt(weekdays - 6, tuesday)
    }
}
