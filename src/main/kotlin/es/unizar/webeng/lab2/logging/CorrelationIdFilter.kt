package es.unizar.webeng.lab2.logging

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Filtro que asegura que cada request tenga un Correlation ID único.
 * Este ID se guarda en el MDC para ser usado en los logs y también se devuelve en el header de la respuesta.
 */
@Component
class CorrelationIdFilter : Filter {
    companion object {
        // Nombre del header HTTP que contiene el Correlation ID
        const val CORRELATION_ID_HEADER = "X-Request-Id"
    }

    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain,
    ) {
        // Convertimos a HttpServletRequest/Response
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        // Obtenemos el Correlation ID del header, si no existe se genera uno nuevo
        val correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER) ?: UUID.randomUUID().toString()

        // Guardamos el Correlation ID en el MDC para que los logs puedan usarlo
        MDC.put(CORRELATION_ID_HEADER, correlationId)

        // Añadimos el Correlation ID en la respuesta HTTP
        httpResponse.addHeader(CORRELATION_ID_HEADER, correlationId)

        try {
            // Continuamos con la cadena de filtros
            chain.doFilter(request, response)
        } finally {
            // Eliminamos el Correlation ID del MDC después de procesar la request
            MDC.remove(CORRELATION_ID_HEADER)
        }
    }
}
