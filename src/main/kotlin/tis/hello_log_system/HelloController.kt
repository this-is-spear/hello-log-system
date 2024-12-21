package tis.hello_log_system

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HelloController {
    private val log = LoggerFactory.getLogger(HelloController::class.java)

    @GetMapping("/hello")
    fun hello(): String {
        log.info("Saying hello")
        return "Hello, World!"
    }

    @GetMapping("/error")
    fun error(): String {
        log.info("Throwing an error")
        throw RuntimeException("An error occurred")
    }
}
