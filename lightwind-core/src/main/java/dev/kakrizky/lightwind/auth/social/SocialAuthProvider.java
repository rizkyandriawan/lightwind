package dev.kakrizky.lightwind.auth.social;

import dev.kakrizky.lightwind.auth.dto.SocialLoginRequest;

/**
 * Interface for social authentication providers (Google, GitHub, etc.).
 *
 * <p>Implement this interface and annotate with {@code @ApplicationScoped}
 * to add social login support for a provider.</p>
 */
public interface SocialAuthProvider {

    /**
     * Provider name (e.g., "google", "github"). Used in the login endpoint path.
     */
    String getProviderName();

    /**
     * Verify the social login request and return user info.
     *
     * @param request contains the token (for Google ID token) or code (for GitHub OAuth code)
     * @return verified social user info
     * @throws RuntimeException if verification fails
     */
    SocialUser verify(SocialLoginRequest request);
}
