package dev.kakrizky.lightwind.auth.dto;

/**
 * Request body for social login.
 * For Google: send the ID token in {@code token}.
 * For GitHub: send the OAuth code in {@code code}.
 */
public class SocialLoginRequest {
    private String token;
    private String code;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
