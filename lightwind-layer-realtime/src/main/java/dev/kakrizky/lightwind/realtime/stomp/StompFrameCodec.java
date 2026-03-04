package dev.kakrizky.lightwind.realtime.stomp;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StompFrameCodec {

    private static final char NULL_CHAR = '\0';

    public StompFrame decode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        // Strip trailing NULL character
        String data = raw.endsWith(String.valueOf(NULL_CHAR))
                ? raw.substring(0, raw.length() - 1)
                : raw;

        // Split into header block and body at first blank line
        int headerEnd = data.indexOf("\n\n");
        if (headerEnd == -1) {
            headerEnd = data.indexOf("\r\n\r\n");
        }

        String headerBlock;
        String body = null;
        if (headerEnd >= 0) {
            headerBlock = data.substring(0, headerEnd);
            int bodyStart = data.charAt(headerEnd) == '\r' ? headerEnd + 4 : headerEnd + 2;
            if (bodyStart < data.length()) {
                body = data.substring(bodyStart);
            }
        } else {
            headerBlock = data;
        }

        // First line is the command
        String[] lines = headerBlock.split("\\r?\\n");
        if (lines.length == 0) {
            return null;
        }

        StompFrame frame = new StompFrame();
        try {
            frame.setCommand(StompCommand.valueOf(lines[0].trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return null;
        }

        // Remaining lines are headers
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            int colon = line.indexOf(':');
            if (colon > 0) {
                String key = line.substring(0, colon);
                String value = line.substring(colon + 1);
                frame.setHeader(key, value);
            }
        }

        frame.setBody(body);
        return frame;
    }

    public String encode(StompFrame frame) {
        StringBuilder sb = new StringBuilder();
        sb.append(frame.getCommand().name()).append('\n');

        for (var entry : frame.getHeaders().entrySet()) {
            sb.append(entry.getKey()).append(':').append(entry.getValue()).append('\n');
        }
        sb.append('\n');

        if (frame.getBody() != null) {
            sb.append(frame.getBody());
        }
        sb.append(NULL_CHAR);

        return sb.toString();
    }
}
