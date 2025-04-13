package de.laboranowitsch.poc.grpcmock.backend

import de.laboranowitsch.poc.grpcmock.GrpcMockApplication
import de.laboranowitsch.poc.grpcmock.protobuf.*
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
import de.laboranowitsch.poc.grpcmock.protobuf.CalculationStatus.Status as GrpcStatus

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
class ComputationServiceMockTest {

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
                println("Test received: ${response.responseTypeCase}")
                responses.add(response)
                when (response.responseTypeCase) {
                    CalculationResponse.ResponseTypeCase.STATUS_UPDATE -> {
                        finalStatus = response.statusUpdate.status
                        println(" -> Status: ${response.statusUpdate.status}")
                    }
                    CalculationResponse.ResponseTypeCase.OUTPUT_CHUNK -> {
                        receivedChunks++
                        response.outputChunk.itemsList.forEach { receivedOutputItems.add(it) }
                        println(" -> Chunk Items: ${response.outputChunk.itemsList.size}")
                    }
                    else -> println(" -> Unknown response type")
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

    // Add more tests: Error handling, empty input list, >1000 items (expect failure), etc.
}
