package dev.kakrizky.lightwind.realtime.messaging;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class RealtimeMessage {

    private String destination;
    private String payload;
    private Map<String, String> headers;
    private String messageId;

    public RealtimeMessage() {
        this.messageId = UUID.randomUUID().toString();
        this.headers = new LinkedHashMap<>();
    }

    public RealtimeMessage(String destination, String payload) {
        this();
        this.destination = destination;
        this.payload = payload;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}
