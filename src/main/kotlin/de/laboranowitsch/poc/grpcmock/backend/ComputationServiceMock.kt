package de.laboranowitsch.poc.grpcmock.backend

import de.laboranowitsch.poc.grpcmock.protobuf.*
import de.laboranowitsch.poc.grpcmock.protobuf.CalculationStatus.Status as GrpcStatus
import net.devh.boot.grpc.server.service.GrpcService
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@GrpcService // Marks this as a gRPC service bean managed by Spring Boot
class ComputationServiceMock : ComputationServiceGrpcKt.ComputationServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger(javaClass)

    // --- Mock Data Storage (In-memory for POC) ---
    private val jobStatuses = ConcurrentHashMap<String, GrpcStatus>()
    private val jobResults = ConcurrentHashMap<String, List<String>>()
    private val jobInputs = ConcurrentHashMap<String, List<String>>() // Store inputs for simulation
    // --- End Mock Data Storage ---

    // Implementation for the Unary Request / Server Stream RPC
    override fun processCalculation(request: CalculationRequest): Flow<CalculationResponse> = flow {
        val jobId = request.jobId
        val inputItems = request.inputItemsList ?: listOf() // Use generated list property

        logger.info("[{}] Received ProcessCalculation request with {} items.", jobId, inputItems.size)

        // --- Input Validation ---
        if (jobId.isBlank()) {
            logger.error("Request received without job_id.")
            // Emit FAILED status and complete the flow immediately
            emit(createStatusResponse("NO_JOB_ID", GrpcStatus.FAILED, "job_id is required"))
            return@flow // Stop processing this request
        }

        if (inputItems.size > 1000) {
            logger.error("[{}] Received {} items, exceeding limit of 1000.", jobId, inputItems.size)
            emit(createStatusResponse(jobId, GrpcStatus.FAILED, "Exceeded maximum input item limit of 1000"))
            return@flow
        }

        // --- Process Request ---
        jobStatuses[jobId] = GrpcStatus.ACCEPTED
        jobResults.remove(jobId) // Clear previous results if any
        jobInputs[jobId] = inputItems.map { it.value } // Store input values for simulation
        emit(createStatusResponse(jobId, GrpcStatus.ACCEPTED)) // Send ACCEPTED status

        // Simulate calculation and stream results asynchronously
        try {
            // Send IN_PROGRESS status
            jobStatuses[jobId] = GrpcStatus.IN_PROGRESS
            emit(createStatusResponse(jobId, GrpcStatus.IN_PROGRESS, "Calculation starting..."))
            logger.info("[{}] Simulating calculation...", jobId)

            // --- Simulate Work ---
            delay(2.seconds + (inputItems.size * 5L).milliseconds) // Adjust delay as needed
            // --- End Simulate Work ---

            // Simulate generating results
            val results = jobInputs[jobId]?.mapIndexed { index, input -> "Processed: '${input}' (#${index + 1})" } ?: listOf()
            jobResults[jobId] = results // Store mock results
            logger.info("[{}] Calculation finished. Stored {} results.", jobId, results.size)

            // Send FINISHED status
            jobStatuses[jobId] = GrpcStatus.FINISHED
            emit(createStatusResponse(jobId, GrpcStatus.FINISHED, "Calculation complete. Streaming results."))

            // Stream Output Results in Chunks
            logger.info("[{}] Starting to stream results...", jobId)
            results.chunked(10).forEach { chunkValues -> // Use Kotlin's built-in chunked
                val outputItems = chunkValues.map { OutputParamItem.newBuilder().setValue(it).build() }
                val outputChunk = OutputChunk.newBuilder().addAllItems(outputItems).build()
                emit(CalculationResponse.newBuilder()
                    .setJobId(jobId)
                    .setOutputChunk(outputChunk)
                    .build())
                delay(50.milliseconds) // Simulate network delay for chunks
            }
            logger.info("[{}] Finished streaming all results.", jobId)

        } catch (e: Exception) {
            logger.error("[{}] Error during calculation or streaming:", jobId, e)
            jobStatuses[jobId] = GrpcStatus.FAILED
            // Emit FAILED status if an error occurs during processing/streaming
            emit(createStatusResponse(jobId, GrpcStatus.FAILED, "Server processing error: ${e.message}"))
            // Rethrowing might cancel the flow abruptly, emitting FAILED is often better for client handling
        }
        // Flow completion happens automatically when the block finishes
        logger.info("[{}] Response flow completed.", jobId)

    }.catch { e -> // Catch exceptions during flow emission itself (less common)
        logger.error("Error emitting response flow: ", e)
    }

    // Helper to create status responses
    private fun createStatusResponse(jobId: String, status: GrpcStatus, message: String? = null): CalculationResponse {
        val statusBuilder = CalculationStatus.newBuilder().setStatus(status)
        if (message != null) {
            statusBuilder.message = message
        }
        return CalculationResponse.newBuilder()
            .setJobId(jobId)
            .setStatusUpdate(statusBuilder)
            .build()
    }
}

