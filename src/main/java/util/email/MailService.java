package util.email;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.file.FileService;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * Service for sending HTML emails using Gmail SMTP.
 */
public class MailService {
    private static final Logger logger = LoggerFactory.getLogger(MailService.class);
    private static final String HOST = "smtp.gmail.com";
    private static final int PORT = 587;
    private static final String BASE_URL = "http://localhost:9090/UniAcad_war_exploded";

    private final String username;
    private final String password;
    private final Properties smtpProperties;
    private final Properties oauthProperties;

    /**
     * Constructs a MailService with Gmail credentials loaded from oauth.properties.
     */
    public MailService() {
        this.username = "uiniacad.dev@gmail.com";
        this.password = "uxgo qecc roxv okxh"; // Consider using environment variables

        this.smtpProperties = new Properties();
        smtpProperties.put("mail.smtp.auth", "true");
        smtpProperties.put("mail.smtp.host", HOST);
        smtpProperties.put("mail.smtp.port", PORT);
        smtpProperties.put("mail.smtp.starttls.enable", "true");

        this.oauthProperties = new Properties();
        try {
            oauthProperties.load(getClass().getResourceAsStream("/oauth.properties"));
        } catch (IOException e) {
            logger.error("Failed to load oauth.properties", e);
            throw new RuntimeException("Configuration error", e);
        }
    }

    /**
     * Sends an HTML email to a list of recipients.
     *
     * @param recipients List of recipient email addresses
     * @param subject    Email subject
     * @param content    HTML content or verification token
     * @param isVerify   True if sending verification email, false for generic HTML
     * @throws MessagingException If email sending fails
     */
    public void sendEmail(List<String> recipients, String subject, String content, boolean isVerify) throws MessagingException {
        InternetAddress[] addresses = recipients.stream()
                .map(email -> {
                    try {
                        return new InternetAddress(email);
                    } catch (AddressException e) {
                        logger.warn("Invalid email address: {}", email, e);
                        throw new RuntimeException("Invalid email: " + email, e);
                    }
                })
                .toArray(InternetAddress[]::new);

        Session session = Session.getInstance(smtpProperties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setSubject(subject);
        message.setRecipients(Message.RecipientType.TO, addresses);

        String emailContent = isVerify ? buildVerificationEmail(content) : FileService.readFileFromResources(content);
        MimeBodyPart body = new MimeBodyPart();
        body.setContent(emailContent, "text/html; charset=utf-8");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(body);
        message.setContent(multipart);

        Transport.send(message);
        logger.info("Email sent successfully to {}", String.join(", ", recipients));
    }

    /**
     * Builds an HTML email for account verification with a modern, responsive design.
     *
     * @param token Verification token
     * @return HTML email content
     */
    private String buildVerificationEmail(String token) {
        String verificationLink = String.format("%s/verify?token=%s", BASE_URL, token);
        String fromEmail = oauthProperties.getProperty("oauth.email", username);

        return String.format("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Verify Your Email</title>
                    <style>
                        body { margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif; background: #f4f4f9; color: #333; }
                        .container { padding: 20px; }
                        .card { max-width: 600px; margin: 0 auto; background: #fff; border-radius: 16px; overflow: hidden; box-shadow: 0 6px 20px rgba(0,0,0,0.1); }
                        .header { background: linear-gradient(135deg, #4f46e5, #7c3aed); padding: 30px; text-align: center; }
                        .header img { max-width: 120px; height: auto; }
                        .header h1 { margin: 10px 0 0; font-size: 26px; font-weight: 700; color: #fff; }
                        .body { padding: 30px; }
                        .body h2 { font-size: 22px; font-weight: 600; color: #1f2937; margin: 0 0 15px; }
                        .body p { font-size: 16px; line-height: 1.6; color: #4b5563; margin: 0 0 20px; }
                        .button { display: inline-block; padding: 14px 28px; font-size: 16px; font-weight: 600; color: #fff; background: #4f46e5; text-decoration: none; border-radius: 8px; transition: background 0.2s; }
                        .button:hover { background: #4338ca; }
                        .code { font-size: 18px; font-weight: 600; color: #4f46e5; background: #f1f5f9; padding: 10px 15px; border-radius: 6px; display: inline-block; margin: 10px 0; }
                        .footer { background: #f8fafc; padding: 20px; text-align: center; border-top: 1px solid #e2e8f0; }
                        .footer p { font-size: 14px; color: #6b7280; margin: 0 0 5px; }
                        .footer a { color: #4f46e5; text-decoration: none; }
                        .footer a:hover { text-decoration: underline; }
                        @media (max-width: 600px) {
                            .container { padding: 10px; }
                            .card { border-radius: 12px; }
                            .header { padding: 20px; }
                            .header h1 { font-size: 22px; }
                            .body { padding: 20px; }
                            .body h2 { font-size: 20px; }
                            .button { padding: 12px 24px; font-size: 15px; }
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="card">
                            <div class="header">
                                <!-- Replace with your logo URL -->
                                <img src="https://via.placeholder.com/120x40?text=Logo" alt="Logo">
                                <h1>Email Verification</h1>
                            </div>
                            <div class="body">
                                <h2>Welcome Aboard!</h2>
                                <p>You're one step away from activating your account. Click the button below to verify your email address. This link expires in 24 hours.</p>
                                <a href="%s" class="button">Verify Your Email</a>
                                <p>Or use this verification code: <span class="code">%s</span></p>
                                <p>If the button doesn't work, copy and paste this link:</p>
                                <p><a href="%s" style="color: #4f46e5; word-break: break-all;">%s</a></p>
                                <p>Didn't sign up? You can safely ignore this email.</p>
                            </div>
                            <div class="footer">
                                <p>&copy; 2025 UniAcad. All rights reserved.</p>
                                <p>
                                    <a href="%s">Visit our website</a> | 
                                    <a href="mailto:%s">Contact support</a>
                                </p>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """, verificationLink, token, verificationLink, verificationLink, BASE_URL, fromEmail);
    }

    /**
     * Sends a verification email with a token.
     *
     * @param recipients List of recipient email addresses
     * @param subject    Email subject
     * @param token      Verification token
     * @throws MessagingException If email sending fails
     */
    public void sendVerify(List<String> recipients, String subject, String token) throws MessagingException {
        sendEmail(recipients, subject, token, true);
    }

    /**
     * Sends an HTML email using a file from resources.
     *
     * @param recipients List of recipient email addresses
     * @param subject    Email subject
     * @param htmlPath   Path to HTML file in resources
     * @throws MessagingException If email sending fails
     */
    public void send(List<String> recipients, String subject, String htmlPath) throws MessagingException {
        sendEmail(recipients, subject, htmlPath, false);
    }

    public static void main(String[] args) {
        try {
            MailService mailService = new MailService();
            List<String> recipients = List.of("khai1234sd@gmail.com");
            mailService.sendVerify(recipients, "Test Verification", "123456");
        } catch (MessagingException e) {
            logger.error("Failed to send email", e);
        }
    }
}