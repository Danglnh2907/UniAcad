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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import io.github.cdimascio.dotenv.Dotenv;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Enhanced MailService with advanced PDF handling and configuration from save.env.
 */
public class MailService {
    private static final Logger logger = LoggerFactory.getLogger(MailService.class);
    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int MAX_IMAGE_SIZE_BYTES = 2 * 1024 * 1024; // 2MB
    private static final long MAX_PDF_SIZE_BYTES = 10 * 1024 * 1024; // 10MB

    private final String username;
    private final String password;
    private final Properties smtpProperties;
    private final Cache<String, byte[]> resourceCache;
    private final ExecutorService executorService;
    private final BlockingQueue<Runnable> emailQueue;
    private final Bucket rateLimiter;
    private final TemplateEngine templateEngine;
    private final String baseUrl;

    /**
     * Constructs a MailService with configuration from save.env.
     */
    public MailService() {
        // Load configuration from save.env
        Dotenv dotenv = Dotenv.configure()
                .filename("save.env")
                .ignoreIfMissing()
                .load();

        this.username = dotenv.get("SMTP_USERNAME");
        this.password = dotenv.get("SMTP_PASSWORD");
        String smtpHost = dotenv.get("SMTP_HOST", "smtp.gmail.com");
        String smtpPortStr = dotenv.get("SMTP_PORT", "587");
        this.baseUrl = dotenv.get("APP_BASE_URL", "http://localhost:9090/UniAcad_war");

        // Validate required configurations
        if (this.username == null || this.username.trim().isEmpty()) {
            throw new IllegalStateException("SMTP_USERNAME is required in save.env");
        }
        if (this.password == null || this.password.trim().isEmpty()) {
            throw new IllegalStateException("SMTP_PASSWORD is required in save.env");
        }

        int smtpPort;
        try {
            smtpPort = Integer.parseInt(smtpPortStr);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid SMTP_PORT in save.env: " + smtpPortStr, e);
        }

        // Log loaded configuration (mask password)
        logger.info("Loaded SMTP configuration: username={}, host={}, port={}, baseUrl={}",
                username, smtpHost, smtpPort, baseUrl);

        // SMTP configuration
        this.smtpProperties = new Properties();
        smtpProperties.put("mail.smtp.auth", "true");
        smtpProperties.put("mail.smtp.host", smtpHost);
        smtpProperties.put("mail.smtp.port", smtpPort);
        smtpProperties.put("mail.smtp.starttls.enable", "true");

        // Resource cache with larger size and manual cleanup
        this.resourceCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofHours(2))
                .build();

        // Thread pool and email queue
        this.executorService = Executors.newFixedThreadPool(8);
        this.emailQueue = new LinkedBlockingQueue<>();
        startEmailQueueProcessor();

        // Rate limiter: 200 emails per minute
        Bandwidth limit = Bandwidth.classic(200, Refill.greedy(200, Duration.ofMinutes(1)));
        this.rateLimiter = Bucket.builder().addLimit(limit).build();

