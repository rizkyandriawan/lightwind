package dev.kakrizky.lightwind.scheduler;

import dev.kakrizky.lightwind.exception.ObjectNotFoundException;
import dev.kakrizky.lightwind.response.LightResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST resource for job monitoring and management.
 */
@Path("/api/jobs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JobResource {

    @Inject
    JobSchedulerService schedulerService;

    /**
     * List jobs with optional status filter and pagination.
     */
    @GET
    public LightResponse<List<JobRecord>> listJobs(
            @QueryParam("status") String status,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("10") int size
    ) {
        JobStatus jobStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                jobStatus = JobStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new dev.kakrizky.lightwind.exception.BadRequestException(
                        "Invalid status: " + status + ". Valid values: PENDING, RUNNING, COMPLETED, FAILED, CANCELLED");
            }
        }
        return LightResponse.ok(schedulerService.listJobs(jobStatus, page, size));
    }

    /**
     * Get a single job by ID.
     */
    @GET
    @Path("{id}")
    public LightResponse<JobRecord> getJob(@PathParam("id") UUID id) {
        return LightResponse.ok(
                schedulerService.getJob(id)
                        .orElseThrow(() -> new ObjectNotFoundException("Job not found: " + id))
        );
    }

    /**
     * Enqueue a new job.
     */
    @POST
    public LightResponse<EnqueueResponse> enqueueJob(EnqueueRequest request) {
        if (request == null || request.getJobName() == null || request.getJobName().isBlank()) {
            throw new dev.kakrizky.lightwind.exception.BadRequestException("jobName is required");
        }

        UUID jobId;
        if (request.getScheduledAt() != null) {
            jobId = schedulerService.enqueue(request.getJobName(), request.getPayload(), request.getScheduledAt());
        } else {
            jobId = schedulerService.enqueue(request.getJobName(), request.getPayload());
        }

        return LightResponse.ok(new EnqueueResponse(jobId));
    }

    /**
     * Cancel a pending job.
     */
    @DELETE
    @Path("{id}")
    public LightResponse<Void> cancelJob(@PathParam("id") UUID id) {
        schedulerService.cancel(id);
        return LightResponse.ok(null);
    }

    // --- Request/Response DTOs ---

    public static class EnqueueRequest {
        private String jobName;
        private String payload;
        private LocalDateTime scheduledAt;

        public EnqueueRequest() {}

        public String getJobName() { return jobName; }
        public void setJobName(String jobName) { this.jobName = jobName; }

        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }

        public LocalDateTime getScheduledAt() { return scheduledAt; }
        public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    }

    public static class EnqueueResponse {
        private UUID jobId;

        public EnqueueResponse() {}

        public EnqueueResponse(UUID jobId) {
            this.jobId = jobId;
        }

        public UUID getJobId() { return jobId; }
        public void setJobId(UUID jobId) { this.jobId = jobId; }
    }
}
