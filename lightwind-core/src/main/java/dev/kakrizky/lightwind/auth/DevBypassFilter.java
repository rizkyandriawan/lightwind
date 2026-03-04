package dev.kakrizky.lightwind.auth;

import dev.kakrizky.lightwind.config.LightwindConfig;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.security.Principal;
import java.util.UUID;

@Provider
@Priority(Priorities.AUTHENTICATION - 1)
@ApplicationScoped
public class DevBypassFilter implements ContainerRequestFilter {

    private static final UUID DEV_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String DEV_USER_NAME = "dev-admin";

    @Inject
    LightwindConfig config;

    @Override
    public void filter(ContainerRequestContext ctx) {
        if (!config.auth().bypassEnabled()) return;
        if (ctx.getSecurityContext().getUserPrincipal() != null) return;

        ctx.setSecurityContext(new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return () -> DEV_USER_NAME;
            }

            @Override
            public boolean isUserInRole(String role) {
                return true;
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public String getAuthenticationScheme() {
                return "DEV_BYPASS";
            }
        });
    }
}
