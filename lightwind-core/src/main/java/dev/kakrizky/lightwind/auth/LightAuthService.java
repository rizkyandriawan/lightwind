package dev.kakrizky.lightwind.auth;

import dev.kakrizky.lightwind.auth.dto.*;
import dev.kakrizky.lightwind.auth.social.SocialAuthProvider;
import dev.kakrizky.lightwind.auth.social.SocialUser;
import dev.kakrizky.lightwind.config.LightwindConfig;
import dev.kakrizky.lightwind.exception.BadRequestException;
import dev.kakrizky.lightwind.exception.ConflictException;
import dev.kakrizky.lightwind.exception.UnauthorizedException;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Abstract auth service providing login, register, refresh token, social auth, and session management.
 *
 * <p>Extend this in your application:</p>
 * <pre>
 * {@literal @}ApplicationScoped
 * public class AuthService extends LightAuthService&lt;User&gt; {
 *     {@literal @}Override
 *     protected Class&lt;User&gt; getUserClass() { return User.class; }
 *
 *     {@literal @}Override
 *     protected Set&lt;String&gt; getDefaultRoles() { return Set.of("USER"); }
 * }
 * </pre>
 */
public abstract class LightAuthService<U extends LightAuthUser> {

    @Inject
    EntityManager em;

    @Inject
    LightwindConfig config;

    @Inject
    Instance<SocialAuthProvider> socialProviders;

    protected abstract Class<U> getUserClass();

    /**
     * Default roles assigned to new users on registration.
     */
    protected Set<String> getDefaultRoles() {
        return Set.of("USER");
    }

    // --- Public API ---

    /**
     * Register a new user with email/password.
     */
    @Transactional
    public TokenPair register(RegisterRequest request) {
        validateRegistration(request);

        U existing = findByEmail(request.getEmail());
        if (existing != null) {
            throw new ConflictException("Email already registered");
        }

        U user = createUser(request);
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setDisplayName(request.getDisplayName());
        user.setPasswordHash(BcryptUtil.bcryptHash(request.getPassword()));
        user.setRoles(new HashSet<>(getDefaultRoles()));
        user.setActive(true);

        afterUserCreated(user, request);
        user.persist();

        return generateTokenPair(user);
    }

    /**
     * Login with email/password.
     */
    @Transactional
    public TokenPair login(LoginRequest request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new BadRequestException("Email and password are required");
        }

        U user = findByEmail(request.getEmail().toLowerCase().trim());
        if (user == null || !user.isActive()) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (user.getPasswordHash() == null) {
            throw new UnauthorizedException("This account uses social login. Please login with your social provider.");
        }

        if (!BcryptUtil.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        user.setLastLoginAt(LocalDateTime.now());
        user.persist();

        return generateTokenPair(user);
    }

    /**
     * Refresh access token using a valid refresh token.
     */
    @Transactional
    public TokenPair refreshToken(RefreshTokenRequest request) {
        if (request.getRefreshToken() == null) {
            throw new BadRequestException("Refresh token is required");
        }

        LightRefreshToken rt = LightRefreshToken.find("token", request.getRefreshToken()).firstResult();
        if (rt == null || !rt.isValid()) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        // Revoke old token (rotation)
        rt.setRevokedAt(LocalDateTime.now());
        rt.persist();

        U user = em.find(getUserClass(), rt.getUserId());
        if (user == null || !user.isActive()) {
            throw new UnauthorizedException("User not found or inactive");
        }

        return generateTokenPair(user);
    }

    /**
     * Social login — verify token with provider, create or link user.
     */
    @Transactional
    public TokenPair socialLogin(String providerName, SocialLoginRequest request) {
        SocialAuthProvider provider = findSocialProvider(providerName);
        SocialUser socialUser = provider.verify(request);

        // Find existing user by social provider ID or email
        U user = findBySocialProvider(providerName, socialUser.getProviderId());
        if (user == null) {
            user = findByEmail(socialUser.getEmail());
        }

        if (user == null) {
            // Create new user from social login
            user = createUserFromSocial(socialUser);
            user.persist();
        } else {
            // Link social provider if not already linked
            if (user.getSocialProvider() == null) {
                user.setSocialProvider(socialUser.getProvider());
                user.setSocialProviderId(socialUser.getProviderId());
            }
            if (socialUser.getAvatarUrl() != null) {
                user.setAvatarUrl(socialUser.getAvatarUrl());
            }
        }

        user.setLastLoginAt(LocalDateTime.now());
        user.persist();

        return generateTokenPair(user);
    }

