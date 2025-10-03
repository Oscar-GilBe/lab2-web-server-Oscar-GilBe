package es.unizar.webeng.lab2.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource

// Clase de pruebas para verificar el comportamiento del logging en perfil DEV
// Se centra en el nivel de log (INFO), propagación de correlationId y enmascaramiento de headers sensibles
@SpringBootTest
@TestPropertySource(properties = ["spring.profiles.active=dev"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LoggingDevProfile {
    // Filter que intercepta las requests y genera logs
    @Autowired
    private lateinit var filter: RequestLoggingFilter

    // Mapper para convertir logs JSON en mapas
    private val objectMapper = jacksonObjectMapper()

    // Logger y appender para capturar los logs generados en los tests
    private lateinit var logger: Logger
    private lateinit var appender: ListAppender<ILoggingEvent>

    @BeforeEach
    fun setup() {
        // Configuración de logger y appender antes de cada test
        logger = LoggerFactory.getLogger("es.unizar.webeng.lab2") as Logger
        appender = ListAppender<ILoggingEvent>()
        appender.start()
        logger.addAppender(appender)
    }

    @AfterEach
    fun tearDown() {
        // Limpiar el appender después de cada test
        logger.detachAppender(appender)
        appender.stop()
    }

    @Test
    fun `internal logs are INFO in dev profile`() {
        // Verificar que el nivel efectivo del logger sea INFO
        assertEquals(
            Level.INFO,
            logger.effectiveLevel,
            "El nivel efectivo del logger debería ser INFO en perfil dev",
        )

        val request = MockHttpServletRequest("GET", "/")
        val response = MockHttpServletResponse()
        val chain = FilterChain { _, res -> (res as HttpServletResponse).status = 200 }

        filter.doFilter(request, response, chain)

        // Verificar que hay logs
        assertTrue(
            appender.list.isNotEmpty(),
            "Debería haber logs capturados",
        )

        // Filtrar solo los logs generados por nuestro paquete
        val events = appender.list.filter { it.loggerName == "es.unizar.webeng.lab2" }
        assertTrue(
            events.isNotEmpty(),
            "Debería haber logs del filtro en es.unizar.webeng.lab2",
        )

        val event = events.first()
        assertEquals(
            Level.INFO,
            event.level,
            "El nivel del log debería ser INFO en perfil dev",
        )

        // Verificar estructura JSON del log
        val logMap: Map<String, Any> = objectMapper.readValue(event.formattedMessage)
        assertEquals("/", logMap["path"])
        assertNotNull(logMap["correlationId"])
        assertNotNull(logMap["duration_ms"])
    }

    @Test
    fun `logger level is INFO not DEBUG in dev profile`() {
        // Verificar explícitamente que DEBUG NO está habilitado y INFO SÍ lo está
        assertFalse(
            logger.isDebugEnabled,
            "El logger NO debería tener DEBUG habilitado en perfil dev",
        )
        assertTrue(
            logger.isInfoEnabled,
            "El logger debería tener INFO habilitado en perfil dev",
        )
    }

    @Test
    fun `correlation id is present in logs`() {
        // Testea que la cabecera X-Request-Id se propaga correctamente al log como correlationId
        val request = MockHttpServletRequest("GET", "/api/test")
        request.addHeader("X-Request-Id", "dev-correlation-456")
        val response = MockHttpServletResponse()
        val chain = FilterChain { _, res -> (res as HttpServletResponse).status = 200 }

        filter.doFilter(request, response, chain)

        val events = appender.list.filter { it.loggerName == "es.unizar.webeng.lab2" }
        assertTrue(events.isNotEmpty())

        val event = events.first()
        assertEquals(Level.INFO, event.level, "Debería ser INFO en dev")
        val logMap: Map<String, Any> = objectMapper.readValue(event.formattedMessage)
        assertEquals("dev-correlation-456", logMap["correlationId"])
    }

    @Test
    fun `sensitive headers are masked in logs`() {
        // Testea que los headers sensibles como Authorization y Cookie están enmascarados
        val request = MockHttpServletRequest("POST", "/test")
        request.addHeader("Authorization", "Bearer secret-token-dev")
        request.addHeader("Cookie", "session=xyz789")
        request.addHeader("Content-Type", "application/json")
        val response = MockHttpServletResponse()
        val chain = FilterChain { _, res -> (res as HttpServletResponse).status = 200 }

        filter.doFilter(request, response, chain)

        val events = appender.list.filter { it.loggerName == "es.unizar.webeng.lab2" }
        assertTrue(events.isNotEmpty(), "Debería haber eventos de log")

        val event = events.first()
        val logMap: Map<String, Any> = objectMapper.readValue(event.formattedMessage)

        // Extrae los headers del log para validarlos
        @Suppress("UNCHECKED_CAST")
        val headers = logMap["headers"] as Map<String, String>

        assertEquals(
            "*****",
            headers["Authorization"],
            "Authorization header debería estar enmascarado",
        )
        assertEquals(
            "*****",
            headers["Cookie"],
            "Cookie header debería estar enmascarado",
        )
        assertEquals(
            "application/json",
            headers["Content-Type"],
            "Content-Type NO debería estar enmascarado",
        )
    }
}
