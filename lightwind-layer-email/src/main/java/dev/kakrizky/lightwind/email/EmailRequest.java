package dev.kakrizky.lightwind.email;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder-pattern class for constructing email messages.
 *
 * <p>Supports plain text, raw HTML, Qute template rendering,
 * and file attachments. Use the fluent API to build a request
 * then pass it to {@link LightMailService}.</p>
 */
public class EmailRequest {

    private final List<String> to = new ArrayList<>();
    private final List<String> cc = new ArrayList<>();
    private final List<String> bcc = new ArrayList<>();
    private String subject;
    private String textBody;
    private String htmlBody;
    private String templateName;
    private Map<String, Object> templateData;
    private final List<Attachment> attachments = new ArrayList<>();

    /**
     * Attachment record holding binary content with name and MIME type.
     */
    public record Attachment(String name, byte[] content, String contentType) {
    }

    private EmailRequest() {
    }

    /**
     * Creates a new EmailRequest builder.
     */
    public static EmailRequest builder() {
        return new EmailRequest();
    }

    public EmailRequest to(String... addresses) {
        Collections.addAll(this.to, addresses);
        return this;
    }

    public EmailRequest cc(String... addresses) {
        Collections.addAll(this.cc, addresses);
        return this;
    }

    public EmailRequest bcc(String... addresses) {
        Collections.addAll(this.bcc, addresses);
        return this;
    }

    public EmailRequest subject(String subject) {
        this.subject = subject;
        return this;
    }

    public EmailRequest textBody(String body) {
        this.textBody = body;
        return this;
    }

    public EmailRequest htmlBody(String body) {
        this.htmlBody = body;
        return this;
    }

    /**
     * Render the email body from a Qute template.
     *
     * @param templateName template file name (relative to the configured template path)
     * @param data         key-value pairs passed to the template
     */
    public EmailRequest template(String templateName, Map<String, Object> data) {
        this.templateName = templateName;
        this.templateData = new HashMap<>(data);
        return this;
    }

    public EmailRequest attachment(String name, byte[] content, String contentType) {
        this.attachments.add(new Attachment(name, content, contentType));
        return this;
    }

    // --- Getters ---

    public List<String> getTo() {
        return to;
    }

    public List<String> getCc() {
        return cc;
    }

    public List<String> getBcc() {
        return bcc;
    }

    public String getSubject() {
        return subject;
    }

    public String getTextBody() {
        return textBody;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public String getTemplateName() {
        return templateName;
    }

    public Map<String, Object> getTemplateData() {
        return templateData;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public boolean hasTemplate() {
        return templateName != null && !templateName.isBlank();
    }
}
