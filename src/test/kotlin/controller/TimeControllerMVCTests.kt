package es.unizar.webeng.lab2

import com.fasterxml.jackson.databind.ObjectMapper
import es.unizar.webeng.lab2.config.CorsConfig
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(TimeController::class)
@Import(CorsConfig::class)
class TimeControllerMVCTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var timeProvider: TimeProvider

    private val objectMapper = ObjectMapper()

    @Test
    fun `should return server time matching fixed timestamp`() {
        // Timestamp fijo al inicio del test
        val fixedNow = LocalDateTime.now()
        whenever(timeProvider.now()).thenReturn(fixedNow)

        mockMvc
            .perform(get("/time"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect { result ->
                // parámetro de la lambda
                // Obtenemos el cuerpo de la respuesta HTTP como un string JSON
                val json = result.response.contentAsString

                // Parseamos el string JSON usando Jackson ObjectMapper
                // Esto nos devuelve un objeto JsonNode que podemos recorrer
                val jsonNode = objectMapper.readTree(json)

                // Extraemos el valor del campo "time" del JSON como string
                val actualTime = LocalDateTime.parse(jsonNode.get("time").asText())

                // Comparamos el LocalDateTime obtenido de la respuesta con
                // el timestamp fijo que definimos al inicio del test
                assertThat(actualTime, equalTo(fixedNow))
            }
    }
}
