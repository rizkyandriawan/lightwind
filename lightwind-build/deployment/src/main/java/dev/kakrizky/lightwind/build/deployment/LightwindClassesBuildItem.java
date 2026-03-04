package dev.kakrizky.lightwind.build.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

import java.util.Collections;
import java.util.Set;

/**
 * Build item containing all discovered Lightwind app classes.
 * Produced by {@link LightwindProcessor} and consumed by
 * {@link LightwindNativeLayerProcessor} for layered image configuration.
 */
public final class LightwindClassesBuildItem extends SimpleBuildItem {

    private final Set<String> entityClasses;
    private final Set<String> dtoClasses;
    private final Set<String> serviceClasses;
    private final Set<String> resourceClasses;

    public LightwindClassesBuildItem(
            Set<String> entityClasses,
            Set<String> dtoClasses,
            Set<String> serviceClasses,
            Set<String> resourceClasses) {
        this.entityClasses = Collections.unmodifiableSet(entityClasses);
        this.dtoClasses = Collections.unmodifiableSet(dtoClasses);
        this.serviceClasses = Collections.unmodifiableSet(serviceClasses);
        this.resourceClasses = Collections.unmodifiableSet(resourceClasses);
    }

    public Set<String> getEntityClasses() {
        return entityClasses;
    }

    public Set<String> getDtoClasses() {
        return dtoClasses;
    }

    public Set<String> getServiceClasses() {
        return serviceClasses;
    }

    public Set<String> getResourceClasses() {
        return resourceClasses;
    }

    public int totalAppClasses() {
        return entityClasses.size() + dtoClasses.size()
                + serviceClasses.size() + resourceClasses.size();
    }
}
