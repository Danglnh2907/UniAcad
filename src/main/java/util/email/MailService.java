package util.email;

import jakarta.activation.DataHandler;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.util.ByteArrayDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.file.FileService;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.*;

/**
 * Service for sending personalized HTML emails with embedded images and attachments using Jakarta EE 11.
 */
public class MailService {
    private static final Logger logger = LoggerFactory.getLogger(MailService.class);
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;
    private static final String SMTP_AUTH = "true";
    private static final String SMTP_STARTTLS = "true";
    private static final String BASE_URL = "http://localhost:9090/UniAcad_war";

    private final String username;
    private final String password;
    private final Properties smtpProperties;
    private final Map<String, byte[]> resourceCache;
    private final ExecutorService executorService;

    /**
     * Constructs a MailService with SMTP credentials.
     *
     * @throws RuntimeException If configuration is invalid
     */
    public MailService() {
        this.username = "uiniacad.dev@gmail.com";
        this.password = "uxgo qecc roxv okxh";

        this.smtpProperties = new Properties();
        smtpProperties.put("mail.smtp.auth", SMTP_AUTH);
        smtpProperties.put("mail.smtp.host", SMTP_HOST);
        smtpProperties.put("mail.smtp.port", SMTP_PORT);
        smtpProperties.put("mail.smtp.starttls.enable", SMTP_STARTTLS);

        this.resourceCache = Collections.synchronizedMap(new HashMap<>());
        this.executorService = Executors.newFixedThreadPool(4); // Giới hạn 4 luồng
    }

