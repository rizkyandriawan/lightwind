package dev.kakrizky.lightwind.auth.social;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kakrizky.lightwind.auth.dto.SocialLoginRequest;
import dev.kakrizky.lightwind.exception.BadRequestException;
import dev.kakrizky.lightwind.exception.UnauthorizedException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

/**
 * Google OAuth2 social auth provider.
 * Verifies Google ID tokens via Google's tokeninfo endpoint.
 *
 * <p>Configure via:</p>
 * <pre>
 * lightwind.auth.social.google.client-id=your-google-client-id
 * </pre>
 */
@ApplicationScoped
public class GoogleAuthProvider implements SocialAuthProvider {

    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "lightwind.auth.social.google.client-id", defaultValue = "")
    String clientId;

    @Override
    public String getProviderName() {
        return "google";
    }

    @Override
    public SocialUser verify(SocialLoginRequest request) {
        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new BadRequestException("Google ID token is required");
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(TOKENINFO_URL + request.getToken()))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new UnauthorizedException("Invalid Google token");
            }

            JsonNode body = objectMapper.readTree(response.body());

            // Verify audience matches our client ID
            if (!clientId.isEmpty()) {
                String aud = body.path("aud").asText();
                if (!clientId.equals(aud)) {
                    throw new UnauthorizedException("Google token audience mismatch");
                }
            }

            return new SocialUser(
                    "google",
                    body.path("sub").asText(),
                    body.path("email").asText(),
                    body.path("name").asText(body.path("email").asText()),
                    body.path("picture").asText(null),
                    body.path("email_verified").asBoolean(false)
            );
        } catch (UnauthorizedException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("Google token verification failed: " + e.getMessage());
        }
    }
}
