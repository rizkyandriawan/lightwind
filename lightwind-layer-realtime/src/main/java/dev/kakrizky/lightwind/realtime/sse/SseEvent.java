package dev.kakrizky.lightwind.realtime.sse;

import java.util.UUID;

public class SseEvent {

    private String id;
    private String name;
    private String data;
    private Long retry;

    public SseEvent() {
        this.id = UUID.randomUUID().toString();
    }

    public SseEvent(String name, String data) {
        this();
        this.name = name;
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Long getRetry() {
        return retry;
    }

    public void setRetry(Long retry) {
        this.retry = retry;
    }
}