    /**
     * Loads and caches a resource (HTML or image) from the classpath.
     *
     * @param resourcePath Path to the resource
     * @return Resource content as byte array
     * @throws IOException If resource loading fails
     */
    private byte[] loadResource(String resourcePath) throws IOException {
        if (resourceCache.containsKey(resourcePath)) {
            return resourceCache.get(resourcePath);
        }
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            byte[] content = stream.readAllBytes();
            resourceCache.put(resourcePath, content);
            return content;
        }
    }

    /**
     * Sends personalized HTML emails to recipients with embedded images and optional attachments using multiple threads.
     *
     * @param htmlPath        Path to the HTML template in resources
     * @param recipients      List of recipient email addresses
     * @param subject         Email subject
     * @param variables       Map of recipient email to their specific variables (e.g., {{name}}, {{token}})
     * @param imageMap        Map of image resource paths to their Content-IDs for embedding
     * @param attachments     Optional list of attachments
     * @return List of recipients for whom email sending failed
     * @throws IOException If HTML template or image loading fails
     * @throws MessagingException If email configuration is invalid
     */
    public List<String> sendPersonalized(
            String htmlPath,
            List<String> recipients,
            String subject,
            Map<String, Map<String, String>> variables,
            Map<String, String> imageMap,
            List<Attachment> attachments) throws IOException, MessagingException {
        if (recipients == null || recipients.isEmpty()) {
            throw new IllegalArgumentException("Recipient list cannot be empty");
        }
        if (htmlPath == null || htmlPath.isBlank()) {
            throw new IllegalArgumentException("HTML template path cannot be empty");
        }
        if (variables == null) {
            throw new IllegalArgumentException("Variables map cannot be null");
        }

        // Load HTML template
        String htmlTemplate = FileService.readFileFromResources(htmlPath);

        // Validate placeholders in HTML
        Set<String> placeholders = extractPlaceholders(htmlTemplate);
        for (String recipient : recipients) {
            Map<String, String> recipientVars = variables.getOrDefault(recipient, Map.of());
            for (String placeholder : placeholders) {
                if (!recipientVars.containsKey(placeholder) && !placeholder.equals("baseUrl")) {
                    logger.warn("Missing variable '{}' for recipient '{}'. Using empty string.", placeholder, recipient);
                }
            }
        }

        // Send emails using multiple threads
        List<String> failedRecipients = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String recipient : recipients) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // Validate recipient email
                    InternetAddress address = new InternetAddress(recipient);

                    // Create personalized email
                    Session session = Session.getInstance(smtpProperties, new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, password);
                        }
                    });

                    MimeMessage message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(username));
                    message.setRecipient(Message.RecipientType.TO, address);
                    message.setSubject(subject);

                    // Create multipart content
                    MimeMultipart multipart = new MimeMultipart("related");

                    // Personalize HTML content
                    String emailContent = personalizeContent(htmlTemplate, variables.getOrDefault(recipient, Map.of()));
                    MimeBodyPart htmlPart = new MimeBodyPart();
                    htmlPart.setContent(emailContent, "text/html; charset=utf-8");
                    multipart.addBodyPart(htmlPart);

                    // Embed images
                    if (imageMap != null) {
                        for (Map.Entry<String, String> image : imageMap.entrySet()) {
                            MimeBodyPart imagePart = new MimeBodyPart();
                            byte[] imageBytes = loadResource(image.getKey());
                            ByteArrayDataSource dataSource = new ByteArrayDataSource(imageBytes, getMimeType(image.getKey()));
                            imagePart.setDataHandler(new DataHandler(dataSource));
                            imagePart.setHeader("Content-ID", "<" + image.getValue() + ">");
                            imagePart.setDisposition(MimeBodyPart.INLINE);
                            multipart.addBodyPart(imagePart);
                        }
                    }

                    // Add attachments
                    if (attachments != null) {
                        for (Attachment attachment : attachments) {
                            MimeBodyPart attachmentPart = new MimeBodyPart();
                            ByteArrayDataSource attachmentSource = new ByteArrayDataSource(attachment.content, attachment.mimeType);
                            attachmentPart.setDataHandler(new DataHandler(attachmentSource));
                            attachmentPart.setFileName(attachment.fileName);
                            multipart.addBodyPart(attachmentPart);
                        }
                    }

                    // Set message content and send
                    message.setContent(multipart);
                    Transport.send(message);
                    logger.info("Email sent successfully to {}", recipient);
                } catch (AddressException e) {
                    logger.warn("Invalid email address: {}", recipient, e);
                    failedRecipients.add(recipient);
                } catch (MessagingException | IOException e) {
                    logger.error("Failed to send email to {}", recipient, e);
                    failedRecipients.add(recipient);
                }
            }, executorService);
            futures.add(future);
        }

        // Wait for all emails to be sent
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return failedRecipients;
    }

    /**
     * Extracts placeholders (e.g., {{name}}) from the HTML template.
     *
     * @param htmlTemplate HTML template content
     * @return Set of placeholder names
     */
    private Set<String> extractPlaceholders(String htmlTemplate) {
        Set<String> placeholders = new HashSet<>();
        Pattern pattern = Pattern.compile("\\{\\{(\\w+)\\}\\}");
        Matcher matcher = pattern.matcher(htmlTemplate);
        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }
        return placeholders;
    }

    /**
     * Personalizes the HTML content by replacing placeholders with recipient-specific variables.
     *
     * @param htmlTemplate HTML template with placeholders (e.g., {{name}})
     * @param variables    Map of placeholder keys to their values
     * @return Personalized HTML content
     */
    private String personalizeContent(String htmlTemplate, Map<String, String> variables) {
        String content = htmlTemplate;
        Set<String> placeholders = extractPlaceholders(htmlTemplate);
        for (String placeholder : placeholders) {
            String value = variables.getOrDefault(placeholder, "");
            if (placeholder.equals("baseUrl")) {
                value = BASE_URL;
            }
            content = content.replace("{{" + placeholder + "}}", value);
        }
        return content;
    }

    /**
     * Determines the MIME type based on file extension.
     *
     * @param filePath Path to the file
     * @return MIME type (e.g., "image/png" or "image/jpeg")
     */
    private String getMimeType(String filePath) {
        String lowerCasePath = filePath.toLowerCase();
        if (lowerCasePath.endsWith(".png")) {
            return "image/png";
        } else if (lowerCasePath.endsWith(".jpg") || lowerCasePath.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }

    /**
     * Represents an email attachment.
     */
    public static class Attachment {
        private final String fileName;
        private final byte[] content;
        private final String mimeType;

        public Attachment(String fileName, byte[] content, String mimeType) {
            this.fileName = fileName;
            this.content = content;
            this.mimeType = mimeType != null ? mimeType : "application/octet-stream";
        }
    }

    /**
     * Demo method to showcase sending personalized emails.
     */
    public static void main(String[] args) {
        try {
            // Initialize MailService
            MailService mailService = new MailService();

            // Danh sách người nhận
            List<String> recipients = List.of(
                    "khai1234sd@gmail.com",
                    "khainhce182286@fpt.edu.vn",
                    "khaiproject1234@gmail.com",
                    "nkhai7018@gmail.com"
            );

            // Biến cá nhân hóa cho mỗi người nhận
            Map<String, Map<String, String>> variables = new HashMap<>();
            variables.put("khai1234sd@gmail.com", Map.of(
                    "name", "Khai Nguyen Hoang",
                    "token", "abc123",
                    "verificationLink", BASE_URL + "/verify?token=abc123"
            ));
            variables.put("khainhce182286@fpt.edu.vn", Map.of(
                    "name", "Jane Smith",
                    "token", "xyz789",
                    "verificationLink", BASE_URL + "/verify?token=xyz789"
            ));
            variables.put("khaiproject1234@gmail.com",Map.of(
                    "name","Khai Project",
                    "token","123456",
                    "verificationLink", BASE_URL + "/verify?token=123456"
            ));
            variables.put("nkhai7018@gmail.com",Map.of(
                    "name","Khai Nguyen",
                    "token","789012",
                    "verificationLink", BASE_URL + "/verify?token=789012"
            ));

            // Danh sách ảnh cần nhúng
            Map<String, String> imageMap = Map.of(
                    "img/1cd2ff272e2531b8041264de38db1b5f.png", "banner",
                    "img/4d3b20f647cbdeb288013a15cce39fdf.jpg", "icon"
            );

            // Tệp đính kèm (giả lập)
            byte[] pdfBytes = "Sample PDF content".getBytes();
            List<Attachment> attachments = List.of(
                    new Attachment("document.pdf", pdfBytes, "pdf/demo.pdf")
            );

            // Gửi email
            List<String> failedRecipients = mailService.sendPersonalized(
                    "templates/email.html",
                    recipients,
                    "Welcome to Our Service",
                    variables,
                    imageMap,
                    attachments
            );

            // Báo cáo kết quả
            if (failedRecipients.isEmpty()) {
                LoggerFactory.getLogger(MailService.class).info("All emails sent successfully");
            } else {
                LoggerFactory.getLogger(MailService.class).warn("Failed to send emails to: {}", failedRecipients);
            }
        } catch (IOException | MessagingException e) {
            logger.error("Demo failed", e);
            LoggerFactory.getLogger("MailService").error("Demo failed", e);
        }
    }
}