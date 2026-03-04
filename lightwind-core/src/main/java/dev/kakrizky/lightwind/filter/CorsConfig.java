package dev.kakrizky.lightwind.filter;

/**
 * CORS is handled by Quarkus built-in support. Configure via application.properties:
 *
 * <pre>
 * # Enable CORS
 * quarkus.http.cors=true
 *
 * # Allow all origins (dev mode)
 * quarkus.http.cors.origins=*
 *
 * # Production: specific origins
 * quarkus.http.cors.origins=https://myapp.com,https://admin.myapp.com
 *
 * # Allowed methods
 * quarkus.http.cors.methods=GET,POST,PUT,PATCH,DELETE,OPTIONS
 *
 * # Allowed headers
 * quarkus.http.cors.headers=Content-Type,Authorization,X-Request-Id
 *
 * # Exposed headers (accessible from browser JS)
 * quarkus.http.cors.exposed-headers=X-Request-Id
 *
 * # Max age for preflight cache (seconds)
 * quarkus.http.cors.access-control-max-age=24H
 * </pre>
 *
 * No custom filter needed — Quarkus handles CORS natively.
 */
public final class CorsConfig {
    private CorsConfig() {}
}
