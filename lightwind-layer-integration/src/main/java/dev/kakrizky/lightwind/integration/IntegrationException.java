package dev.kakrizky.lightwind.integration;

/**
 * Runtime exception thrown when an integration HTTP call fails.
 *
 * <p>Carries the HTTP status code, response body, and target URL for
 * diagnostic purposes.</p>
 */
public class IntegrationException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;
    private final String url;

    public IntegrationException(String message, String url, int statusCode, String responseBody) {
        super(message);
        this.url = url;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getUrl() {
        return url;
    }
}
