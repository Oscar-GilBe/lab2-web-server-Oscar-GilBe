package es.unizar.webeng.lab2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")  // usa application-test.yml
class ErrorPageIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Test
    fun `should return a custom error page with 404 status code`() {
        // Forzar Accept: text/html para que Spring renderice error.html
        val headers = HttpHeaders()
        headers.accept = listOf(MediaType.TEXT_HTML)
        val entity = HttpEntity<String>(headers)

        val response = restTemplate.exchange(
            "http://localhost:$port/recurso-inexistente",
            HttpMethod.GET,
            entity,
            String::class.java
        )

        // Comprobaciones
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(response.body).contains("<!DOCTYPE html>")
        assertThat(response.body).contains("<title>Error Page</title>")
        assertThat(response.body).contains("<h1>Oops! Something went wrong</h1>")
        assertThat(response.body).contains("<p>It appears either something went wrong or the page doesn't exist anymore.</p>")
        assertThat(response.body).contains("Date:</div>")
        assertThat(response.body).contains("Path:</div>")
        assertThat(response.body).contains("Error:</div>")
        assertThat(response.body).contains("404")
    }
}
