package de.laboranowitsch.poc.grpcmock.backend.controller

import de.laboranowitsch.poc.grpcmock.backend.service.ComputationService
import de.laboranowitsch.poc.grpcmock.logging.LoggingAware
import de.laboranowitsch.poc.grpcmock.logging.logger
import de.laboranowitsch.poc.grpcmock.protobuf.*
import de.laboranowitsch.poc.grpcmock.protobuf.CalculationStatus.Status as GrpcStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import net.devh.boot.grpc.server.service.GrpcService

/**
 * gRPC controller for the computation service.
 * This class handles gRPC requests and delegates to the ComputationService for business logic.
 */
@GrpcService
class ComputationGrpcController(private val computationService: ComputationService) : 
    ComputationServiceGrpcKt.ComputationServiceCoroutineImplBase(), LoggingAware {

    private val logger = logger()

    /**
     * Processes a calculation request and returns a flow of responses.
     *
     * @param request The calculation request
     * @return A flow of calculation responses
     */
    override fun processCalculation(request: CalculationRequest): Flow<CalculationResponse> {
        val jobId = request.jobId
        val inputItems = request.inputItemsList ?: listOf()

        logger.info("[$jobId] Received ProcessCalculation request with {} items.", inputItems.size)

        // Validate request
        val validationResult = computationService.validateRequest(jobId, inputItems)
        if (!validationResult.isValid) {
            // Return a flow with a single FAILED status response
            return kotlinx.coroutines.flow.flowOf(
                computationService.createStatusResponse(
                    jobId.ifBlank { "NO_JOB_ID" }, 
                    GrpcStatus.FAILED, 
                    validationResult.errorMessage
                )
            )
        }

        // Process the request
        return computationService.processCalculation(jobId, inputItems)
            .catch { e ->
                // Catch exceptions during flow emission
                logger.error("Error emitting response flow", e)
            }
    }
}
