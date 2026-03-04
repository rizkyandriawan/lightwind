package dev.kakrizky.lightwind.auth.social;

/**
 * User info returned from a social auth provider after token verification.
 */
public class SocialUser {

    private String provider;
    private String providerId;
    private String email;
    private String displayName;
    private String avatarUrl;
    private boolean emailVerified;

    public SocialUser() {}

    public SocialUser(String provider, String providerId, String email,
                      String displayName, String avatarUrl, boolean emailVerified) {
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.emailVerified = emailVerified;
    }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
}
