package de.laboranowitsch.poc.grpcmock.backend.repository

import de.laboranowitsch.poc.grpcmock.protobuf.CalculationStatus.Status as GrpcStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for the JobRepository class.
 */
class JobRepositoryTest {

    private lateinit var repository: JobRepository

    @BeforeEach
    fun setUp() {
        repository = JobRepository()
    }

    @Test
    fun `should store and retrieve job status`() {
        // Given
        val jobId = "test-job-1"
        val status = GrpcStatus.ACCEPTED

        // When
        repository.updateJobStatus(jobId, status)
        val retrievedStatus = repository.getJobStatus(jobId)

        // Then
        assertEquals(status, retrievedStatus)
    }

    @Test
    fun `should return null for non-existent job status`() {
        // Given
        val jobId = "non-existent-job"

        // When
        val retrievedStatus = repository.getJobStatus(jobId)

        // Then
        assertNull(retrievedStatus)
    }

    @Test
    fun `should store and retrieve job inputs`() {
        // Given
        val jobId = "test-job-2"
        val inputs = listOf("input1", "input2", "input3")

        // When
        repository.storeJobInputs(jobId, inputs)
        val retrievedInputs = repository.getJobInputs(jobId)

        // Then
        assertEquals(inputs, retrievedInputs)
    }

    @Test
    fun `should return null for non-existent job inputs`() {
        // Given
        val jobId = "non-existent-job"

        // When
        val retrievedInputs = repository.getJobInputs(jobId)

        // Then
        assertNull(retrievedInputs)
    }

    @Test
    fun `should store and retrieve job results`() {
        // Given
        val jobId = "test-job-3"
        val results = listOf("result1", "result2", "result3")

        // When
        repository.storeJobResults(jobId, results)
        val retrievedResults = repository.getJobResults(jobId)

        // Then
        assertEquals(results, retrievedResults)
    }

    @Test
    fun `should return null for non-existent job results`() {
        // Given
        val jobId = "non-existent-job"

        // When
        val retrievedResults = repository.getJobResults(jobId)

        // Then
        assertNull(retrievedResults)
    }

    @Test
    fun `should clear job results`() {
        // Given
        val jobId = "test-job-4"
        val results = listOf("result1", "result2", "result3")
        repository.storeJobResults(jobId, results)

        // When
        repository.clearJobResults(jobId)
        val retrievedResults = repository.getJobResults(jobId)

        // Then
        assertNull(retrievedResults)
    }

    @Test
    fun `should update job status`() {
        // Given
        val jobId = "test-job-5"
        repository.updateJobStatus(jobId, GrpcStatus.ACCEPTED)

        // When
        repository.updateJobStatus(jobId, GrpcStatus.IN_PROGRESS)
        val retrievedStatus = repository.getJobStatus(jobId)

        // Then
        assertEquals(GrpcStatus.IN_PROGRESS, retrievedStatus)
    }
}