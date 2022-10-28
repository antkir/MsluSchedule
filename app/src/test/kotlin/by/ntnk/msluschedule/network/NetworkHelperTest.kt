package by.ntnk.msluschedule.network

import by.ntnk.msluschedule.TestTree
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.EMPTY_STRING
import by.ntnk.msluschedule.utils.InvalidYearException
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.threeten.bp.LocalDate
import timber.log.Timber
import org.mockito.Mockito.`when` as whenever

class NetworkHelperTest {

    @Mock
    private lateinit var currentDate: CurrentDate
    private lateinit var networkHelper: NetworkHelper

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        networkHelper = NetworkHelper(currentDate)
        Timber.plant(TestTree())
    }

    @Test
    fun `Throws InvalidYearException() when main html page doesn't contain requested elements`() {
        // given
        val requestInfo = networkHelper.facultyRequestInfo
        val mediaType = MediaType.parse(EMPTY_STRING)
        val responseBody = ResponseBody.create(mediaType, EMPTY_STRING).string()
        // when
        val testObserver = networkHelper.parseDataFromHtmlBody(requestInfo, responseBody).test()
        // then
        testObserver.assertError(InvalidYearException::class.java)
    }

    @Test
    fun `Throws InvalidYearException() when device year is not present in values parsed from main html page`() {
        // given
        val requestInfo = networkHelper.facultyRequestInfo
        val htmlBody = javaClass.getResource("/main.html")!!.readText()
        val mediaType = MediaType.parse(EMPTY_STRING)
        val responseBody = ResponseBody.create(mediaType, htmlBody).string()
        val date = LocalDate.of(2000, 1, 1)
        whenever(currentDate.date).thenReturn(date)
        // when
        val testObserver = networkHelper.parseDataFromHtmlBody(requestInfo, responseBody).test()
        // then
        testObserver.assertError(InvalidYearException::class.java)
    }
}
