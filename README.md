[![Build Status](../../actions/workflows/CI.yml/badge.svg)](../../actions/workflows/CI.yml)

# Web Engineering 2024-2025 / Lab 2: Web Server

A minimal Spring Boot + Kotlin starter for Lab 2. Complete the tasks in `docs/GUIDE.md` to customize the error page, add a `/time` endpoint, and enable HTTP/2 + SSL.

## Tech stack
- Spring Boot 3.5.3
- Kotlin 2.2.10
- Java 21 (toolchain)
- Gradle Wrapper

## Prerequisites
- Java 21
- Git
- OpenSSL (for SSL steps in the GUIDE)

## Quick start
```bash
./gradlew clean build
./gradlew bootRun
# Default: http://localhost:8080
# After completing SSL task: https://127.0.0.1:8443
```

## Project structure
```
src
├── main
│   ├── kotlin
│   │   └── es
│   │       └── unizar
│   │           └── webeng
│   │               └── lab2                                # Application code
│   │                   ├── Application.kt                  # Main entry point annotated with @SpringBootApplication
│   │                   ├── TimeComponent.kt                # Component that provides time-related logic
│   │                   ├── config
│   │                   │   └── CorsConfig.kt               # Configures CORS settings (origins, methods, headers)
│   │                   └── logging
│   │                       ├── CorrelationIdFilter.kt      # Adds correlation IDs to logs/requests for tracing
│   │                       └── RequestLoggingFilter.kt     # Logs HTTP requests and responses in structured JSON
│   │ 
│   └── resources                                           # Configuration
│       ├── application-dev.yml                             # Configuration for the 'dev' Spring profile
│       ├── application-secrets.properties                  # Sensitive properties (e.g., KEY_STORE, SSL_PASSWORD)
│       ├── application.properties                          # Default Spring Boot configuration file
│       ├── application.yml                                 
│       ├── localhost.p12                                   # SSL certificate (for HTTPS)
│       ├── logback-spring.xml                              # Logback configuration (logging levels, appenders)
│       └── templates                                       # Thymeleaf templates
│           └── error.html                                  # Default error page
│
└── test
    ├── kotlin
    │   ├── ErrorPageIntegrationTest.kt                     # Integration test verifying custom error page handling
    │   ├── controller                                      # Tests related to time controller
    │   │   ├── TimeControllerMVCTests.kt
    │   │   └── TimeControllerUnitTests.kt
    │   ├── cors                                            # Tests for CORS configuration and profile-specific behavior
    │   │   ├── CorsDevProfile.kt
    │   │   └── CorsTest.kt
    │   └── logging                                         # Tests for logging configuration and behavior per profile
    │       ├── LoggingDevProfile.kt
    │       ├── LoggingTest.kt
    │       └── LoggingTestProfile.kt
    └── resources
        └── application-test.yml                            # Configuration file for the 'test' Spring profile

17 directories, 21 files
```

## Assignment tasks
See `docs/GUIDE.md` for detailed steps:
- Customize the whitelabel error page using Thymeleaf
- Implement `/time` endpoint (DTO + service + controller)
- Enable HTTP/2 and SSL with a PKCS12 keystore

## Code quality and formatting
```bash
./gradlew ktlintFormat ktlintCheck
# If Detekt is enabled in your environment:
# ./gradlew detekt
```

## Testing
```bash
./gradlew test
```

## Bonus opportunities

Be the first to complete **at least two** of the following tasks to earn a bonus:

1. **Provide JUnit-Based Validation of the Tasks**

    - **Description:** Develop comprehensive JUnit tests that validate each primary task end‑to‑end.
    - **JUnit Test:** Cover happy path and edge cases (404 custom error page content, `/time` JSON shape and headers). Add a property‑based test (e.g., jqwik) for date serialization constraints and verify both HTTP and HTTPS when SSL is enabled. Include a negative test demonstrating expected 4xx/5xx behavior. Add a deterministic time test by injecting a fixed `TimeProvider`. Target ≥80% line coverage for all controller/service classes.
    - **Goal:** Ensure that your implementation meets all requirements and behaves as expected under various scenarios.
    - **Benefit:** Demonstrates proficiency in test-driven development and reinforces code reliability.

2. **Use a Different Web Server Than Tomcat**

    - **Description:** Configure your Spring Boot application to use a web server other than the default Tomcat server, such as **Jetty** or **Undertow**.
    - **JUnit Test:** Add an integration test that boots with the alternative server, verifies it starts, keeps HTTP/2 working over SSL (check ALPN `h2`), and asserts the server implementation (e.g., via `ApplicationContext` or response headers) is Jetty/Undertow. Validate graceful shutdown and a bounded thread pool size via config. Capture and assert the server banner/header differs from Tomcat. Configure max header size and assert oversized requests are rejected with an appropriate status.
    - **Goal:** Gain experience with alternative embedded servers and understand their configurations.
    - **Benefit:** Enhances your ability to customize and optimize server environments for different use cases.

3. **Enable HTTP Response Compression**

    - **Description:** Configure your application to enable HTTP response compression (e.g., GZIP).
    - **JUnit Test:** Write tests with and without `Accept-Encoding: gzip` verifying `Content-Encoding: gzip` presence, and configure `server.compression` (min size and MIME types) so small responses are not compressed; assert both behaviors. Verify `Vary: Accept-Encoding` is present. Ensure streaming endpoints (e.g., `text/event-stream`) are excluded from compression. Assert `Content-Length` is omitted when compressed and present otherwise.
    - **Goal:** Improve application performance by reducing the size of data transmitted over the network.
    - **Benefit:** Demonstrates understanding of web performance optimization techniques.

