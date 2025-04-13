package de.laboranowitsch.poc.grpcmock.backend.repository

import de.laboranowitsch.poc.grpcmock.protobuf.CalculationStatus.Status as GrpcStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository for storing and retrieving job data.
 * This class handles the persistence of job statuses, results, and inputs.
 */
@Repository
class JobRepository {

    private val logger = LoggerFactory.getLogger(javaClass)

    // --- Mock Data Storage (In-memory for POC) ---
    private val jobStatuses = ConcurrentHashMap<String, GrpcStatus>()
    private val jobResults = ConcurrentHashMap<String, List<String>>()
    private val jobInputs = ConcurrentHashMap<String, List<String>>() // Store inputs for simulation
    // --- End Mock Data Storage ---

    /**
     * Updates the status of a job.
     *
     * @param jobId The ID of the job
     * @param status The new status
     */
    fun updateJobStatus(jobId: String, status: GrpcStatus) {
        jobStatuses[jobId] = status
        logger.debug("[{}] Job status updated to {}", jobId, status)
    }

    /**
     * Gets the current status of a job.
     *
     * @param jobId The ID of the job
     * @return The current status, or null if the job doesn't exist
     */
    fun getJobStatus(jobId: String): GrpcStatus? {
        return jobStatuses[jobId]
    }

    /**
     * Stores the input values for a job.
     *
     * @param jobId The ID of the job
     * @param inputs The input values
     */
    fun storeJobInputs(jobId: String, inputs: List<String>) {
        jobInputs[jobId] = inputs
        logger.debug("[{}] Stored {} input items", jobId, inputs.size)
    }

    /**
     * Gets the input values for a job.
     *
     * @param jobId The ID of the job
     * @return The input values, or null if the job doesn't exist
     */
    fun getJobInputs(jobId: String): List<String>? {
        return jobInputs[jobId]
    }

    /**
     * Stores the results for a job.
     *
     * @param jobId The ID of the job
     * @param results The result values
     */
    fun storeJobResults(jobId: String, results: List<String>) {
        jobResults[jobId] = results
        logger.debug("[{}] Stored {} result items", jobId, results.size)
    }

    /**
     * Gets the results for a job.
     *
     * @param jobId The ID of the job
     * @return The result values, or null if the job doesn't exist
     */
    fun getJobResults(jobId: String): List<String>? {
        return jobResults[jobId]
    }

    /**
     * Clears the results for a job.
     *
     * @param jobId The ID of the job
     */
    fun clearJobResults(jobId: String) {
        jobResults.remove(jobId)
        logger.debug("[{}] Cleared job results", jobId)
    }
}