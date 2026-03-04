package dev.kakrizky.lightwind.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled processor that polls for pending jobs and executes them.
 * Runs every 10 seconds, picks up one job at a time, and handles
 * execution, completion, failure, and retry logic.
 */
@ApplicationScoped
public class JobProcessor {

    private static final Logger LOG = Logger.getLogger(JobProcessor.class);

    @Inject
    Instance<LightJob> jobImplementations;

    @Scheduled(every = "10s")
    void processJobs() {
        List<JobRecord> pendingJobs = JobRecord.find(
                "status = ?1 AND scheduledAt <= ?2 ORDER BY scheduledAt ASC",
                JobStatus.PENDING, LocalDateTime.now()
        ).list();

        for (JobRecord job : pendingJobs) {
            processJob(job.getId());
        }
    }

    @Transactional
    void processJob(java.util.UUID jobId) {
        JobRecord job = JobRecord.findById(jobId);
        if (job == null || job.getStatus() != JobStatus.PENDING) {
            return;
        }

        // Find matching job implementation
        LightJob handler = null;
        for (LightJob candidate : jobImplementations) {
            if (candidate.getJobName().equals(job.getJobName())) {
                handler = candidate;
                break;
            }
        }

        if (handler == null) {
            LOG.warnf("No LightJob implementation found for jobName: %s", job.getJobName());
            job.markFailed("No job handler found for: " + job.getJobName());
            job.persist();
            return;
        }

        // Mark as running
        job.markRunning();
        job.persist();

        try {
            String result = handler.execute(job.getPayload());
            job.markCompleted(result);
            job.persist();
            LOG.infof("Job completed: %s [%s]", job.getJobName(), job.getId());
        } catch (Exception e) {
            LOG.errorf(e, "Job failed: %s [%s]", job.getJobName(), job.getId());
            job.markFailed(e.getMessage());
            job.setRetryCount(job.getRetryCount() + 1);
            job.persist();

            // Re-enqueue for retry if allowed
            if (job.shouldRetry()) {
                LOG.infof("Re-enqueuing job for retry (%d/%d): %s [%s]",
                        job.getRetryCount(), job.getMaxRetries(), job.getJobName(), job.getId());
                job.setStatus(JobStatus.PENDING);
                job.setStartedAt(null);
                job.setCompletedAt(null);
                job.setErrorMessage(null);
                job.persist();
            }
        }
    }
}
