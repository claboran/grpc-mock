package de.laboranowitsch.poc.grpcmock

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GrpcMockApplication

fun main(args: Array<String>) {
    runApplication<GrpcMockApplication>(*args)
}
