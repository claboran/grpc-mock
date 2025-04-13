package de.laboranowitsch.poc.grpcmock.backend.service

import de.laboranowitsch.poc.grpcmock.backend.repository.JobRepository
import de.laboranowitsch.poc.grpcmock.protobuf.*
import de.laboranowitsch.poc.grpcmock.protobuf.CalculationStatus.Status as GrpcStatus
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

/**
 * Unit tests for the ComputationService class.
 */
@ExtendWith(MockitoExtension::class)
class ComputationServiceTest {

    @Mock
    private lateinit var jobRepository: JobRepository

    private lateinit var service: ComputationService

    @BeforeEach
    fun setUp() {
        service = ComputationService(jobRepository)
    }

    @Test
    fun `validateRequest should return valid result for valid request`() {
        // Given
        val jobId = "test-job"
        val inputItems = listOf(
            InputParamItem.newBuilder().setValue("item1").build(),
            InputParamItem.newBuilder().setValue("item2").build()
        )

        // When
        val result = service.validateRequest(jobId, inputItems)

        // Then
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }

    @Test
    fun `validateRequest should return invalid result for blank job ID`() {
        // Given
        val jobId = ""
        val inputItems = listOf(
            InputParamItem.newBuilder().setValue("item1").build()
        )

        // When
        val result = service.validateRequest(jobId, inputItems)

        // Then
        assertFalse(result.isValid)
        assertEquals("job_id is required", result.errorMessage)
    }

    @Test
    fun `validateRequest should return invalid result for too many input items`() {
        // Given
        val jobId = "test-job"
        val inputItems = List(1001) { 
            InputParamItem.newBuilder().setValue("item$it").build() 
        }

        // When
        val result = service.validateRequest(jobId, inputItems)

        // Then
        assertFalse(result.isValid)
        assertEquals("Exceeded maximum input item limit of 1000", result.errorMessage)
    }

    @Test
    fun `createStatusResponse should create response with status update`() {
        // Given
        val jobId = "test-job"
        val status = GrpcStatus.ACCEPTED
        val message = "Test message"

        // When
        val response = service.createStatusResponse(jobId, status, message)

        // Then
        assertEquals(jobId, response.jobId)
        assertEquals(status, response.statusUpdate.status)
        assertEquals(message, response.statusUpdate.message)
    }

    @Test
    fun `createStatusResponse should create response without message when not provided`() {
        // Given
        val jobId = "test-job"
        val status = GrpcStatus.ACCEPTED

        // When
        val response = service.createStatusResponse(jobId, status)

        // Then
        assertEquals(jobId, response.jobId)
        assertEquals(status, response.statusUpdate.status)
        assertEquals("", response.statusUpdate.message)
    }

    @Test
    fun `processCalculation should emit status updates and results`() = runBlocking {
        // Given
        val jobId = "test-job"
        val inputItems = listOf(
            InputParamItem.newBuilder().setValue("item1").build(),
            InputParamItem.newBuilder().setValue("item2").build()
        )
        val inputValues = inputItems.map { it.value }
        val results = listOf("Processed: 'item1' (#1)", "Processed: 'item2' (#2)")

        // Mock repository behavior
        Mockito.`when`(jobRepository.getJobInputs(jobId)).thenReturn(inputValues)

        // When
        val responses = service.processCalculation(jobId, inputItems).toList()

        // Then
        // Verify repository interactions
        Mockito.verify(jobRepository).updateJobStatus(jobId, GrpcStatus.ACCEPTED)
        Mockito.verify(jobRepository).clearJobResults(jobId)
        Mockito.verify(jobRepository).storeJobInputs(jobId, inputValues)
        Mockito.verify(jobRepository).updateJobStatus(jobId, GrpcStatus.IN_PROGRESS)
        Mockito.verify(jobRepository).storeJobResults(jobId, results)
        Mockito.verify(jobRepository).updateJobStatus(jobId, GrpcStatus.FINISHED)

        // Verify responses
        assertTrue(responses.isNotEmpty())

        // First response should be ACCEPTED status
        assertEquals(GrpcStatus.ACCEPTED, responses[0].statusUpdate.status)

        // Second response should be IN_PROGRESS status
        assertEquals(GrpcStatus.IN_PROGRESS, responses[1].statusUpdate.status)

        // Third response should be FINISHED status
        assertEquals(GrpcStatus.FINISHED, responses[2].statusUpdate.status)

        // Should have at least one output chunk
        assertTrue(responses.any { it.responseTypeCase == CalculationResponse.ResponseTypeCase.OUTPUT_CHUNK })
    }

    @Test
    fun `processCalculation should handle errors`() = runBlocking {
        // Given
        val jobId = "test-job"
        val inputItems = listOf(
            InputParamItem.newBuilder().setValue("item1").build()
        )
        val inputValues = inputItems.map { it.value }

        // Mock repository to throw exception during processing
        Mockito.`when`(jobRepository.getJobInputs(jobId)).thenThrow(RuntimeException("Test exception"))

        // When
        val responses = service.processCalculation(jobId, inputItems).toList()

        // Then
        // Verify repository interactions
        Mockito.verify(jobRepository).updateJobStatus(jobId, GrpcStatus.ACCEPTED)
        Mockito.verify(jobRepository).clearJobResults(jobId)
        Mockito.verify(jobRepository).storeJobInputs(jobId, inputValues)
        Mockito.verify(jobRepository).updateJobStatus(jobId, GrpcStatus.IN_PROGRESS)
        Mockito.verify(jobRepository).updateJobStatus(jobId, GrpcStatus.FAILED)

        // Verify responses
        assertTrue(responses.isNotEmpty())

        // First response should be ACCEPTED status
        assertEquals(GrpcStatus.ACCEPTED, responses[0].statusUpdate.status)

        // Second response should be IN_PROGRESS status
        assertEquals(GrpcStatus.IN_PROGRESS, responses[1].statusUpdate.status)

        // Last response should be FAILED status
        assertEquals(GrpcStatus.FAILED, responses.last().statusUpdate.status)
        assertTrue(responses.last().statusUpdate.message.contains("Test exception"))
    }
}
