package dev.kakrizky.lightwind.scheduler;

import dev.kakrizky.lightwind.exception.BadRequestException;
import dev.kakrizky.lightwind.exception.ObjectNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for enqueuing, cancelling, and querying background jobs.
 */
@ApplicationScoped
public class JobSchedulerService {

    @Inject
    Instance<LightJob> jobImplementations;

    /**
     * Enqueue a job for immediate execution.
     */
    @Transactional
    public UUID enqueue(String jobName, String payload) {
        return enqueue(jobName, payload, LocalDateTime.now());
    }

    /**
     * Enqueue a job scheduled for a specific time.
     */
    @Transactional
    public UUID enqueue(String jobName, String payload, LocalDateTime scheduledAt) {
        if (jobName == null || jobName.isBlank()) {
            throw new BadRequestException("jobName is required");
        }

        // Resolve max retries from the job implementation if available
        int maxRetries = 3;
        for (LightJob job : jobImplementations) {
            if (job.getJobName().equals(jobName)) {
                maxRetries = job.getMaxRetries();
                break;
            }
        }

        JobRecord record = new JobRecord();
        record.setJobName(jobName);
        record.setPayload(payload);
        record.setScheduledAt(scheduledAt != null ? scheduledAt : LocalDateTime.now());
        record.setMaxRetries(maxRetries);
        record.persist();

        return record.getId();
    }

    /**
     * Cancel a pending job. Only PENDING jobs can be cancelled.
     */
    @Transactional
    public void cancel(UUID jobId) {
        JobRecord record = JobRecord.findById(jobId);
        if (record == null) {
            throw new ObjectNotFoundException("Job not found: " + jobId);
        }
        if (record.getStatus() != JobStatus.PENDING) {
            throw new BadRequestException("Only PENDING jobs can be cancelled, current status: " + record.getStatus());
        }
        record.setStatus(JobStatus.CANCELLED);
        record.persist();
    }

    /**
     * Get a job by its ID.
     */
    public Optional<JobRecord> getJob(UUID jobId) {
        JobRecord record = JobRecord.findById(jobId);
        return Optional.ofNullable(record);
    }

    /**
     * List jobs filtered by status with pagination.
     *
     * @param status filter by status, or null for all jobs
     * @param page   1-based page number
     * @param size   page size
     */
    public List<JobRecord> listJobs(JobStatus status, int page, int size) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.max(1, Math.min(size, 100));

        if (status != null) {
            return JobRecord.find("status = ?1 ORDER BY createdAt DESC", status)
                    .page(pageIndex, pageSize)
                    .list();
        }
        return JobRecord.find("ORDER BY createdAt DESC")
                .page(pageIndex, pageSize)
                .list();
    }
}
