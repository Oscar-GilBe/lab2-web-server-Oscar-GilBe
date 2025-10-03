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

// Clase de pruebas para verificar el comportamiento del logging en perfil TEST
// Se centra en el nivel de log DEBUG, propagación de correlationId y enmascaramiento de headers sensibles
@SpringBootTest
@TestPropertySource(properties = ["spring.profiles.active=test"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LoggingTestProfile {
    // Filter que intercepta las requests y genera logs
    @Autowired
    private lateinit var filter: RequestLoggingFilter

    // Mapper para convertir logs JSON en mapas
    private val objectMapper = jacksonObjectMapper()

    // Logger y appender para capturar los logs durante los tests
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
        // Limpiar appender después de cada test
        logger.detachAppender(appender)
        appender.stop()
    }

    @Test
    fun `internal logs are DEBUG in test profile`() {
        // Verificar que el nivel efectivo del logger sea DEBUG
        assertEquals(
            Level.DEBUG,
            logger.effectiveLevel,
            "El nivel efectivo del logger debería ser DEBUG en perfil test",
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

        val events = appender.list.filter { it.loggerName == "es.unizar.webeng.lab2" }
        assertTrue(
            events.isNotEmpty(),
            "Debería haber logs del filtro en es.unizar.webeng.lab2",
        )

        val event = events.first()
        assertEquals(
            Level.DEBUG,
            event.level,
            "El nivel del log debería ser DEBUG en perfil test",
        )

        val logMap: Map<String, Any> = objectMapper.readValue(event.formattedMessage)
        assertEquals("/", logMap["path"])
        assertNotNull(logMap["correlationId"])
        assertNotNull(logMap["duration_ms"])
    }

    @Test
    fun `logger level is DEBUG in test profile`() {
        // Verifica explícitamente que DEBUG e INFO están habilitados en perfil TEST
        assertTrue(
            logger.isDebugEnabled,
            "El logger debería tener DEBUG habilitado en perfil test",
        )
        assertTrue(
            logger.isInfoEnabled,
            "El logger debería tener INFO habilitado en perfil test",
        )
    }

    @Test
    fun `correlation id is present in logs`() {
        // Testea que la cabecera X-Request-Id se propaga correctamente al log como correlationId
        val request = MockHttpServletRequest("GET", "/test")
        request.addHeader("X-Request-Id", "test-correlation-123")
        val response = MockHttpServletResponse()
        val chain = FilterChain { _, res -> (res as HttpServletResponse).status = 200 }

        filter.doFilter(request, response, chain)

        val events = appender.list.filter { it.loggerName == "es.unizar.webeng.lab2" }
        assertTrue(events.isNotEmpty())

        val event = events.first()
        val logMap: Map<String, Any> = objectMapper.readValue(event.formattedMessage)
        assertEquals("test-correlation-123", logMap["correlationId"])
    }

    @Test
    fun `sensitive headers are masked in logs`() {
        // Testea que los headers sensibles como Authorization y Cookie están enmascarados en perfil TEST
        val request = MockHttpServletRequest("GET", "/test")
        request.addHeader("Authorization", "Bearer secret-token")
        request.addHeader("Cookie", "session=abc123")
        request.addHeader("User-Agent", "TestAgent/1.0")
        val response = MockHttpServletResponse()
        val chain = FilterChain { _, res -> (res as HttpServletResponse).status = 200 }

        filter.doFilter(request, response, chain)

        val events = appender.list.filter { it.loggerName == "es.unizar.webeng.lab2" }
        assertTrue(events.isNotEmpty())

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
            "TestAgent/1.0",
            headers["User-Agent"],
            "User-Agent NO debería estar enmascarado",
        )
    }
}
