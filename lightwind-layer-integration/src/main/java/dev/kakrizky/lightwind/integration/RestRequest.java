package dev.kakrizky.lightwind.integration;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder-pattern class representing an HTTP request for the low-level
 * {@link LightRestClient#execute(RestRequest)} method.
 */
public class RestRequest {

    private String url;
    private String method;
    private Map<String, String> headers;
    private String body;
    private int timeoutSeconds;

    private RestRequest() {
        this.headers = new HashMap<>();
        this.method = "GET";
        this.timeoutSeconds = 0;
    }

    /**
     * Creates a new builder instance.
     *
     * @return a new RestRequest builder
     */
    public static RestRequest builder() {
        return new RestRequest();
    }

    public RestRequest url(String url) {
        this.url = url;
        return this;
    }

    public RestRequest method(String method) {
        this.method = method;
        return this;
    }

    public RestRequest headers(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public RestRequest header(String name, String value) {
        this.headers.put(name, value);
        return this;
    }

    public RestRequest body(String body) {
        this.body = body;
        return this;
    }

    public RestRequest timeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }
}
