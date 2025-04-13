package de.laboranowitsch.poc.grpcmock.backend.service

import de.laboranowitsch.poc.grpcmock.backend.repository.JobRepository
import de.laboranowitsch.poc.grpcmock.logging.LoggingAware
import de.laboranowitsch.poc.grpcmock.logging.logger
import de.laboranowitsch.poc.grpcmock.protobuf.*
import de.laboranowitsch.poc.grpcmock.protobuf.CalculationStatus.Status as GrpcStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Service
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Service for processing calculation requests.
 * This class contains the business logic for validating requests,
 * processing calculations, and generating responses.
 */
@Service
class ComputationService(private val jobRepository: JobRepository) : LoggingAware {

    private val logger = logger()

    /**
     * Validates a calculation request.
     *
     * @param jobId The ID of the job
     * @param inputItems The list of input items
     * @return A validation result containing a status and optional error message
     */
    fun validateRequest(jobId: String, inputItems: List<InputParamItem>): ValidationResult {
        if (jobId.isBlank()) {
            logger.error("Request received without job_id.")
            return ValidationResult(false, "job_id is required")
        }

        if (inputItems.size > 1000) {
            logger.error("[{}] Received {} items, exceeding limit of 1000.", jobId, inputItems.size)
            return ValidationResult(false, "Exceeded maximum input item limit of 1000")
        }

        return ValidationResult(true)
    }

    /**
     * Processes a calculation request and returns a flow of responses.
     *
     * @param jobId The ID of the job
     * @param inputItems The list of input items
     * @return A flow of calculation responses
     */
    fun processCalculation(jobId: String, inputItems: List<InputParamItem>): Flow<CalculationResponse> = flow {
        logger.info("[{}] Processing calculation with {} items.", jobId, inputItems.size)

        // Initialize job data
        jobRepository.updateJobStatus(jobId, GrpcStatus.ACCEPTED)
        jobRepository.clearJobResults(jobId)
        jobRepository.storeJobInputs(jobId, inputItems.map { it.value })

        // Emit ACCEPTED status
        emit(createStatusResponse(jobId, GrpcStatus.ACCEPTED))

        try {
            // Update status to IN_PROGRESS
            jobRepository.updateJobStatus(jobId, GrpcStatus.IN_PROGRESS)
            emit(createStatusResponse(jobId, GrpcStatus.IN_PROGRESS, "Calculation starting..."))
            logger.info("[{}] Simulating calculation...", jobId)

            // Simulate work
            delay(2.seconds + (inputItems.size * 5L).milliseconds)

            // Generate results
            val results = generateResults(jobId, inputItems)
            jobRepository.storeJobResults(jobId, results)
            logger.info("[{}] Calculation finished. Stored {} results.", jobId, results.size)

            // Update status to FINISHED
            jobRepository.updateJobStatus(jobId, GrpcStatus.FINISHED)
            emit(createStatusResponse(jobId, GrpcStatus.FINISHED, "Calculation complete. Streaming results."))

            // Stream results
            streamResults(jobId, results, this)

        } catch (e: Exception) {
            handleProcessingError(jobId, e, this)
        }

        logger.info("[{}] Response flow completed.", jobId)
    }

    /**
     * Generates results for a calculation.
     *
     * @param jobId The ID of the job
     * @param inputItems The list of input items
     * @return A list of result strings
     */
    private fun generateResults(jobId: String, inputItems: List<InputParamItem>): List<String> {
        return jobRepository.getJobInputs(jobId)?.mapIndexed { index, input -> 
            "Processed: '${input}' (#${index + 1})" 
        } ?: listOf()
    }

    /**
     * Streams results to the client.
     *
     * @param jobId The ID of the job
     * @param results The list of result strings
     * @param emitter The flow collector to emit responses to
     */
    private suspend fun streamResults(
        jobId: String, 
        results: List<String>, 
        emitter: kotlinx.coroutines.flow.FlowCollector<CalculationResponse>
    ) {
        logger.info("[{}] Starting to stream results...", jobId)
        results.chunked(10).forEach { chunkValues ->
            val outputItems = chunkValues.map { OutputParamItem.newBuilder().setValue(it).build() }
            val outputChunk = OutputChunk.newBuilder().addAllItems(outputItems).build()
            emitter.emit(CalculationResponse.newBuilder()
                .setJobId(jobId)
                .setOutputChunk(outputChunk)
                .build())
            delay(50.milliseconds) // Simulate network delay for chunks
        }
        logger.info("[{}] Finished streaming all results.", jobId)
    }

    /**
     * Handles errors during processing.
     *
     * @param jobId The ID of the job
     * @param exception The exception that occurred
     * @param emitter The flow collector to emit responses to
     */
    private suspend fun handleProcessingError(
        jobId: String, 
        exception: Exception, 
        emitter: kotlinx.coroutines.flow.FlowCollector<CalculationResponse>
    ) {
        logger.error("[{}] Error during calculation or streaming:", jobId, exception)
        jobRepository.updateJobStatus(jobId, GrpcStatus.FAILED)
        emitter.emit(createStatusResponse(jobId, GrpcStatus.FAILED, "Server processing error: ${exception.message}"))
    }

    /**
     * Creates a status response.
     *
     * @param jobId The ID of the job
     * @param status The status
     * @param message An optional message
     * @return A calculation response with the status update
     */
    fun createStatusResponse(jobId: String, status: GrpcStatus, message: String? = null): CalculationResponse {
        val statusBuilder = CalculationStatus.newBuilder().setStatus(status)
        if (message != null) {
            statusBuilder.message = message
        }
        return CalculationResponse.newBuilder()
            .setJobId(jobId)
            .setStatusUpdate(statusBuilder)
            .build()
    }

    /**
     * Represents the result of validating a request.
     *
     * @property isValid Whether the request is valid
     * @property errorMessage An optional error message if the request is invalid
     */
    data class ValidationResult(val isValid: Boolean, val errorMessage: String? = null)
}
