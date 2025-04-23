package util.email;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.activation.DataHandler;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.util.ByteArrayDataSource;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.file.FileService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private final String username = "uiniacad.dev@gmail.com";
    private final String password = "uxgo qecc roxv okxh";
    private final Properties smtpProperties;
    private final Cache<String, byte[]> resourceCache;
    private final ExecutorService executorService;
    private final Bucket rateLimiter;

    /**
     * Constructs a MailService with SMTP credentials and rate limiting.
     */
    public MailService() {
        // SMTP configuration
        this.smtpProperties = new Properties();
        smtpProperties.put("mail.smtp.auth", SMTP_AUTH);
        smtpProperties.put("mail.smtp.host", SMTP_HOST);
        smtpProperties.put("mail.smtp.port", SMTP_PORT);
        smtpProperties.put("mail.smtp.starttls.enable", SMTP_STARTTLS);

        // Resource cache with size limit and expiration
        this.resourceCache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(Duration.ofHours(1))
                .build();

        // Thread pool for sending emails
        this.executorService = Executors.newFixedThreadPool(4);

        // Rate limiter: 100 emails per minute
        Bandwidth limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)));
        this.rateLimiter = Bucket.builder().addLimit(limit).build();
    }

    /**
     * Sends personalized HTML emails to recipients with embedded images and optional attachments.
     *
     * @param htmlPath    Path to the HTML template in resources
     * @param subject     Email subject
     * @param variables   Map of recipient email to their specific variables
     * @param imageMap    Map of image resource paths to their Content-IDs
     * @param attachments Optional list of attachments
     * @return List of recipients for whom email sending failed
     * @throws IOException        If HTML template or image loading fails
     * @throws MessagingException If email configuration is invalid
     */
    public List<String> sendPersonalized(
            String htmlPath,
            String subject,
            Map<String, Map<String, String>> variables,
            Map<String, String> imageMap,
            List<Attachment> attachments) throws IOException, MessagingException {

        if (variables == null || variables.isEmpty()) {
            throw new IllegalArgumentException("Variables map cannot be null or empty");
        }

        // Load and validate HTML template
        String htmlTemplate = FileService.readFileFromResources(htmlPath);
        validateHtmlTemplate(htmlTemplate);
        Set<String> placeholders = extractPlaceholders(htmlTemplate);

        // Validate placeholders for each recipient
        for (String recipient : variables.keySet()) {
            Map<String, String> recipientVars = variables.get(recipient);
            for (String placeholder : placeholders) {
                if (!recipientVars.containsKey(placeholder) && !placeholder.equals("baseUrl")) {
                    logger.warn("Missing variable '{}' for recipient '{}'. Using empty string.", placeholder, recipient);
                }
            }
        }

        // Send emails using multiple threads
        List<String> failedRecipients = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String recipient : variables.keySet()) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // Apply rate limiting
                    rateLimiter.asBlocking().consume(1);

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

                    // Retry mechanism for sending email
                    int maxRetries = 3;
                    for (int attempt = 1; attempt <= maxRetries; attempt++) {
                        try {
                            message.setContent(multipart);
                            Transport.send(message);
                            logger.info("Email sent successfully to {}", recipient);
                            break;
                        } catch (MessagingException e) {
                            if (attempt == maxRetries) {
                                logger.error("Failed to send email to {} after {} attempts", recipient, maxRetries, e);
                                failedRecipients.add(recipient);
                            } else {
                                try {
                                    Thread.sleep(2000 * attempt); // Exponential backoff
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                        }
                    }
                } catch (AddressException e) {
                    logger.warn("Invalid email address: {}", recipient, e);
                    failedRecipients.add(recipient);
                } catch (MessagingException | IOException e) {
                    logger.error("Failed to send email to {}", recipient, e);
                    failedRecipients.add(recipient);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }, executorService);

            futures.add(future);
        }

        // Wait for all emails to be sent
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return failedRecipients;
    }

    /**
     * Validates the HTML template using Jsoup.
     *
     * @param htmlTemplate HTML template content
     * @throws IllegalArgumentException If HTML is invalid
     */
    private void validateHtmlTemplate(String htmlTemplate) {
        try {
            Jsoup.parse(htmlTemplate);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid HTML template: " + e.getMessage());
        }
    }

    /**
     * Loads and caches a resource (HTML or image) from the classpath.
     *
     * @param resourcePath Path to the resource
     * @return Resource content as byte array
     * @throws IOException If resource loading fails
     */
    private byte[] loadResource(String resourcePath) throws IOException {
        byte[] cachedContent = resourceCache.getIfPresent(resourcePath);
        if (cachedContent != null) {
            return cachedContent;
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
     * @param htmlTemplate HTML template with placeholders
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
     * Determines the MIME type based on file extension or content probing.
     *
     * @param filePath Path to the file
     * @return MIME type
     */
    private String getMimeType(String filePath) {
        try {
            String mimeType = Files.probeContentType(Paths.get(filePath));
            return mimeType != null ? mimeType : "application/octet-stream";
        } catch (IOException e) {
            String lowerCasePath = filePath.toLowerCase();
            if (lowerCasePath.endsWith(".png")) return "image/png";
            if (lowerCasePath.endsWith(".jpg") || lowerCasePath.endsWith(".jpeg")) return "image/jpeg";
            if (lowerCasePath.endsWith(".gif")) return "image/gif";
            if (lowerCasePath.endsWith(".pdf")) return "application/pdf";
            if (lowerCasePath.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            return "application/octet-stream";
        }
    }

    /**
     * Shuts down the executor service gracefully.
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
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
            MailService mailService = new MailService();

            // Personalized variables for recipients
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

            // Embedded images
            Map<String, String> imageMap = Map.of(
                    "img/1cd2ff272e2531b8041264de38db1b5f.png", "banner",
                    "img/4d3b20f647cbdeb288013a15cce39fdf.jpg", "icon"
            );

            // Attachments
            byte[] pdfBytes = "Sample PDF content".getBytes();
            List<Attachment> attachments = List.of(
                    new Attachment("document.pdf", pdfBytes, "pdf/demo.pdf")
            );

            // Send emails
            List<String> failedRecipients = mailService.sendPersonalized(
                    "templates/email.html",
                    "Welcome to Our Service",
                    variables,
                    imageMap,
                    attachments
            );

            // Report results
            if (failedRecipients.isEmpty()) {
                System.out.println("All emails sent successfully!");
            } else {
                System.out.println("Failed to send emails to: " + failedRecipients);
            }

            // Cleanup
            mailService.shutdown();
        } catch (IOException | MessagingException e) {
            logger.error("Demo failed", e);
            System.out.println("Demo failed: " + e.getMessage());
        }
    }
}