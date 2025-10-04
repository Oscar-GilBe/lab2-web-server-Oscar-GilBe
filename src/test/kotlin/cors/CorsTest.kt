package es.unizar.webeng.lab2.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // Usamos el perfil "test" con orígenes específicos de prueba
class CorsTest {
    @Autowired
    private lateinit var mockMvc: MockMvc // Permite simular peticiones HTTP al backend sin arrancar un servidor real

    // Inyectamos la propiedad de configuración para verificar que se lee correctamente
    @Value("\${cors.allowed-origins}")
    private lateinit var allowedOriginsProperty: String

    /**
     * Test que verifica que la configuración de orígenes permitidos
     * se carga correctamente desde application-test.yml
     *
     * Propósito:
     * - Confirmar que los orígenes se externalizan vía propiedades
     * - Verificar que el perfil "test" tiene su configuración específica
     */
    @Test
    fun `configuration loads allowed origins from properties for test profile`() {
        // Verificamos que la propiedad cargada coincide con lo esperado en application-test.yml
        assertThat(allowedOriginsProperty)
            .isEqualTo("http://testclient.local")
            .describedAs("Los orígenes permitidos deben cargarse desde application-test.yml")
    }

    /**
     * Test que comprueba la respuesta a una petición CORS "preflight" (OPTIONS)
     * desde un origen permitido.
     *
     * Propósito:
     * - Verificar que Spring responde con status 200 OK a la petición OPTIONS
     * - Confirmar que devuelve todas las cabeceras CORS obligatorias:
     *   * Access-Control-Allow-Origin con el origen específico
     *   * Access-Control-Allow-Methods con la lista de métodos permitidos
     *   * Access-Control-Allow-Credentials = true
     *   * Access-Control-Max-Age para cachear la respuesta preflight
     */
    @Test
    fun `preflight OPTIONS from allowed origin returns correct headers`() {
        mockMvc
            .perform(
                options("/time")
                    .header("Origin", "http://testclient.local")
                    .header("Access-Control-Request-Method", "GET"),
            ).andDo(print())
            .andExpect(status().isOk)
            // Verificar que existe y coincide con el origen específico
            .andExpect(header().exists("Access-Control-Allow-Origin"))
            .andExpect(header().string("Access-Control-Allow-Origin", "http://testclient.local"))
            // Verificar métodos permitidos
            .andExpect(header().exists("Access-Control-Allow-Methods"))
            .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,OPTIONS,PUT,DELETE"))
            // Verificar que credentials está habilitado
            .andExpect(header().exists("Access-Control-Allow-Credentials"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
            // Verificar max-age para optimizar futuros preflights
            .andExpect(header().exists("Access-Control-Max-Age"))
            .andExpect(header().string("Access-Control-Max-Age", "3600"))
    }

    /**
     * Test que comprueba que una petición GET real desde un origen permitido
     * devuelve las cabeceras CORS correctas.
     *
     * Propósito:
     * - Simular una petición real después del preflight exitoso
     * - Verificar que la respuesta incluye:
     *   * Access-Control-Allow-Origin con el origen específico
     *   * Access-Control-Allow-Credentials = true
     * - Confirmar que el endpoint /time responde correctamente (200 OK)
     */
    @Test
    fun `GET from allowed origin returns correct headers and credentials enabled`() {
        mockMvc
            .perform(
                get("/time")
                    .header("Origin", "http://testclient.local"),
            ).andDo(print())
            .andExpect(status().isOk)
            // Verificar origen específico
            .andExpect(header().exists("Access-Control-Allow-Origin"))
            .andExpect(header().string("Access-Control-Allow-Origin", "http://testclient.local"))
            // Verificar que credentials sigue habilitado en peticiones reales
            .andExpect(header().exists("Access-Control-Allow-Credentials"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
    }

    /**
     * Test que comprueba que una petición GET desde un origen NO permitido
     * es rechazada correctamente por la configuración CORS.
     *
     * Propósito:
     * - Simular un intento de acceso desde un origen no configurado
     * - Verificar que el servidor responde con 403 Forbidden
     * - Confirmar que NO se devuelven cabeceras CORS (Access-Control-Allow-Origin)
     * - Validar que la seguridad CORS funciona como protección
     */
    @Test
    fun `GET from disallowed origin is rejected with no CORS headers`() {
        mockMvc
            .perform(
                get("/time")
                    .header("Origin", "http://malicious.com"),
            ).andDo(print())
            // El servidor rechaza la petición
            .andExpect(status().isForbidden)
            // No se devuelven cabeceras CORS para orígenes no permitidos
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
            .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"))
    }

    /**
     * Test que verifica que el preflight también rechaza orígenes no permitidos
     *
     * Propósito:
     * - Confirmar que incluso el preflight OPTIONS bloquea orígenes no permitidos
     * - Verificar comportamiento consistente entre preflight y peticiones reales
     */
    @Test
    fun `preflight OPTIONS from disallowed origin is rejected`() {
        mockMvc
            .perform(
                options("/time")
                    .header("Origin", "http://evil.com")
                    .header("Access-Control-Request-Method", "GET"),
            ).andDo(print())
            .andExpect(status().isForbidden)
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
    }
}
