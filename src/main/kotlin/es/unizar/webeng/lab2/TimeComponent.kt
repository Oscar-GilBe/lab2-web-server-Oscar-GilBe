package es.unizar.webeng.lab2

import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

// Data Transfer Object (DTO)
data class TimeDTO(val time: LocalDateTime)

// Interface. This interface allows for different implementations of time retrieval
interface TimeProvider {
    fun now(): LocalDateTime
}

// Service
@Service
class TimeService : TimeProvider {
    override fun now(): LocalDateTime = LocalDateTime.now()
}

// Extension function
// In Kotlin allow to extend a class with new functionality without inheriting from it
fun LocalDateTime.toDTO(): TimeDTO = TimeDTO(time = this)

// REST Controller
@RestController
class TimeController(private val service: TimeProvider) {

    @GetMapping("/time")
    fun time(): TimeDTO = service.now().toDTO()
}