4. **Implement Content Negotiation in `/time` Endpoint**

    - **Description:** Modify the `/time` endpoint to support content negotiation in JSON and XML (add Jackson XML module or equivalent). Add a vendor‑specific media type (e.g., `application/vnd.example.time+json`) for versioning, and define a sensible default when `Accept` is missing. Provide an explicit `produces` list on the controller method.
    - **JUnit Test:** Verify correct media type selection for JSON, XML, and vendor type; assert `406 Not Acceptable` for unsupported `Accept`. Validate the XML/JSON output against a schema (basic structural checks). Assert default content type when no `Accept` is provided and that `Content-Type` matches the negotiated media type.
    - **Goal:** Enhance the flexibility and interoperability of your API.
    - **Benefit:** Shows proficiency in building RESTful services that adhere to best practices.

5. **Enable CORS (Cross-Origin Resource Sharing) Support** ✅

    - **Description:** Configure CORS with explicit allowed origins, methods, headers, credentials, and max-age for the `/time` endpoint (or globally). Do not use wildcard `*` when credentials are allowed. Externalize allowed origins via configuration and vary them by profile.
    - **JUnit Test:** Test preflight `OPTIONS` and actual `GET` verifying `Access-Control-Allow-*` headers, including credentials behavior and that disallowed origins are rejected. Assert that configured origins are picked from properties at runtime and differ by profile.
    - **Goal:** Allow web applications from other domains to interact with your API.
    - **Benefit:** Essential for developing modern web applications that consume APIs from different origins.

6. **Replace `error.html` with `@ControllerAdvice`**

    - **Description:** Replace the static page with a global exception handler returning RFC 7807 `ProblemDetail` (or equivalent), mapping at least one custom domain exception with meaningful fields and localization via `MessageSource`. Handle `NoHandlerFoundException` and validation errors with distinct problem types; include `type` as a resolvable URI and `instance` as the request path.
    - **JUnit Test:** Verify status codes and JSON body shape for different exceptions; include a test that ensures a correlation/trace id is included (header and response) and localized `detail` based on `Accept-Language`. Assert `404` for unknown paths uses your problem format and that content negotiation returns XML when requested.
    - **Goal:** Achieve more flexible and centralized error handling.
    - **Benefit:** Enhances maintainability and provides a consistent error response structure.

7. **Implement Swagger/OpenAPI Documentation**

    - **Description:** Integrate Swagger UI into your application to auto-generate API documentation.
    - **JUnit Test:** Verify `/v3/api-docs` contains the `/time` schema and that Swagger UI is accessible; ensure the OpenAPI server URL reflects HTTPS when SSL is enabled. Add a security scheme (HTTP bearer) and an example for `/time` and assert they appear in the spec. Mark `/actuator` as hidden in the spec if exposed. Validate that vendor media type is documented with an example.
    - **Goal:** Improve API usability and provide a professional touch to your application.
    - **Benefit:** Improves API usability and provides a professional touch to your application.

8. **Implement Logging with SLF4J and Logback** ✅

    - **Description:** Configure structured JSON logging (e.g., Logstash encoder), propagate a correlation id (e.g., `X-Request-Id`) via MDC across async boundaries, and set different log levels per profile. Mask sensitive headers/fields (e.g., `Authorization`) in logs and include request duration.
    - **JUnit Test:** Use a test appender to verify logs contain correlation id, endpoint path, JSON structure, and duration; assert level differences across profiles and that sensitive fields are masked.
    - **Goal:** Demonstrate your ability to implement effective logging strategies for monitoring and debugging.
    - **Benefit:** Shows your ability to implement effective logging strategies for monitoring and debugging.

9. **Use Profiles to Manage Configurations**

    - **Description:** Utilize Spring Boot profiles to manage different configurations (e.g., development, production).
    - **JUnit Test:** Provide tests using `@ActiveProfiles` for `dev` and `prod` asserting profile-specific properties (e.g., port, logging level), that a profile-scoped bean is selected, and that sensitive properties are loaded from environment variables without being hardcoded. Use `DynamicPropertySource` to supply test‑time overrides and confirm profile-specific OpenAPI server URLs.
    - **Goal:** Learn how to manage application settings across different environments.
    - **Benefit:** Enhances your application's flexibility and readiness for deployment in different environments.

10. **Automated HTTP/2 Verification**

    - **Description:** Add integration tests that verify HTTP/2 over SSL is used and that disabling HTTP/2 falls back to HTTP/1.1. Enforce TLS 1.3 with restricted ciphers for the HTTPS profile and set a strict `server.connection-timeout`. Expose a readiness probe endpoint and ensure it remains accessible over HTTP/2.
    - **JUnit Test:** Use Spring Boot Test with JDK `HttpClient` to assert `HTTP_2` when `server.http2.enabled=true` and `HTTP_1_1` when disabled; assert status `200` for `/time` in both cases. Additionally, verify ALPN `h2` is negotiated and that TLS 1.2 connections are rejected when 1.3 is enforced. Assert requests exceeding the connection timeout fail as expected and that the readiness probe works over HTTP/2.
    - **Goal:** Ensure HTTP/2 is correctly configured and verifiable in CI, not only via manual `curl` tests.
    - **Benefit:** Increases confidence that production transport features are enabled and tested automatically.

Feel free to choose any of these bonus tasks to enrich your assignment.
Completing them will not only earn you a bonus but also deepen your understanding of best practices in web development.

If you need guidance on how to approach any of these tasks or have questions, don't hesitate to ask!