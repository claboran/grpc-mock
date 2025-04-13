package de.laboranowitsch.poc.grpcmock.backend.integration

import de.laboranowitsch.poc.grpcmock.GrpcMockApplication
import de.laboranowitsch.poc.grpcmock.logging.LoggingAware
import de.laboranowitsch.poc.grpcmock.logging.logger
import de.laboranowitsch.poc.grpcmock.protobuf.*
import de.laboranowitsch.poc.grpcmock.protobuf.CalculationStatus.Status as GrpcStatus
import io.grpc.testing.GrpcCleanupRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import net.devh.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration
import net.devh.boot.grpc.client.inject.GrpcClient
import net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration
import net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration
import org.junit.Rule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.fail
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext

/**
 * Integration test for the Computation Service.
 * This test verifies that the entire system works correctly with the new modular implementation.
 */
@SpringBootTest(
    classes = [GrpcMockApplication::class], // Load main context
    properties = [
        "grpc.server.inProcessName=test", // Create an in-process server for tests
        "grpc.server.port=-1",            // Disable the network server during tests
        "grpc.client.inProcess.address=in-process:test" // Configure client to use in-process server
    ]
)
@ImportAutoConfiguration(
    GrpcServerAutoConfiguration::class,
    GrpcServerFactoryAutoConfiguration::class,
    GrpcClientAutoConfiguration::class // Auto-configure client for testing
)
@DirtiesContext // Fresh context per test class
class ComputationServiceIntegrationTest : LoggingAware {

    private val logger = logger()

    @get:Rule
    val grpcCleanup = GrpcCleanupRule() // Manages server/channel lifecycle

    // Inject the client stub configured by Spring Boot test auto-config
    @GrpcClient("inProcess") // Name matches the config property prefix
    private lateinit var clientStub: ComputationServiceGrpcKt.ComputationServiceCoroutineStub

    @Test
    fun `should accept input, process, and stream status and results`() = runBlocking {
        val jobId = "test-integration-${System.currentTimeMillis()}"
        val inputValues = listOf("itemA", "itemB", "itemC", "itemD", "itemE", "itemF", "itemG", "itemH", "itemI", "itemJ", "itemK") // 11 items
        val inputItemsProto = inputValues.map { InputParamItem.newBuilder().setValue(it).build() }

        val request = CalculationRequest.newBuilder()
            .setJobId(jobId)
            .addAllInputItems(inputItemsProto)
            .build()

        val responses = mutableListOf<CalculationResponse>()
        val receivedOutputItems = mutableListOf<OutputParamItem>()
        var finalStatus: GrpcStatus? = null
        var receivedChunks = 0

        // Collect the response stream
        val collectionJob = launch {
            clientStub.processCalculation(request).collect { response ->
                logger.info("Test received: {}", response.responseTypeCase)
                responses.add(response)
                when (response.responseTypeCase) {
                    CalculationResponse.ResponseTypeCase.STATUS_UPDATE -> {
                        finalStatus = response.statusUpdate.status
                        logger.info(" -> Status: {}", response.statusUpdate.status)
                    }
                    CalculationResponse.ResponseTypeCase.OUTPUT_CHUNK -> {
                        receivedChunks++
                        response.outputChunk.itemsList.forEach { receivedOutputItems.add(it) }
                        logger.info(" -> Chunk Items: {}", response.outputChunk.itemsList.size)
                    }
                    else -> logger.info(" -> Unknown response type")
                }
            }
        }

        // Wait for the stream to complete
        withTimeoutOrNull(30_000L) { // 30 second timeout
            collectionJob.join()
        } ?: fail("Response collection timed out for $jobId")

        // Assertions
        // Status checks
        assertNotNull(responses.find { it.statusUpdate?.status == GrpcStatus.ACCEPTED }, "Should receive ACCEPTED status")
        assertNotNull(responses.find { it.statusUpdate?.status == GrpcStatus.IN_PROGRESS }, "Should receive IN_PROGRESS status")
        assertNotNull(responses.find { it.statusUpdate?.status == GrpcStatus.FINISHED }, "Should receive FINISHED status")
        assertEquals(GrpcStatus.FINISHED, finalStatus, "Final status should be FINISHED")

        // Output checks
        assertEquals(11, receivedOutputItems.size, "Should receive 11 output items")
        // Expect 2 chunks (10 items + 1 item)
        val outputChunksReceived = responses.count { it.responseTypeCase == CalculationResponse.ResponseTypeCase.OUTPUT_CHUNK }
        assertEquals(2, outputChunksReceived, "Should receive results in 2 chunks")

        assertTrue(receivedOutputItems.any { it.value.contains("Processed: 'itemA'") })
        assertTrue(receivedOutputItems.any { it.value.contains("Processed: 'itemK'") })
    }

