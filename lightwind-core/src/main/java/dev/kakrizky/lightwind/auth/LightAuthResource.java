package dev.kakrizky.lightwind.auth;

import dev.kakrizky.lightwind.auth.dto.*;
import dev.kakrizky.lightwind.response.LightResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

/**
 * Abstract auth REST resource. Extend in your application:
 *
 * <pre>
 * {@literal @}Path("/auth")
 * public class AuthResource extends LightAuthResource&lt;User&gt; {
 *     {@literal @}Inject AuthService authService;
 *     {@literal @}Override
 *     protected LightAuthService&lt;User&gt; getAuthService() { return authService; }
 * }
 * </pre>
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public abstract class LightAuthResource<U extends LightAuthUser> {

    protected abstract LightAuthService<U> getAuthService();

    @POST
    @Path("register")
    public LightResponse<TokenPair> register(RegisterRequest request) {
        return LightResponse.ok(getAuthService().register(request));
    }

    @POST
    @Path("login")
    public LightResponse<TokenPair> login(LoginRequest request) {
        return LightResponse.ok(getAuthService().login(request));
    }

    @POST
    @Path("refresh")
    public LightResponse<TokenPair> refresh(RefreshTokenRequest request) {
        return LightResponse.ok(getAuthService().refreshToken(request));
    }

    @POST
    @Path("social/{provider}")
    public LightResponse<TokenPair> socialLogin(
            @PathParam("provider") String provider,
            SocialLoginRequest request
    ) {
        return LightResponse.ok(getAuthService().socialLogin(provider, request));
    }

    @POST
    @Path("logout")
    public LightResponse<Map<String, String>> logout(RefreshTokenRequest request) {
        getAuthService().logout(request);
        return LightResponse.ok(Map.of("message", "Logged out"));
    }

    @POST
    @Path("forgot-password")
    public LightResponse<Map<String, String>> forgotPassword(ForgotPasswordRequest request) {
        getAuthService().forgotPassword(request);
        return LightResponse.ok(Map.of("message", "If the email exists, a reset link has been sent"));
    }

    @POST
    @Path("reset-password")
    public LightResponse<Map<String, String>> resetPassword(ResetPasswordRequest request) {
        getAuthService().resetPassword(request);
        return LightResponse.ok(Map.of("message", "Password has been reset"));
    }
}
