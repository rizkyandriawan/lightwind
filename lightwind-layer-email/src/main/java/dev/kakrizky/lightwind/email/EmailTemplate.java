package dev.kakrizky.lightwind.email;

import java.util.Map;

/**
 * Utility class providing pre-built {@link EmailRequest} instances for
 * common transactional emails.
 *
 * <p>Each method returns a fully constructed request using inline HTML.
 * For customisation, use Qute templates instead via
 * {@link EmailRequest#template(String, Map)}.</p>
 */
public final class EmailTemplate {

    private EmailTemplate() {
        // utility class
    }

    /**
     * Builds a welcome email for a newly registered user.
     */
    public static EmailRequest welcome(String toEmail, String userName, String loginUrl) {
        String html = """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: sans-serif; color: #333; max-width: 600px; margin: 0 auto;">
                  <h2 style="color: #2563eb;">Welcome to Lightwind!</h2>
                  <p>Hi %s,</p>
                  <p>Thank you for creating your account. We are excited to have you on board.</p>
                  <p>
                    <a href="%s"
                       style="display: inline-block; padding: 12px 24px; background: #2563eb; color: #fff;
                              text-decoration: none; border-radius: 6px;">
                      Sign In
                    </a>
                  </p>
                  <p style="color: #666; font-size: 14px;">If the button does not work, copy and paste this URL into your browser:<br>%s</p>
                  <hr style="border: none; border-top: 1px solid #e5e7eb; margin-top: 32px;">
                  <p style="color: #999; font-size: 12px;">This email was sent by Lightwind App.</p>
                </body>
                </html>
                """.formatted(escapeHtml(userName), escapeHtml(loginUrl), escapeHtml(loginUrl));

        return EmailRequest.builder()
                .to(toEmail)
                .subject("Welcome to Lightwind!")
                .htmlBody(html);
    }

    /**
     * Builds a password-reset email with a time-limited reset link.
     */
    public static EmailRequest resetPassword(String toEmail, String userName, String resetUrl) {
        String html = """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: sans-serif; color: #333; max-width: 600px; margin: 0 auto;">
                  <h2 style="color: #2563eb;">Reset Your Password</h2>
                  <p>Hi %s,</p>
                  <p>We received a request to reset your password. Click the button below to choose a new one.</p>
                  <p>
                    <a href="%s"
                       style="display: inline-block; padding: 12px 24px; background: #2563eb; color: #fff;
                              text-decoration: none; border-radius: 6px;">
                      Reset Password
                    </a>
                  </p>
                  <p style="color: #666; font-size: 14px;">If you did not request this, you can safely ignore this email.</p>
                  <p style="color: #666; font-size: 14px;">If the button does not work, copy and paste this URL into your browser:<br>%s</p>
                  <hr style="border: none; border-top: 1px solid #e5e7eb; margin-top: 32px;">
                  <p style="color: #999; font-size: 12px;">This email was sent by Lightwind App.</p>
                </body>
                </html>
                """.formatted(escapeHtml(userName), escapeHtml(resetUrl), escapeHtml(resetUrl));

        return EmailRequest.builder()
                .to(toEmail)
                .subject("Reset Your Password")
                .htmlBody(html);
    }

    /**
     * Builds an email-verification message with a confirmation link.
     */
    public static EmailRequest emailVerification(String toEmail, String userName, String verifyUrl) {
        String html = """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: sans-serif; color: #333; max-width: 600px; margin: 0 auto;">
                  <h2 style="color: #2563eb;">Verify Your Email</h2>
                  <p>Hi %s,</p>
                  <p>Please verify your email address by clicking the button below.</p>
                  <p>
                    <a href="%s"
                       style="display: inline-block; padding: 12px 24px; background: #2563eb; color: #fff;
                              text-decoration: none; border-radius: 6px;">
                      Verify Email
                    </a>
                  </p>
                  <p style="color: #666; font-size: 14px;">If you did not create an account, you can safely ignore this email.</p>
                  <p style="color: #666; font-size: 14px;">If the button does not work, copy and paste this URL into your browser:<br>%s</p>
                  <hr style="border: none; border-top: 1px solid #e5e7eb; margin-top: 32px;">
                  <p style="color: #999; font-size: 12px;">This email was sent by Lightwind App.</p>
                </body>
                </html>
                """.formatted(escapeHtml(userName), escapeHtml(verifyUrl), escapeHtml(verifyUrl));

        return EmailRequest.builder()
                .to(toEmail)
                .subject("Verify Your Email Address")
                .htmlBody(html);
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
