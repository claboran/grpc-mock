package de.laboranowitsch.poc.grpcmock.backend.controller

import de.laboranowitsch.poc.grpcmock.backend.service.ComputationService
import de.laboranowitsch.poc.grpcmock.logging.LoggingAware
import de.laboranowitsch.poc.grpcmock.protobuf.CalculationRequest
import de.laboranowitsch.poc.grpcmock.protobuf.CalculationResponse
import de.laboranowitsch.poc.grpcmock.protobuf.CalculationStatus
import de.laboranowitsch.poc.grpcmock.protobuf.InputParamItem
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import de.laboranowitsch.poc.grpcmock.protobuf.CalculationStatus.Status as GrpcStatus

/**
 * Unit tests for the ComputationGrpcController class.
 */
@ExtendWith(MockitoExtension::class)
class ComputationGrpcControllerTest : LoggingAware {

    @Mock
    private lateinit var computationService: ComputationService

    private lateinit var controller: ComputationGrpcController

    @BeforeEach
    fun setUp() {
        controller = ComputationGrpcController(computationService)
    }

    @Test
    fun `should delegate to service for valid request`() {
        // Given
        val jobId = "test-job"
        val inputItems = listOf(
            InputParamItem.newBuilder().setValue("item1").build(),
            InputParamItem.newBuilder().setValue("item2").build()
        )
        val request = CalculationRequest.newBuilder()
            .setJobId(jobId)
            .addAllInputItems(inputItems)
            .build()

        val validationResult = ComputationService.ValidationResult(true)
        val serviceResponse = CalculationResponse.newBuilder()
            .setJobId(jobId)
            .setStatusUpdate(CalculationStatus.newBuilder().setStatus(GrpcStatus.ACCEPTED))
            .build()
        val serviceFlow = flowOf(serviceResponse)

        // Mock service behavior
        Mockito.`when`(computationService.validateRequest(jobId, inputItems))
            .thenReturn(validationResult)
        Mockito.`when`(computationService.processCalculation(jobId, inputItems))
            .thenReturn(serviceFlow)

        // Use kotlinx-coroutines-test runTest
        kotlinx.coroutines.runBlocking {
            // When
            val responses = controller.processCalculation(request).toList()

            // Then
            assertEquals(1, responses.size)
            assertEquals(jobId, responses[0].jobId)
            assertEquals(GrpcStatus.ACCEPTED, responses[0].statusUpdate.status)
        }

        // Verify service interactions
        Mockito.verify(computationService).validateRequest(jobId, inputItems)
        Mockito.verify(computationService).processCalculation(jobId, inputItems)
    }

    @Test
    fun `should return error response for invalid request`() {
        // Given
        val jobId = ""
        val inputItems = listOf(
            InputParamItem.newBuilder().setValue("item1").build()
        )
        val request = CalculationRequest.newBuilder()
            .setJobId(jobId)
            .addAllInputItems(inputItems)
            .build()

        val errorMessage = "job_id is required"
        val validationResult = ComputationService.ValidationResult(false, errorMessage)
        val errorResponse = CalculationResponse.newBuilder()
            .setJobId("NO_JOB_ID")
            .setStatusUpdate(CalculationStatus.newBuilder()
                .setStatus(GrpcStatus.FAILED)
                .setMessage(errorMessage))
            .build()

        // Mock service behavior
        Mockito.`when`(computationService.validateRequest(jobId, inputItems))
            .thenReturn(validationResult)
        Mockito.`when`(computationService.createStatusResponse("NO_JOB_ID", GrpcStatus.FAILED, errorMessage))
            .thenReturn(errorResponse)

        kotlinx.coroutines.runBlocking {
            // When
            val responses = controller.processCalculation(request).toList()

            // Then
            assertEquals(1, responses.size)
            assertEquals("NO_JOB_ID", responses[0].jobId)
            assertEquals(GrpcStatus.FAILED, responses[0].statusUpdate.status)
            assertEquals(errorMessage, responses[0].statusUpdate.message)
        }

        // Verify service interactions
        Mockito.verify(computationService).validateRequest(jobId, inputItems)
        Mockito.verify(computationService).createStatusResponse("NO_JOB_ID", GrpcStatus.FAILED, errorMessage)
        // Verify that processCalculation was not called
        Mockito.verify(computationService, Mockito.never()).processCalculation(Mockito.anyString(), Mockito.anyList())
    }

    @Test
    fun `should handle too many input items`() {
        // Given
        val jobId = "test-job"
        val inputItems = List(1001) { 
            InputParamItem.newBuilder().setValue("item$it").build() 
        }
        val request = CalculationRequest.newBuilder()
            .setJobId(jobId)
            .addAllInputItems(inputItems)
            .build()

        val errorMessage = "Exceeded maximum input item limit of 1000"
        val validationResult = ComputationService.ValidationResult(false, errorMessage)
        val errorResponse = CalculationResponse.newBuilder()
            .setJobId(jobId)
            .setStatusUpdate(CalculationStatus.newBuilder()
                .setStatus(GrpcStatus.FAILED)
                .setMessage(errorMessage))
            .build()

        // Mock service behavior
        Mockito.`when`(computationService.validateRequest(jobId, inputItems))
            .thenReturn(validationResult)
        Mockito.`when`(computationService.createStatusResponse(jobId, GrpcStatus.FAILED, errorMessage))
            .thenReturn(errorResponse)

        kotlinx.coroutines.runBlocking {
            // When
            val responses = controller.processCalculation(request).toList()

            // Then
            assertEquals(1, responses.size)
            assertEquals(jobId, responses[0].jobId)
            assertEquals(GrpcStatus.FAILED, responses[0].statusUpdate.status)
            assertEquals(errorMessage, responses[0].statusUpdate.message)
        }

        // Verify service interactions
        Mockito.verify(computationService).validateRequest(jobId, inputItems)
        Mockito.verify(computationService).createStatusResponse(jobId, GrpcStatus.FAILED, errorMessage)
        // Verify that processCalculation was not called
        Mockito.verify(computationService, Mockito.never()).processCalculation(Mockito.anyString(), Mockito.anyList())
    }
}
