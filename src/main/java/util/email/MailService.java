package util.email;

import jakarta.activation.DataHandler;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.file.FileService;
import jakarta.mail.util.ByteArrayDataSource;

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
    private static final String VERIFICATION_TEMPLATE_PATH = "templates/verification-email.html";

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
     * Sends an HTML email to a list of recipients with embedded images.
     *
     * @param recipients List of recipient email addresses
     * @param subject    Email subject
     * @param content    HTML content or verification token
     * @param isVerify   True if sending verification email, false for generic HTML
     * @throws MessagingException If email sending fails
     * @throws IOException If image loading fails
     */
    public void sendEmail(List<String> recipients, String subject, String content, boolean isVerify) throws MessagingException, IOException {
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

        // Create a multipart object to hold HTML and images
        MimeMultipart multipart = new MimeMultipart("related");

        // Load and add the HTML content
        String emailContent = isVerify ? buildVerificationEmail(content) : FileService.readFileFromResources(content);
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(emailContent, "text/html; charset=utf-8");
        multipart.addBodyPart(htmlPart);

        // List of images to embed
        String[] imagePaths = {
                "img/948015252763872ed01b79cbbbb7c68b.png", // Banner image
                "img/f8d71b6c42f7300871f9e091c6a737e3.jpg", // Email icon
                "img/4d3b20f647cbdeb288013a15cce39fdf.jpg", // Text icon
                "img/1cd2ff272e2531b8041264de38db1b5f.png", // X icon
                "img/51a2644c1491853d60a9688ed8f4fa9e.png", // Instagram icon
                "img/7575b9251670cd15f3423fd911239179.png"  // Facebook icon
        };
        String[] contentIds = {
                "banner",
                "email_icon",
                "text_icon",
                "x_icon",
                "instagram_icon",
                "facebook_icon"
        };

        // Embed each image
        for (int i = 0; i < imagePaths.length; i++) {
            MimeBodyPart imagePart = new MimeBodyPart();
            byte[] imageBytes = getClass().getClassLoader().getResourceAsStream(imagePaths[i]).readAllBytes();
            ByteArrayDataSource dataSource = new ByteArrayDataSource(imageBytes, getMimeType(imagePaths[i]));
            imagePart.setDataHandler(new DataHandler(dataSource));
            imagePart.setHeader("Content-ID", "<" + contentIds[i] + ">");
            imagePart.setDisposition(MimeBodyPart.INLINE);
            multipart.addBodyPart(imagePart);
        }

        // Set the multipart content in the message
        message.setContent(multipart);

        Transport.send(message);
        logger.info("Email sent successfully to {}", String.join(", ", recipients));
    }

    /**
     * Builds an HTML email for account verification by loading a template and injecting dynamic values.
     *
     * @param token Verification token
     * @return HTML email content
     */
    private String buildVerificationEmail(String token) {
        String verificationLink = String.format("%s/verify?token=%s", BASE_URL, token);
        String fromEmail = oauthProperties.getProperty("oauth.email", username);

        try {
            String template = FileService.readFileFromResources(VERIFICATION_TEMPLATE_PATH);
            return template
                    .replace("{{verificationLink}}", verificationLink)
                    .replace("{{fromEmail}}", fromEmail)
                    .replace("{{baseUrl}}", BASE_URL);
        } catch (Exception e) {
            logger.error("Failed to load or process verification email template", e);
            throw new RuntimeException("Cannot load verification email template", e);
        }
    }

    /**
     * Determines the MIME type based on file extension.
     *
     * @param filePath Path to the image file
     * @return MIME type (e.g., "image/png" or "image/jpeg")
     */
    private String getMimeType(String filePath) {
        if (filePath.toLowerCase().endsWith(".png")) {
            return "image/png";
        } else if (filePath.toLowerCase().endsWith(".jpg") || filePath.toLowerCase().endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }

    /**
     * Sends a verification email with a token.
     *
     * @param recipients List of recipient email addresses
     * @param subject    Email subject
     * @param token      Verification token
     * @throws MessagingException If email sending fails
     * @throws IOException If image loading fails
     */
    public void sendVerify(List<String> recipients, String subject, String token) throws MessagingException, IOException {
        sendEmail(recipients, subject, token, true);
    }

    /**
     * Sends an HTML email using a file from resources.
     *
     * @param recipients List of recipient email addresses
     * @param subject    Email subject
     * @param htmlPath   Path to HTML file in resources
     * @throws MessagingException If email sending fails
     * @throws IOException If image loading fails
     */
    public void send(List<String> recipients, String subject, String htmlPath) throws MessagingException, IOException {
        sendEmail(recipients, subject, htmlPath, false);
    }

    public static void main(String[] args) {
        try {
            MailService mailService = new MailService();
            List<String> recipients = List.of("khai1234sd@gmail.com");
            mailService.sendVerify(recipients, "Test Verification", "123456");
        } catch (MessagingException | IOException e) {
            logger.error("Failed to send email", e);
        }
    }
}