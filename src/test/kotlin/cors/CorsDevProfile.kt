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
@ActiveProfiles("dev") // Usamos el perfil "dev" con múltiples orígenes locales
class CorsDevProfile {
    @Autowired
    private lateinit var mockMvc: MockMvc // Permite simular peticiones HTTP al backend sin arrancar un servidor real

    // Inyectamos la propiedad de configuración para verificar que se lee correctamente
    @Value("\${cors.allowed-origins}")
    private lateinit var allowedOriginsProperty: String

    /**
     * Test que verifica que la configuración de orígenes permitidos
     * se carga correctamente desde application-dev.yml
     *
     * Propósito:
     * - Demostrar que diferentes perfiles pueden tener diferentes configuraciones
     * - Confirmar que los orígenes se externalizan vía propiedades
     * - Verificar que el perfil "dev" tiene su configuración específica
     */
    @Test
    fun `dev profile loads multiple allowed origins from properties`() {
        assertThat(allowedOriginsProperty)
            .contains("http://localhost:3000")
            .contains("http://127.0.0.1:3000")
            .describedAs("El perfil dev debe permitir múltiples orígenes locales")
    }

    /**
     * Test de preflight OPTIONS desde localhost:3000 en perfil dev
     *
     * Propósito:
     * - Verificar que el navegador puede hacer preflight desde localhost
     * - Confirmar todas las cabeceras CORS necesarias para desarrollo local
     * - Validar que max-age está configurado para optimizar preflights repetidos
     */
    @Test
    fun `preflight OPTIONS from localhost is allowed in dev profile`() {
        mockMvc
            .perform(
                options("/time")
                    .header("Origin", "http://localhost:3000")
                    .header("Access-Control-Request-Method", "GET"),
            ).andDo(print())
            .andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
            .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,OPTIONS,PUT,DELETE"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
            .andExpect(header().string("Access-Control-Max-Age", "3600"))
    }

    /**
     * Test de preflight OPTIONS desde 127.0.0.1:3000 en perfil dev
     *
     * Propósito:
     * - Verificar que ambos localhost y 127.0.0.1 funcionan en desarrollo
     * - Confirmar comportamiento consistente entre orígenes del mismo perfil
     */
    @Test
    fun `preflight OPTIONS from 127_0_0_1 is allowed in dev profile`() {
        mockMvc
            .perform(
                options("/time")
                    .header("Origin", "http://127.0.0.1:3000")
                    .header("Access-Control-Request-Method", "POST"),
            ).andDo(print())
            .andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:3000"))
            .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,OPTIONS,PUT,DELETE"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
            .andExpect(header().string("Access-Control-Max-Age", "3600"))
    }

    /**
     * Test que verifica que localhost:3000 está permitido en perfil dev
     */
    @Test
    fun `GET from localhost is allowed in dev profile`() {
        mockMvc
            .perform(
                get("/time")
                    .header("Origin", "http://localhost:3000"),
            ).andDo(print())
            .andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
    }

    /**
     * Test que verifica que 127.0.0.1:3000 también está permitido en perfil dev
     */
    @Test
    fun `GET from 127_0_0_1 is allowed in dev profile`() {
        mockMvc
            .perform(
                get("/time")
                    .header("Origin", "http://127.0.0.1:3000"),
            ).andDo(print())
            .andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:3000"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
    }

    /**
     * Test que verifica que orígenes no permitidos se rechazan incluso en dev
     *
     * Propósito:
     * - Confirmar que el perfil dev no es completamente abierto
     * - Validar que solo los orígenes configurados están permitidos
     * - Demostrar seguridad incluso en entorno de desarrollo
     */
    @Test
    fun `preflight OPTIONS from unauthorized origin is rejected in dev profile`() {
        mockMvc
            .perform(
                options("/time")
                    .header("Origin", "http://malicious.com")
                    .header("Access-Control-Request-Method", "GET"),
            ).andDo(print())
            .andExpect(status().isForbidden)
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
    }
}
