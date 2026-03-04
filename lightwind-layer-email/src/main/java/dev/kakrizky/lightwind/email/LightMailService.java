package dev.kakrizky.lightwind.email;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

/**
 * Central service for sending emails through Quarkus Mailer.
 *
 * <p>Supports synchronous and asynchronous sending, Qute template
 * rendering, attachments, and bulk operations.</p>
 */
@ApplicationScoped
public class LightMailService {

    private static final Logger LOG = Logger.getLogger(LightMailService.class);

    @Inject
    Mailer mailer;

    @Inject
    ReactiveMailer reactiveMailer;

    @Inject
    Engine quteEngine;

    @Inject
    EmailConfig emailConfig;

    /**
     * Sends an email synchronously. Blocks until the mail server accepts the message.
     */
    public void send(EmailRequest request) {
        Mail mail = buildMail(request);
        mailer.send(mail);
        LOG.debugf("Email sent synchronously to %s — subject: %s", request.getTo(), request.getSubject());
    }

    /**
     * Sends an email asynchronously. Returns immediately; delivery happens in the background.
     */
    public void sendAsync(EmailRequest request) {
        Mail mail = buildMail(request);
        reactiveMailer.send(mail)
                .subscribe().with(
                        success -> LOG.debugf("Email sent asynchronously to %s — subject: %s",
                                request.getTo(), request.getSubject()),
                        failure -> LOG.errorf(failure, "Failed to send async email to %s — subject: %s",
                                request.getTo(), request.getSubject())
                );
    }

    /**
     * Sends multiple emails synchronously.
     */
    public void sendBulk(List<EmailRequest> requests) {
        for (EmailRequest request : requests) {
            try {
                send(request);
            } catch (Exception e) {
                LOG.errorf(e, "Failed to send bulk email to %s — subject: %s",
                        request.getTo(), request.getSubject());
            }
        }
    }

    // --- Internal helpers ---

    private Mail buildMail(EmailRequest request) {
        Mail mail = new Mail();

        // From
        mail.setFrom(emailConfig.fromName() + " <" + emailConfig.fromAddress() + ">");

        // Recipients
        mail.setTo(request.getTo());
        if (!request.getCc().isEmpty()) {
            mail.setCc(request.getCc());
        }
        if (!request.getBcc().isEmpty()) {
            mail.setBcc(request.getBcc());
        }

        // Subject
        mail.setSubject(request.getSubject());

        // Body — template takes precedence over raw HTML, which takes precedence over plain text
        if (request.hasTemplate()) {
            String html = renderTemplate(request.getTemplateName(), request.getTemplateData());
            mail.setHtml(html);
        } else if (request.getHtmlBody() != null) {
            mail.setHtml(request.getHtmlBody());
        } else if (request.getTextBody() != null) {
            mail.setText(request.getTextBody());
        }

        // Attachments
        for (EmailRequest.Attachment attachment : request.getAttachments()) {
            mail.addAttachment(attachment.name(), attachment.content(), attachment.contentType());
        }

        return mail;
    }

    private String renderTemplate(String templateName, Map<String, Object> data) {
        String templatePath = emailConfig.templatePath().orElse("emails/") + templateName;
        Template template = quteEngine.getTemplate(templatePath);
        if (template == null) {
            throw new IllegalArgumentException("Email template not found: " + templatePath);
        }

        var instance = template.instance();
        if (data != null) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                instance = instance.data(entry.getKey(), entry.getValue());
            }
        }
        return instance.render();
    }
}
