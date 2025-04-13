package de.laboranowitsch.poc.grpcmock.backend

import de.laboranowitsch.poc.grpcmock.backend.controller.ComputationGrpcController
import de.laboranowitsch.poc.grpcmock.backend.repository.JobRepository
import de.laboranowitsch.poc.grpcmock.backend.service.ComputationService
import de.laboranowitsch.poc.grpcmock.protobuf.*
import kotlinx.coroutines.flow.Flow
import net.devh.boot.grpc.server.service.GrpcService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary

/**
 * Legacy implementation of the ComputationService gRPC service.
 * This class is kept for backward compatibility and delegates to the new modular implementation.
 * 
 * @deprecated Use ComputationGrpcController instead
 */
//@GrpcService
@Primary // Ensure this implementation takes precedence if both are active
class ComputationServiceMock(
    private val computationGrpcController: ComputationGrpcController
) : ComputationServiceGrpcKt.ComputationServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Delegates to the new controller implementation.
     */
    override fun processCalculation(request: CalculationRequest): Flow<CalculationResponse> {
        logger.info("Legacy service received request, delegating to new implementation")
        return computationGrpcController.processCalculation(request)
    }
}
