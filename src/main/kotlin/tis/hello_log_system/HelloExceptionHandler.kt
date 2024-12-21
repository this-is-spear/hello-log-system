package tis.hello_log_system

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class HelloExceptionHandler {
    private val log = LoggerFactory.getLogger(HelloExceptionHandler::class.java)
    @ExceptionHandler(RuntimeException::class)
    fun handleException(e: RuntimeException): String {
        log.warn("An exception occurred {}", e.message)
        return "Error: ${e.message}"
    }
}
