package es.unizar.webeng.lab2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

class TimeControllerUnitTests {
    private lateinit var controller: TimeController
    private lateinit var timeProvider: TimeProvider

    @BeforeEach
    fun setup() {
        timeProvider = mock()
        controller = TimeController(timeProvider)
    }

    @Test
    fun `should return TimeDTO with fixed time`() {
        val fixedNow = LocalDateTime.now()
        val fixedNowDto = fixedNow.toDTO()
        whenever(timeProvider.now()).thenReturn(fixedNow)

        val result = controller.time()

        assertThat(result).isInstanceOf(TimeDTO::class.java)
        assertThat(result).isEqualTo(fixedNowDto)
        assertThat(result.time).isEqualTo(fixedNow)
    }
}
