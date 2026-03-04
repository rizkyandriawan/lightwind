package dev.kakrizky.lightwind.integration;

import java.util.List;
import java.util.Map;

/**
 * Generic response wrapper for HTTP responses from {@link LightRestClient}.
 *
 * @param <T> the type of the response body
 */
public class RestResponse<T> {

    private int statusCode;
    private T body;
    private Map<String, List<String>> headers;
    private long durationMs;

    public RestResponse() {
    }

    public RestResponse(int statusCode, T body, Map<String, List<String>> headers, long durationMs) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers;
        this.durationMs = durationMs;
    }

    /**
     * Returns {@code true} if the status code is in the 2xx range.
     */
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
}