        // Initialize Thymeleaf template engine
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
    }

    /**
     * Sends personalized HTML emails with PDF attachments specified by resource paths.
     *
     * @param templatePath Path to the Thymeleaf template in resources
     * @param subject      Email subject
     * @param variables    Map of recipient email to their specific variables
     * @param imageMap     Map of image resource paths to their Content-IDs
     * @param pdfPaths     List of PDF resource paths to attach
     * @param batchSize    Number of emails per batch
     * @return Map of failed recipients with failure reasons
     * @throws IOException If template or resource loading fails
     */
    public Map<String, String> sendPersonalizedWithPDF(
            String templatePath,
            String subject,
            Map<String, Map<String, Object>> variables,
            Map<String, String> imageMap,
            List<String> pdfPaths,
            int batchSize) throws IOException {

        // Load and validate PDF attachments
        List<Attachment> attachments = loadPDFAttachments(pdfPaths);

        // Delegate to the main sendPersonalized method
        return sendPersonalized(templatePath, subject, variables, imageMap, attachments, batchSize);
    }

    /**
     * Sends personalized HTML emails to recipients with embedded images and attachments.
     */
    public Map<String, String> sendPersonalized(
            String templatePath,
            String subject,
            Map<String, Map<String, Object>> variables,
            Map<String, String> imageMap,
            List<Attachment> attachments,
            int batchSize) throws IOException {

        if (variables == null || variables.isEmpty()) {
            throw new IllegalArgumentException("Variables map cannot be null or empty");
        }

        // Validate template
        validateTemplate(templatePath);

        // Split recipients into batches
        batchSize = batchSize <= 0 ? DEFAULT_BATCH_SIZE : batchSize;
        List<List<String>> recipientBatches = partitionRecipients(variables.keySet(), batchSize);
        Map<String, String> failedRecipients = new ConcurrentHashMap<>();

        // Process each batch
        for (List<String> batch : recipientBatches) {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (String recipient : batch) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        // Apply rate limiting
                        rateLimiter.asBlocking().consume(1);

                        // Validate recipient email
                        InternetAddress address = new InternetAddress(recipient);
                        address.validate();

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

                        // Personalize HTML content with Thymeleaf
                        String emailContent = personalizeContent(templatePath, variables.getOrDefault(recipient, Map.of()));
                        MimeBodyPart htmlPart = new MimeBodyPart();
                        htmlPart.setContent(emailContent, "text/html; charset=utf-8");
                        multipart.addBodyPart(htmlPart);

                        // Embed compressed images
                        if (imageMap != null) {
                            for (Map.Entry<String, String> image : imageMap.entrySet()) {
                                MimeBodyPart imagePart = new MimeBodyPart();
                                byte[] imageBytes = compressImageIfNeeded(loadResource(image.getKey()));
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
                                validateAttachment(attachment);
                                MimeBodyPart attachmentPart = new MimeBodyPart();
                                ByteArrayDataSource attachmentSource = new ByteArrayDataSource(attachment.content, attachment.mimeType);
                                attachmentPart.setDataHandler(new DataHandler(attachmentSource));
                                attachmentPart.setFileName(attachment.fileName);
                                multipart.addBodyPart(attachmentPart);
                            }
                        }

                        // Retry mechanism
                        int maxRetries = 3;
                        for (int attempt = 1; attempt <= maxRetries; attempt++) {
                            try {
                                message.setContent(multipart);
                                Transport.send(message);
                                logger.info("Email sent successfully to {}", recipient);
                                break;
                            } catch (MessagingException e) {
                                if (attempt == maxRetries) {
                                    String reason = "Failed after " + maxRetries + " attempts: " + e.getMessage();
                                    logger.error("Failed to send email to {}: {}", recipient, reason, e);
                                    failedRecipients.put(recipient, reason);
                                } else {
                                    Thread.sleep(2000L * attempt);
                                }
                            }
                        }
                    } catch (AddressException e) {
                        String reason = "Invalid email address: " + e.getMessage();
                        logger.warn("Invalid email address: {}", recipient, e);
                        failedRecipients.put(recipient, reason);
                    } catch (MessagingException | IOException | InterruptedException e) {
                        String reason = "Failed to process email: " + e.getMessage();
                        logger.error("Failed to send email to {}: {}", recipient, reason, e);
                        failedRecipients.put(recipient, reason);
                    }
                }, executorService);
                futures.add(future);
            }

            // Wait for batch completion
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }

        return failedRecipients;
    }

    /**
     * Loads and validates PDF files from resource paths.
     */
    private List<Attachment> loadPDFAttachments(List<String> pdfPaths) throws IOException {
        if (pdfPaths == null || pdfPaths.isEmpty()) {
            return Collections.emptyList();
        }

        List<Attachment> attachments = new ArrayList<>();
        for (String pdfPath : pdfPaths) {
            if (pdfPath == null || pdfPath.trim().isEmpty()) {
                logger.warn("Skipping empty PDF path");
                continue;
            }

            try {
                byte[] pdfBytes = loadResource(pdfPath);
                validatePDF(pdfPath, pdfBytes);
                String fileName = pdfPath.substring(pdfPath.lastIndexOf('/') + 1);
                attachments.add(new Attachment(fileName, pdfBytes, "application/pdf"));
                logger.info("Loaded PDF attachment: {}", pdfPath);
            } catch (IOException e) {
                logger.error("Failed to load PDF: {}", pdfPath, e);
                throw new IOException("Cannot load PDF attachment: " + pdfPath + ", " + e.getMessage(), e);
            }
        }
        return attachments;
    }

    /**
     * Validates a PDF file.
     */
    private void validatePDF(String pdfPath, byte[] content) throws IOException {
        if (content.length > MAX_PDF_SIZE_BYTES) {
            throw new IOException("PDF file too large: " + pdfPath + " (" + content.length + " bytes, max " + MAX_PDF_SIZE_BYTES + ")");
        }
        if (content.length < 4 || !new String(content, 0, 4, StandardCharsets.UTF_8).startsWith("%PDF")) {
            throw new IOException("Invalid PDF file: " + pdfPath);
        }
    }

    /**
     * Validates the Thymeleaf template.
     */
    private void validateTemplate(String templatePath) throws IOException {
        try {
            templateEngine.process(templatePath, new Context());
        } catch (Exception e) {
            throw new IOException("Invalid template: " + templatePath + ", " + e.getMessage(), e);
        }
    }

    /**
     * Loads and caches a resource from the classpath.
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
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192]; // Buffer size 8KB
            int nRead;
            while ((nRead = stream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            byte[] content = buffer.toByteArray();
            validateResource(resourcePath, content);
            resourceCache.put(resourcePath, content);
            return content;
        }
    }

    /**
     * Validates resource content (e.g., check file type).
     */
    private void validateResource(String resourcePath, byte[] content) throws IOException {
        String lowerCasePath = resourcePath.toLowerCase();
        if (lowerCasePath.endsWith(".pdf")) {
            validatePDF(resourcePath, content);
        } else if (lowerCasePath.matches(".*\\.(png|jpg|jpeg|gif)$")) {
            try {
                ImageIO.read(new ByteArrayInputStream(content));
            } catch (Exception e) {
                throw new IOException("Invalid image file: " + resourcePath, e);
            }
        }
    }

    /**
     * Compresses image if it exceeds size limit.
     */
    private byte[] compressImageIfNeeded(byte[] imageBytes) throws IOException {
        if (imageBytes.length <= MAX_IMAGE_SIZE_BYTES) {
            return imageBytes;
        }
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image == null) {
            throw new IOException("Failed to read image for compression");
        }
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", compressed);
        return compressed.toByteArray();
    }

    /**
     * Personalizes content using Thymeleaf.
     */
    private String personalizeContent(String templatePath, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariable("baseUrl", baseUrl);
        variables.forEach(context::setVariable);
        return templateEngine.process(templatePath, context);
    }

    /**
     * Determines MIME type based on file extension.
     */
    private String getMimeType(String filePath) {
        String lowerCasePath = filePath.toLowerCase();
        if (lowerCasePath.endsWith(".png")) return "image/png";
        if (lowerCasePath.endsWith(".jpg") || lowerCasePath.endsWith(".jpeg")) return "image/jpeg";
        if (lowerCasePath.endsWith(".gif")) return "image/gif";
        if (lowerCasePath.endsWith(".pdf")) return "application/pdf";
        if (lowerCasePath.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        return "application/octet-stream";
    }

    /**
     * Validates attachment content.
     */
    private void validateAttachment(Attachment attachment) throws IOException {
        if (attachment.content == null || attachment.content.length == 0) {
            throw new IOException("Attachment content is empty: " + attachment.fileName);
        }
        if (attachment.mimeType == null || attachment.mimeType.isEmpty()) {
            throw new IOException("Invalid MIME type for attachment: " + attachment.fileName);
        }
    }

    /**
     * Partitions recipients into batches.
     */
    private List<List<String>> partitionRecipients(Set<String> recipients, int batchSize) {
        List<String> recipientList = new ArrayList<>(recipients);
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < recipientList.size(); i += batchSize) {
            batches.add(recipientList.subList(i, Math.min(i + batchSize, recipientList.size())));
        }
        return batches;
    }

    /**
     * Starts a processor for the email queue.
     */
    private void startEmailQueueProcessor() {
        executorService.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Runnable task = emailQueue.take();
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error processing email queue task", e);
                }
            }
        });
    }

    /**
     * Shuts down the service gracefully.
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
     * Demo method to showcase sending personalized emails with PDF attachments.
     */
    public static void main(String[] args) {
        try {
            MailService mailService = new MailService();

            // Personalized variables for recipients
            Map<String, Map<String, Object>> variables = new HashMap<>();
            variables.put("khai1234sd@gmail.com", Map.of(
                    "name", "Khai Nguyen Hoang",
                    "token", "abc123",
                    "verificationLink", mailService.baseUrl + "/verify?token=abc123",
                    "items", List.of("Item 1", "Item 2")
            ));
            variables.put("khainhce182286@fpt.edu.vn", Map.of(
                    "name", "Jane Smith",
                    "token", "xyz789",
                    "verificationLink", mailService.baseUrl + "/verify?token=xyz789",
                    "items", List.of("Item A", "Item B")
            ));

            // Embedded images
            Map<String, String> imageMap = Map.of(
                    "img/1cd2ff272e2531b8041264de38db1b5f.png", "banner",
                    "img/4d3b20f647cbdeb288013a15cce39fdf.jpg", "icon"
            );

            // PDF paths
            List<String> pdfPaths = List.of("pdf/demo.pdf");

            // Send emails with batch size of 10
            Map<String, String> failedRecipients = mailService.sendPersonalizedWithPDF(
                    "email",
                    "Welcome to Our Service",
                    variables,
                    imageMap,
                    pdfPaths,
                    10
            );

            // Report results
            if (failedRecipients.isEmpty()) {
                System.out.println("All emails sent successfully!");
            } else {
                System.out.println("Failed to send emails to: ");
                failedRecipients.forEach((email, reason) -> System.out.println(email + ": " + reason));
            }

            // Cleanup
            mailService.shutdown();
        } catch (IOException | IllegalStateException e) {
            logger.error("Demo failed", e);
            System.out.println("Demo failed: " + e.getMessage());
        }
    }
}