package dev.kakrizky.lightwind.scheduler;

/**
 * Interface that users implement for custom background jobs.
 * Each implementation must be a CDI bean (e.g. {@code @ApplicationScoped})
 * and return a unique job name from {@link #getJobName()}.
 */
public interface LightJob {

    /**
     * Unique name identifying this job type.
     * Used to match enqueued jobs to their handler.
     */
    String getJobName();

    /**
     * Execute the job with the given JSON payload.
     *
     * @param payload JSON string with job-specific data, may be null
     * @return result JSON string, may be null
     * @throws Exception if execution fails
     */
    String execute(String payload) throws Exception;

    /**
     * Maximum number of retry attempts on failure.
     * Defaults to 3.
     */
    default int getMaxRetries() {
        return 3;
    }
}
