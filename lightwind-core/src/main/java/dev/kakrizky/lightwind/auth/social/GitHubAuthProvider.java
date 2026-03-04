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

/**
 * GitHub OAuth2 social auth provider.
 * Exchanges OAuth code for access token, then fetches user info.
 *
 * <p>Configure via:</p>
 * <pre>
 * lightwind.auth.social.github.client-id=your-github-client-id
 * lightwind.auth.social.github.client-secret=your-github-client-secret
 * </pre>
 */
@ApplicationScoped
public class GitHubAuthProvider implements SocialAuthProvider {

    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String USER_URL = "https://api.github.com/user";
    private static final String EMAILS_URL = "https://api.github.com/user/emails";

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "lightwind.auth.social.github.client-id", defaultValue = "")
    String clientId;

    @ConfigProperty(name = "lightwind.auth.social.github.client-secret", defaultValue = "")
    String clientSecret;

    @Override
    public String getProviderName() {
        return "github";
    }

    @Override
    public SocialUser verify(SocialLoginRequest request) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BadRequestException("GitHub OAuth code is required");
        }

        try {
            String accessToken = exchangeCodeForToken(request.getCode());
            return fetchUserInfo(accessToken);
        } catch (BadRequestException | UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("GitHub authentication failed: " + e.getMessage());
        }
    }

    private String exchangeCodeForToken(String code) throws Exception {
        String body = objectMapper.writeValueAsString(new TokenRequest(clientId, clientSecret, code));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode json = objectMapper.readTree(response.body());
        String accessToken = json.path("access_token").asText(null);
        if (accessToken == null) {
            String error = json.path("error_description").asText("Unknown error");
            throw new UnauthorizedException("GitHub token exchange failed: " + error);
        }
        return accessToken;
    }

    private SocialUser fetchUserInfo(String accessToken) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // Fetch user profile
        HttpRequest userRequest = HttpRequest.newBuilder()
                .uri(URI.create(USER_URL))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> userResponse = client.send(userRequest, HttpResponse.BodyHandlers.ofString());
        JsonNode user = objectMapper.readTree(userResponse.body());

        String email = user.path("email").asText(null);

        // If email is private, fetch from emails endpoint
        if (email == null || email.isEmpty()) {
            HttpRequest emailRequest = HttpRequest.newBuilder()
                    .uri(URI.create(EMAILS_URL))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> emailResponse = client.send(emailRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode emails = objectMapper.readTree(emailResponse.body());

            for (JsonNode e : emails) {
                if (e.path("primary").asBoolean(false)) {
                    email = e.path("email").asText();
                    break;
                }
            }
        }

        if (email == null || email.isEmpty()) {
            throw new UnauthorizedException("Could not retrieve email from GitHub");
        }

        String name = user.path("name").asText(user.path("login").asText());

        return new SocialUser(
                "github",
                String.valueOf(user.path("id").asLong()),
                email,
                name,
                user.path("avatar_url").asText(null),
                true // GitHub emails are verified
        );
    }

    // Inner class for token request body
    private record TokenRequest(String client_id, String client_secret, String code) {}
}
