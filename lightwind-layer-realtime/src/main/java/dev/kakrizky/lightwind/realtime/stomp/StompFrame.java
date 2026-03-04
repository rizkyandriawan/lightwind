package dev.kakrizky.lightwind.realtime.stomp;

import java.util.LinkedHashMap;
import java.util.Map;

public class StompFrame {

    private StompCommand command;
    private Map<String, String> headers;
    private String body;

    public StompFrame() {
        this.headers = new LinkedHashMap<>();
    }

    public StompFrame(StompCommand command) {
        this.command = command;
        this.headers = new LinkedHashMap<>();
    }

    public StompCommand getCommand() {
        return command;
    }

    public void setCommand(StompCommand command) {
        this.command = command;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getHeader(String key) {
        return headers.get(key);
    }

    public void setHeader(String key, String value) {
        headers.put(key, value);
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getDestination() {
        return getHeader("destination");
    }

    public String getId() {
        return getHeader("id");
    }

    public String getReceipt() {
        return getHeader("receipt");
    }
}
