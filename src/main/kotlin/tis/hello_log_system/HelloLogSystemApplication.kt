package tis.hello_log_system

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HelloLogSystemApplication

fun main(args: Array<String>) {
	runApplication<HelloLogSystemApplication>(*args)
}
