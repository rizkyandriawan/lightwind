package dev.kakrizky.lightwind.realtime;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "lightwind.realtime")
public interface RealtimeConfig {

    @WithDefault("true")
    boolean enabled();

    @WithDefault("true")
    boolean stompEnabled();

    @WithDefault("true")
    boolean sseEnabled();

    @WithDefault("10000")
    long heartbeatIntervalMs();

    @WithDefault("300")
    long sseReconnectDelayMs();

    @WithDefault("1000")
    int maxConnectionsPerUser();
}
