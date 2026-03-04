package dev.kakrizky.lightwind.build;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Runtime bean providing information about the native image layer.
 * Populated at build time by the deployment processor.
 */
@ApplicationScoped
public class LayerInfo {

    private String lightwindVersion;
    private String baseLayerHash;
    private boolean layeredBuild;
    private int entityCount;
    private int serviceCount;
    private int resourceCount;

    public String getLightwindVersion() {
        return lightwindVersion;
    }

    public void setLightwindVersion(String lightwindVersion) {
        this.lightwindVersion = lightwindVersion;
    }

    public String getBaseLayerHash() {
        return baseLayerHash;
    }

    public void setBaseLayerHash(String baseLayerHash) {
        this.baseLayerHash = baseLayerHash;
    }

    public boolean isLayeredBuild() {
        return layeredBuild;
    }

    public void setLayeredBuild(boolean layeredBuild) {
        this.layeredBuild = layeredBuild;
    }

    public int getEntityCount() {
        return entityCount;
    }

    public void setEntityCount(int entityCount) {
        this.entityCount = entityCount;
    }

    public int getServiceCount() {
        return serviceCount;
    }

    public void setServiceCount(int serviceCount) {
        this.serviceCount = serviceCount;
    }

    public int getResourceCount() {
        return resourceCount;
    }

    public void setResourceCount(int resourceCount) {
        this.resourceCount = resourceCount;
    }

    @Override
    public String toString() {
        return "LayerInfo{" +
                "lightwindVersion='" + lightwindVersion + '\'' +
                ", layeredBuild=" + layeredBuild +
                ", entities=" + entityCount +
                ", services=" + serviceCount +
                ", resources=" + resourceCount +
                '}';
    }
}
