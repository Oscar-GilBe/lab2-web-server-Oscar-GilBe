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
import java.io.File

// Clase de pruebas para verificar el correcto funcionamiento del logging en la aplicación
// Se prueba el logging interno y outbound, la propagación del correlationId, el masking de headers
// y la generación de ficheros de log en formato JSON.
@SpringBootTest
@TestPropertySource(properties = ["spring.profiles.active=test"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LoggingTest {
    // Filter que intercepta las requests y realiza logging
    @Autowired
    private lateinit var filter: RequestLoggingFilter

    // Mapper para convertir logs JSON en mapas
    private val objectMapper = jacksonObjectMapper()

    // Loggers para pruebas internas y outbound
    private lateinit var internalLogger: Logger
    private lateinit var outboundLogger: Logger

    // Appenders para capturar los logs durante los tests
    private lateinit var internalAppender: ListAppender<ILoggingEvent>
    private lateinit var outboundAppender: ListAppender<ILoggingEvent>

    @BeforeEach
    fun setup() {
        // Configuración de loggers y appenders antes de cada test
        internalLogger = LoggerFactory.getLogger("es.unizar.webeng.lab2") as Logger
        outboundLogger = LoggerFactory.getLogger("outbound-logs") as Logger

        internalAppender = ListAppender<ILoggingEvent>()
        outboundAppender = ListAppender<ILoggingEvent>()

        internalAppender.start()
        outboundAppender.start()

        internalLogger.addAppender(internalAppender)
        outboundLogger.addAppender(outboundAppender)
    }

    @AfterEach
    fun tearDown() {
        // Limpiar appenders después de cada test
        internalLogger.detachAppender(internalAppender)
        outboundLogger.detachAppender(outboundAppender)
        internalAppender.stop()
        outboundAppender.stop()
    }

    @Test
    fun `logs should contain correlation id, path, JSON structure and masked headers`() {
        // Testea que los logs contienen correlationId, path, JSON válido y headers enmascarados
        val request = MockHttpServletRequest("GET", "/test")
        request.addHeader("Authorization", "Bearer secret")
        val response = MockHttpServletResponse()

        // Definimos un FilterChain simulado que establece el estado 200 en la respuesta
        val chain =
            FilterChain { _, res ->
                (res as HttpServletResponse).status = 200
            }

        // Ejecutamos el filtro pasando la request, response y el chain simulado
        filter.doFilter(request, response, chain)

        // Obtenemos los logs internos generados durante la ejecución del filtro
        val logs = internalAppender.list.map { it.formattedMessage }
        assertTrue(logs.isNotEmpty(), "No se ha registrado ningún log")

        // Convierte el primer log de JSON a un mapa para poder validar sus campos
        val logMap: Map<String, Any> = objectMapper.readValue(logs[0])
        assertEquals("/test", logMap["path"])
        assertTrue(logMap.containsKey("duration_ms"))
        assertNotNull(logMap["correlationId"])

        // Extrae los headers del log para validarlos
        @Suppress("UNCHECKED_CAST")
        val headers = logMap["headers"] as Map<String, String>
        assertEquals("*****", headers["Authorization"])
    }

    @Test
    fun `internal and outbound logs are recorded correctly`() {
        // Testea que tanto logs internos como outbound se registran correctamente
        val chain =
            FilterChain { _, res ->
                (res as HttpServletResponse).status = 200
            }

        // --- Test ruta interna ---
        val internalRequest = MockHttpServletRequest("GET", "/")
        internalRequest.addHeader("Authorization", "Bearer secret")
        val internalResponse = MockHttpServletResponse()

        filter.doFilter(internalRequest, internalResponse, chain)

        val internalLogs = internalAppender.list.map { it.formattedMessage }
        assertTrue(internalLogs.isNotEmpty(), "No se registró log interno")

        val internalLogMap: Map<String, Any> = objectMapper.readValue(internalLogs[0])
        assertEquals("/", internalLogMap["path"])
        assertNotNull(internalLogMap["correlationId"])
        assertTrue(internalLogMap.containsKey("duration_ms"))

        @Suppress("UNCHECKED_CAST")
        val internalHeaders = internalLogMap["headers"] as Map<String, String>
        assertEquals("*****", internalHeaders["Authorization"])

        // --- Test ruta outbound (/time) ---
        val outboundRequest = MockHttpServletRequest("GET", "/time")
        outboundRequest.addHeader("Authorization", "Bearer secret")
        val outboundResponse = MockHttpServletResponse()

        filter.doFilter(outboundRequest, outboundResponse, chain)

        val outboundLogs = outboundAppender.list.map { it.formattedMessage }
        assertTrue(outboundLogs.isNotEmpty(), "No se registró log outbound")

        val outboundLogMap: Map<String, Any> = objectMapper.readValue(outboundLogs[0])
        assertEquals("/time", outboundLogMap["path"])
        assertNotNull(outboundLogMap["correlationId"])
        assertTrue(outboundLogMap.containsKey("duration_ms"))

        @Suppress("UNCHECKED_CAST")
        val outboundHeaders = outboundLogMap["headers"] as Map<String, String>
        assertEquals("*****", outboundHeaders["Authorization"])
    }

    @Test
    fun `internal logs contain correct JSON structure`() {
        // Verifica que los logs internos tienen la estructura JSON correcta y nivel DEBUG
        val request = MockHttpServletRequest("GET", "/")
        request.addHeader("Authorization", "Bearer secret")
        request.addHeader("User-Agent", "TestAgent")
        val response = MockHttpServletResponse()

        val chain =
            FilterChain { _, res ->
                (res as HttpServletResponse).status = 200
            }

        filter.doFilter(request, response, chain)

        assertTrue(internalAppender.list.isNotEmpty(), "No se registró log interno")
        val event = internalAppender.list[0]

        // Verifica que el nivel sea DEBUG (perfil test activo)
        assertEquals(Level.DEBUG, event.level)

        // JSON válido y contenido
        val logMap: Map<String, Any> = objectMapper.readValue(event.formattedMessage)
        assertEquals("/", logMap["path"])
        assertEquals("GET", logMap["method"])
        assertEquals(200, logMap["status"])
        assertTrue(logMap.containsKey("correlationId"))
        assertTrue(logMap.containsKey("duration_ms"))

        @Suppress("UNCHECKED_CAST")
        val headers = logMap["headers"] as Map<String, String>
        assertEquals("*****", headers["Authorization"])
        assertEquals("TestAgent", headers["User-Agent"])
    }

    @Test
    fun `outbound logs contain correct JSON structure`() {
        // Verifica que los logs outbound tienen la estructura JSON correcta y nivel DEBUG
        val request = MockHttpServletRequest("GET", "/time")
        request.addHeader("Authorization", "Bearer secret")
        request.addHeader("Content-Type", "application/json")
        val response = MockHttpServletResponse()

        val chain =
            FilterChain { _, res ->
                (res as HttpServletResponse).status = 200
            }

        filter.doFilter(request, response, chain)

        assertTrue(outboundAppender.list.isNotEmpty(), "No se registró log outbound")
        val event = outboundAppender.list[0]

        // Verifica nivel DEBUG en perfil test
        assertEquals(Level.DEBUG, event.level)

        // JSON válido y contenido
        val logMap: Map<String, Any> = objectMapper.readValue(event.formattedMessage)
        assertEquals("/time", logMap["path"])
        assertEquals("GET", logMap["method"])
        assertEquals(200, logMap["status"])
        assertTrue(logMap.containsKey("correlationId"))
        assertTrue(logMap.containsKey("duration_ms"))

        @Suppress("UNCHECKED_CAST")
        val headers = logMap["headers"] as Map<String, String>
        assertEquals("*****", headers["Authorization"])
        assertEquals("application/json", headers["Content-Type"])
    }

    @Test
    fun `logger is configured at DEBUG level in test profile`() {
        // Verifica que los loggers internos y outbound están configurados en DEBUG en perfil test
        assertTrue(
            internalLogger.isDebugEnabled,
            "El logger interno debería tener DEBUG habilitado en perfil test",
        )
        assertTrue(
            outboundLogger.isDebugEnabled,
            "El logger outbound debería tener DEBUG habilitado en perfil test",
        )

        assertEquals(
            Level.DEBUG,
            internalLogger.effectiveLevel,
            "El nivel efectivo debería ser DEBUG",
        )
        assertEquals(
            Level.DEBUG,
            outboundLogger.effectiveLevel,
            "El nivel efectivo debería ser DEBUG",
        )
    }

    @Test
    fun `log files are created and contain valid JSON`() {
        // Testea la creación de ficheros de log y que contengan JSON válido con los campos esperados
        val chain =
            FilterChain { _, res ->
                (res as HttpServletResponse).status = 200
            }

        // Generar un correlation ID único para este test
        val uniqueCorrelationId = "test-correlation-${System.currentTimeMillis()}"

        // --- Internal request ---
        val internalRequest = MockHttpServletRequest("GET", "/api/test")
        internalRequest.addHeader("Authorization", "Bearer secret")
        internalRequest.addHeader("X-Request-Id", uniqueCorrelationId)
        val internalResponse = MockHttpServletResponse()
        filter.doFilter(internalRequest, internalResponse, chain)

        // --- Outbound request ---
        val outboundRequest = MockHttpServletRequest("GET", "/time")
        outboundRequest.addHeader("Cookie", "session=abc123")
        outboundRequest.addHeader("X-Request-Id", "$uniqueCorrelationId-outbound")
        val outboundResponse = MockHttpServletResponse()
        filter.doFilter(outboundRequest, outboundResponse, chain)

        // Dar tiempo para que se escriban los logs
        Thread.sleep(200)

        // --- Verificación ficheros ---
        val internalLogFile = File("logs/internal.log")
        val outboundLogFile = File("logs/outbound.log")

        assertTrue(internalLogFile.exists(), "No se creó el fichero internal.log")
        assertTrue(outboundLogFile.exists(), "No se creó el fichero outbound.log")

        val internalContent = internalLogFile.readText()
        val outboundContent = outboundLogFile.readText()

        // Verificar que contienen JSON válido con los campos esperados
        assertTrue(
            internalContent.isNotEmpty(),
            "internal.log está vacío",
        )

        // Los campos aparecen escapados dentro del message field: \"path\"
        assertTrue(
            internalContent.contains("path") || internalContent.contains("\\\"path\\\""),
            "internal.log no contiene el campo 'path'",
        )
        assertTrue(
            internalContent.contains("correlationId") || internalContent.contains("\\\"correlationId\\\""),
            "internal.log no contiene 'correlationId'",
        )
        assertTrue(
            internalContent.contains("duration_ms") || internalContent.contains("\\\"duration_ms\\\""),
            "internal.log no contiene 'duration_ms'",
        )

        // Verificar que nuestro log específico está presente
        assertTrue(
            internalContent.contains(uniqueCorrelationId),
            "internal.log no contiene el correlation ID único de este test: $uniqueCorrelationId",
        )

        assertTrue(
            outboundContent.isNotEmpty(),
            "outbound.log está vacío",
        )

        // Los campos aparecen escapados dentro del message field: \"path\"
        assertTrue(
            outboundContent.contains("path") || outboundContent.contains("\\\"path\\\""),
            "outbound.log no contiene el campo 'path'",
        )
        assertTrue(
            outboundContent.contains("correlationId") || outboundContent.contains("\\\"correlationId\\\""),
            "outbound.log no contiene 'correlationId'",
        )
        assertTrue(
            outboundContent.contains("duration_ms") || outboundContent.contains("\\\"duration_ms\\\""),
            "outbound.log no contiene 'duration_ms'",
        )

        // Verificar que nuestro log específico está presente
        assertTrue(
            outboundContent.contains("$uniqueCorrelationId-outbound"),
            "outbound.log no contiene el correlation ID único de este test: $uniqueCorrelationId-outbound",
        )

        // Verificar que los logs contienen el campo X-Request-Id en el MDC (nivel superior del JSON)
        assertTrue(
            internalContent.contains("\"X-Request-Id\":\"$uniqueCorrelationId\""),
            "internal.log no contiene X-Request-Id en el MDC",
        )
        assertTrue(
            outboundContent.contains("\"X-Request-Id\":\"$uniqueCorrelationId-outbound\""),
            "outbound.log no contiene X-Request-Id en el MDC",
        )
    }

    @Test
    fun `multiple requests are logged separately with unique correlation ids`() {
        // Testea que múltiples requests generan logs separados con correlationId únicos
        val chain =
            FilterChain { _, res ->
                (res as HttpServletResponse).status = 200
            }

        // Primera request
        val request1 = MockHttpServletRequest("GET", "/test1")
        request1.addHeader("X-Request-Id", "correlation-001")
        filter.doFilter(request1, MockHttpServletResponse(), chain)

        // Segunda request
        val request2 = MockHttpServletRequest("POST", "/test2")
        request2.addHeader("X-Request-Id", "correlation-002")
        filter.doFilter(request2, MockHttpServletResponse(), chain)

        assertEquals(
            2,
            internalAppender.list.size,
            "Deberían haberse registrado 2 logs",
        )

        val log1: Map<String, Any> = objectMapper.readValue(internalAppender.list[0].formattedMessage)
        val log2: Map<String, Any> = objectMapper.readValue(internalAppender.list[1].formattedMessage)

        assertEquals("correlation-001", log1["correlationId"])
        assertEquals("/test1", log1["path"])
        assertEquals("GET", log1["method"])

        assertEquals("correlation-002", log2["correlationId"])
        assertEquals("/test2", log2["path"])
        assertEquals("POST", log2["method"])
    }
}
