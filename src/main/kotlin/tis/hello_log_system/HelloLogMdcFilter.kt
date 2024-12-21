package tis.hello_log_system

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.UUID
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

private const val TRANSACTION_ID = "transaction.id"

@Component
class HelloLogMdcFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(HelloLogMdcFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            MDC.put(TRANSACTION_ID, UUID.randomUUID().toString())
            log.info("Setting transaction ID")
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(TRANSACTION_ID)
        }
    }
}