    /**
     * Logout — revoke the given refresh token.
     */
    @Transactional
    public void logout(RefreshTokenRequest request) {
        if (request.getRefreshToken() == null) return;

        LightRefreshToken rt = LightRefreshToken.find("token", request.getRefreshToken()).firstResult();
        if (rt != null && rt.getRevokedAt() == null) {
            rt.setRevokedAt(LocalDateTime.now());
            rt.persist();
        }
    }

    /**
     * Revoke all refresh tokens for a user (logout all devices).
     */
    @Transactional
    public void revokeAllSessions(UUID userId) {
        LightRefreshToken.update("revokedAt = ?1 where userId = ?2 and revokedAt is null",
                LocalDateTime.now(), userId);
    }

    /**
     * Generate a JWT access token only (for backward compatibility).
     */
    public String generateToken(UUID userId, String userName, Set<String> roles) {
        return Jwt.issuer(config.auth().issuer())
                .upn(userName)
                .claim("user_id", userId.toString())
                .claim("user_name", userName)
                .groups(roles)
                .expiresIn(Duration.ofSeconds(config.auth().tokenExpiration()))
                .sign();
    }

    // --- Hooks for subclass customization ---

    /**
     * Create a new user entity instance. Override to set additional fields.
     */
    protected U createUser(RegisterRequest request) {
        try {
            return getUserClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create user instance", e);
        }
    }

    /**
     * Called after user entity is created but before persist. Add custom fields here.
     */
    protected void afterUserCreated(U user, RegisterRequest request) {}

    /**
     * Validate registration input. Override to add custom rules.
     */
    protected void validateRegistration(RegisterRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters");
        }
        if (request.getDisplayName() == null || request.getDisplayName().isBlank()) {
            throw new BadRequestException("Display name is required");
        }
    }

    /**
     * Handle forgot password. Override to implement email sending.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Default: no-op. Override to send reset email.
        // The framework doesn't include an email layer in core.
    }

    /**
     * Handle password reset. Override to implement token validation.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Default: no-op. Override with token validation and password update.
    }

    // --- Internal helpers ---

    protected TokenPair generateTokenPair(U user) {
        String accessToken = Jwt.issuer(config.auth().issuer())
                .upn(user.getEmail())
                .claim("user_id", user.getId().toString())
                .claim("user_name", user.getDisplayName())
                .groups(user.getRoles())
                .expiresIn(Duration.ofSeconds(config.auth().tokenExpiration()))
                .sign();

        String refreshTokenValue = UUID.randomUUID().toString();

        LightRefreshToken rt = new LightRefreshToken();
        rt.setToken(refreshTokenValue);
        rt.setUserId(user.getId());
        rt.setExpiresAt(LocalDateTime.now().plusSeconds(config.auth().refreshTokenExpiration()));
        rt.persist();

        return new TokenPair(accessToken, refreshTokenValue, config.auth().tokenExpiration());
    }

    @SuppressWarnings("unchecked")
    protected U findByEmail(String email) {
        return (U) em.createQuery("SELECT u FROM " + getUserClass().getSimpleName() + " u WHERE lower(u.email) = :email", getUserClass())
                .setParameter("email", email.toLowerCase().trim())
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    protected U findBySocialProvider(String provider, String providerId) {
        return (U) em.createQuery(
                        "SELECT u FROM " + getUserClass().getSimpleName() +
                                " u WHERE u.socialProvider = :provider AND u.socialProviderId = :providerId",
                        getUserClass())
                .setParameter("provider", provider)
                .setParameter("providerId", providerId)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    private U createUserFromSocial(SocialUser socialUser) {
        try {
            U user = getUserClass().getDeclaredConstructor().newInstance();
            user.setEmail(socialUser.getEmail().toLowerCase().trim());
            user.setDisplayName(socialUser.getDisplayName());
            user.setSocialProvider(socialUser.getProvider());
            user.setSocialProviderId(socialUser.getProviderId());
            user.setAvatarUrl(socialUser.getAvatarUrl());
            user.setEmailVerified(socialUser.isEmailVerified());
            user.setRoles(new HashSet<>(getDefaultRoles()));
            user.setActive(true);
            return user;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create user from social login", e);
        }
    }

    private SocialAuthProvider findSocialProvider(String providerName) {
        for (SocialAuthProvider provider : socialProviders) {
            if (provider.getProviderName().equals(providerName)) {
                return provider;
            }
        }
        throw new BadRequestException("Unsupported social provider: " + providerName);
    }
}
