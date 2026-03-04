package dev.kakrizky.lightwind.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple HTTP client wrapper using {@link java.net.http.HttpClient}.
 *
 * <p>Provides typed convenience methods for common HTTP operations (GET, POST, PUT,
 * PATCH, DELETE) with automatic JSON serialization/deserialization via Jackson.
 * All methods throw {@link IntegrationException} on failure.</p>
 */
@ApplicationScoped
public class LightRestClient {

    private static final Logger LOG = Logger.getLogger(LightRestClient.class);

    private final HttpClient httpClient;

    @Inject
    IntegrationConfig config;

    @Inject
    ObjectMapper objectMapper;

    public LightRestClient() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Sends a GET request and deserializes the JSON response.
     *
     * @param url          the target URL
     * @param responseType the class to deserialize the response body into
     * @param <T>          the response type
     * @return the deserialized response
     */
    public <T> T get(String url, Class<T> responseType) {
        return get(url, responseType, Map.of());
    }

    /**
     * Sends a GET request with custom headers and deserializes the JSON response.
     *
     * @param url          the target URL
     * @param responseType the class to deserialize the response body into
     * @param headers      custom HTTP headers
     * @param <T>          the response type
     * @return the deserialized response
     */
    public <T> T get(String url, Class<T> responseType, Map<String, String> headers) {
        RestRequest request = RestRequest.builder()
                .url(url)
                .method("GET")
                .headers(new HashMap<>(headers));
        RestResponse<String> response = execute(request);
        return deserialize(response.getBody(), responseType, url);
    }

    /**
     * Sends a POST request with a JSON body and deserializes the response.
     *
     * @param url          the target URL
     * @param body         the request body (will be serialized to JSON)
     * @param responseType the class to deserialize the response body into
     * @param <T>          the response type
     * @return the deserialized response
     */
    public <T> T post(String url, Object body, Class<T> responseType) {
        return sendWithBody(url, "POST", body, responseType);
    }

    /**
     * Sends a PUT request with a JSON body and deserializes the response.
     *
     * @param url          the target URL
     * @param body         the request body (will be serialized to JSON)
     * @param responseType the class to deserialize the response body into
     * @param <T>          the response type
     * @return the deserialized response
     */
    public <T> T put(String url, Object body, Class<T> responseType) {
        return sendWithBody(url, "PUT", body, responseType);
    }

    /**
     * Sends a PATCH request with a JSON body and deserializes the response.
     *
     * @param url          the target URL
     * @param body         the request body (will be serialized to JSON)
     * @param responseType the class to deserialize the response body into
     * @param <T>          the response type
     * @return the deserialized response
     */
    public <T> T patch(String url, Object body, Class<T> responseType) {
        return sendWithBody(url, "PATCH", body, responseType);
    }

    /**
     * Sends a DELETE request without reading the response body.
     *
     * @param url the target URL
     */
    public void delete(String url) {
        RestRequest request = RestRequest.builder()
                .url(url)
                .method("DELETE");
        RestResponse<String> response = execute(request);
        if (!response.isSuccess()) {
            throw new IntegrationException(
                    "DELETE request failed with status " + response.getStatusCode(),
                    url, response.getStatusCode(), response.getBody());
        }
    }

    /**
     * Sends a DELETE request and deserializes the response.
     *
     * @param url          the target URL
     * @param responseType the class to deserialize the response body into
     * @param <T>          the response type
     * @return the deserialized response
     */
    public <T> T delete(String url, Class<T> responseType) {
        RestRequest request = RestRequest.builder()
                .url(url)
                .method("DELETE");
        RestResponse<String> response = execute(request);
        return deserialize(response.getBody(), responseType, url);
    }

    /**
     * Low-level execute method for full control over the HTTP request.
     *
     * @param request the request to execute
     * @return the raw response with status code, body, headers, and duration
     */
    public RestResponse<String> execute(RestRequest request) {
        try {
            int timeout = request.getTimeoutSeconds() > 0
                    ? request.getTimeoutSeconds()
                    : config.defaultTimeout();

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(request.getUrl()))
                    .timeout(Duration.ofSeconds(timeout));

            // Set headers
            if (request.getHeaders() != null) {
                request.getHeaders().forEach(builder::header);
            }

            // Set method and body
            HttpRequest.BodyPublisher bodyPublisher = request.getBody() != null
                    ? HttpRequest.BodyPublishers.ofString(request.getBody())
                    : HttpRequest.BodyPublishers.noBody();

            builder.method(request.getMethod(), bodyPublisher);

            // Add Content-Type if body is present and not already set
            if (request.getBody() != null && (request.getHeaders() == null
                    || !request.getHeaders().containsKey("Content-Type"))) {
                builder.header("Content-Type", "application/json");
            }

            HttpRequest httpRequest = builder.build();

            LOG.debugf("Executing %s %s", request.getMethod(), request.getUrl());
            long startTime = System.currentTimeMillis();

            HttpResponse<String> httpResponse = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofString());

            long durationMs = System.currentTimeMillis() - startTime;

            // Convert headers
            Map<String, List<String>> responseHeaders = new HashMap<>(httpResponse.headers().map());

            RestResponse<String> response = new RestResponse<>(
                    httpResponse.statusCode(),
                    httpResponse.body(),
                    responseHeaders,
                    durationMs);

            LOG.debugf("Response %s %s: status=%d, duration=%dms",
                    request.getMethod(), request.getUrl(), response.getStatusCode(), durationMs);

            return response;
        } catch (IntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new IntegrationException(
                    "HTTP request failed: " + e.getMessage(),
                    request.getUrl(), 0, null);
        }
    }

    private <T> T sendWithBody(String url, String method, Object body, Class<T> responseType) {
        try {
            String jsonBody = objectMapper.writeValueAsString(body);

            RestRequest request = RestRequest.builder()
                    .url(url)
                    .method(method)
                    .body(jsonBody)
                    .header("Content-Type", "application/json");

            RestResponse<String> response = execute(request);
            return deserialize(response.getBody(), responseType, url);
        } catch (IntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new IntegrationException(
                    "Failed to serialize request body: " + e.getMessage(),
                    url, 0, null);
        }
    }

    private <T> T deserialize(String body, Class<T> responseType, String url) {
        try {
            return objectMapper.readValue(body, responseType);
        } catch (Exception e) {
            throw new IntegrationException(
                    "Failed to deserialize response: " + e.getMessage(),
                    url, 0, body);
        }
    }
}