    @Test
    fun `should handle empty input list`() = runBlocking {
        val jobId = "test-empty-input-${System.currentTimeMillis()}"
        val inputItemsProto = listOf<InputParamItem>()

        val request = CalculationRequest.newBuilder()
            .setJobId(jobId)
            .build() // No input items

        val responses = mutableListOf<CalculationResponse>()
        var finalStatus: GrpcStatus? = null

        // Collect the response stream
        val collectionJob = launch {
            clientStub.processCalculation(request).collect { response ->
                logger.info("Test received: {}", response.responseTypeCase)
                responses.add(response)
                if (response.responseTypeCase == CalculationResponse.ResponseTypeCase.STATUS_UPDATE) {
                    finalStatus = response.statusUpdate.status
                    logger.info(" -> Status: {}", response.statusUpdate.status)
                }
            }
        }

        // Wait for the stream to complete
        withTimeoutOrNull(10_000L) { // 10 second timeout
            collectionJob.join()
        } ?: fail("Response collection timed out for $jobId")

        // Assertions
        // Status checks
        assertNotNull(responses.find { it.statusUpdate?.status == GrpcStatus.ACCEPTED }, "Should receive ACCEPTED status")
        assertNotNull(responses.find { it.statusUpdate?.status == GrpcStatus.IN_PROGRESS }, "Should receive IN_PROGRESS status")
        assertNotNull(responses.find { it.statusUpdate?.status == GrpcStatus.FINISHED }, "Should receive FINISHED status")
        assertEquals(GrpcStatus.FINISHED, finalStatus, "Final status should be FINISHED")

        // Output checks - should be empty
        val outputChunksReceived = responses.count { it.responseTypeCase == CalculationResponse.ResponseTypeCase.OUTPUT_CHUNK }
        assertEquals(0, outputChunksReceived, "Should receive no output chunks for empty input")
    }

    @Test
    fun `should reject request with blank job ID`() = runBlocking {
        val jobId = "" // Blank job ID
        val inputValues = listOf("item1", "item2")
        val inputItemsProto = inputValues.map { InputParamItem.newBuilder().setValue(it).build() }

        val request = CalculationRequest.newBuilder()
            .setJobId(jobId)
            .addAllInputItems(inputItemsProto)
            .build()

        val responses = mutableListOf<CalculationResponse>()
        var finalStatus: GrpcStatus? = null

        // Collect the response stream
        val collectionJob = launch {
            clientStub.processCalculation(request).collect { response ->
                logger.info("Test received: {}", response.responseTypeCase)
                responses.add(response)
                if (response.responseTypeCase == CalculationResponse.ResponseTypeCase.STATUS_UPDATE) {
                    finalStatus = response.statusUpdate.status
                    logger.info(" -> Status: {}", response.statusUpdate.status)
                }
            }
        }

        // Wait for the stream to complete
        withTimeoutOrNull(5_000L) { // 5 second timeout
            collectionJob.join()
        } ?: fail("Response collection timed out for blank job ID")

        // Assertions
        assertEquals(1, responses.size, "Should receive exactly one response")
        assertEquals(GrpcStatus.FAILED, finalStatus, "Final status should be FAILED")
        assertTrue(responses[0].statusUpdate.message.contains("job_id is required"), 
            "Error message should indicate job_id is required")
    }

    @Test
    fun `should reject request with too many input items`() = runBlocking {
        val jobId = "test-too-many-items-${System.currentTimeMillis()}"
        val inputItemsProto = List(1001) { 
            InputParamItem.newBuilder().setValue("item$it").build() 
        }

        val request = CalculationRequest.newBuilder()
            .setJobId(jobId)
            .addAllInputItems(inputItemsProto)
            .build()

        val responses = mutableListOf<CalculationResponse>()
        var finalStatus: GrpcStatus? = null

        // Collect the response stream
        val collectionJob = launch {
            clientStub.processCalculation(request).collect { response ->
                logger.info("Test received: {}", response.responseTypeCase)
                responses.add(response)
                if (response.responseTypeCase == CalculationResponse.ResponseTypeCase.STATUS_UPDATE) {
                    finalStatus = response.statusUpdate.status
                    logger.info(" -> Status: {}", response.statusUpdate.status)
                }
            }
        }

        // Wait for the stream to complete
        withTimeoutOrNull(5_000L) { // 5 second timeout
            collectionJob.join()
        } ?: fail("Response collection timed out for $jobId")

        // Assertions
        assertEquals(1, responses.size, "Should receive exactly one response")
        assertEquals(GrpcStatus.FAILED, finalStatus, "Final status should be FAILED")
        assertTrue(responses[0].statusUpdate.message.contains("Exceeded maximum input item limit"), 
            "Error message should indicate too many items")
    }
}
