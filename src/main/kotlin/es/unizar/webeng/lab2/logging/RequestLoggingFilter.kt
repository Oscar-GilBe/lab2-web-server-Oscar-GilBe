package es.unizar.webeng.lab2.logging

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID
import kotlin.system.measureTimeMillis

/**
 * Filtro que registra información de cada request y response en formato JSON.
 * - Mide el tiempo de procesamiento.
 * - Distingue logs internos de outbound según la URI.
 * - Enmascara headers sensibles.
 * - Usa el Correlation ID del MDC para rastrear requests.
 */
@Component
class RequestLoggingFilter : OncePerRequestFilter() {
    // Logger interno de la aplicación
    private val internalLogger: org.slf4j.Logger
        get() = LoggerFactory.getLogger("es.unizar.webeng.lab2")

    // Logger para logs outbound
    private val outboundLogger: org.slf4j.Logger
        get() = LoggerFactory.getLogger("outbound-logs")

    // Objeto para convertir mapas a JSON
    private val objectMapper = ObjectMapper()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        // Obtener o generar un correlation id
        val correlationId =
            MDC.get(CorrelationIdFilter.CORRELATION_ID_HEADER)
                ?: request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)
                ?: UUID.randomUUID().toString()
        MDC.put(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId)

        // Seleccionar logger según el path de la request
        val logger = selectLogger(request)

        try {
            // Medir duración de la request
            val duration =
                measureTimeMillis {
                    filterChain.doFilter(request, response)
                }

            // Crear mapa con información a loguear
            val logMap =
                mapOf(
                    "path" to request.requestURI,
                    "method" to request.method,
                    "status" to response.status,
                    "duration_ms" to duration,
                    "correlationId" to correlationId,
                    "headers" to maskSensitiveHeaders(request),
                )

            val logJson = objectMapper.writeValueAsString(logMap)

            // Logear según el nivel efectivo del logger
            // En perfil test (DEBUG): logea a DEBUG
            // En perfil dev/prod (INFO): logea a INFO
            if (logger.isDebugEnabled) {
                logger.debug(logJson)
            } else {
                logger.info(logJson)
            }
        } finally {
            // Limpiar el MDC
            MDC.remove(CorrelationIdFilter.CORRELATION_ID_HEADER)
        }
    }

    // Función para enmascarar headers sensibles
    private fun maskSensitiveHeaders(request: HttpServletRequest): Map<String, String> {
        val sensitiveHeaders = setOf("authorization", "cookie")
        val headers = mutableMapOf<String, String>()

        val headerNames = request.headerNames
        while (headerNames.hasMoreElements()) {
            val name = headerNames.nextElement()
            headers[name] =
                if (name.lowercase() in sensitiveHeaders) {
                    "*****" // Ocultar valor sensible
                } else {
                    request.getHeader(name) ?: ""
                }
        }

        return headers
    }

    // Función para decidir qué logger usar según el path
    private fun selectLogger(request: HttpServletRequest): org.slf4j.Logger =
        if (request.requestURI.startsWith("/external") || request.requestURI == "/time") {
            outboundLogger
        } else {
            internalLogger
        }
}
