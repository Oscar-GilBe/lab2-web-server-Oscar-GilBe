package es.unizar.webeng.lab2.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class CorsConfig(
    // Inyecta una propiedad (o usa por defecto localhost:3000)
    @Value("\${cors.allowed-origins:http://localhost:3000}") private val allowedOrigins: String,
) {
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()

        // Configurar orígenes permitidos
        val origins = allowedOrigins.split(",").map { it.trim() }
        config.allowedOrigins = origins

        // Métodos HTTP permitidos
        config.allowedMethods = listOf("GET", "POST", "OPTIONS", "PUT", "DELETE")

        // Headers permitidos en las peticiones
        config.allowedHeaders = listOf("Authorization", "Content-Type", "X-Request-Id", "Cookie", "User-Agent")

        // Headers expuestos al cliente (los que el navegador puede ver)
        config.exposedHeaders = listOf(
            "Content-Type",
            "X-Request-Id",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials",
            "Access-Control-Allow-Methods"
        )

        // Permitir credenciales (cookies, cabeceras de autenticación, etc.)
        config.allowCredentials = true

        // Tiempo de caché para la preflight request (en segundos)
        config.maxAge = 3600L // 1 hora

        // Aplica esta configuración CORS a todas las rutas de la aplicación
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)

        return source
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // Activa CORS y usa la configuración definida arriba
            .cors { corsConfigurer ->
                corsConfigurer.configurationSource(corsConfigurationSource())
            }
            // Desactiva la protección CSRF (no usamos cookies de sesión)
            .csrf { csrfConfigurer ->
                csrfConfigurer.disable()
            }
            // Define las reglas de autorización para las peticiones HTTP
            .authorizeHttpRequests { authz ->
                authz
                    // Permite todas las peticiones OPTIONS (necesarias para preflight de CORS)
                    .requestMatchers("OPTIONS", "/**")
                    .permitAll()
                    .anyRequest()
                    .permitAll()
            }

        return http.build()
    }
}
