package util.service.email;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.activation.DataHandler;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.servlet.ServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import io.github.cdimascio.dotenv.Dotenv;
import util.service.file.FileService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Advanced MailService using FileService to read files for email sending, without saving files.
 */
public class MailService {
    private static final Logger logger = LoggerFactory.getLogger(MailService.class);
    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int MAX_IMAGE_SIZE_BYTES = 2 * 1024 * 1024; // 2MB
    private static final long MAX_ATTACHMENT_SIZE_BYTES = 10 * 1024 * 1024; // 10MB

    private final String username;
    private final String password;
    private final Properties smtpProperties;
    private final Cache<String, byte[]> resourceCache;
    private final ThreadPoolExecutor executorService;
    private final BlockingQueue<Runnable> emailQueue;
    private final Bucket rateLimiter;
    private final TemplateEngine templateEngine;
    private final String baseUrl;
    private final FileService fileService;

    public MailService() {
        this(null);
    }

    public MailService(ServletContext context) {
        Dotenv dotenv = Dotenv.configure()
                .filename("save.env")
                .ignoreIfMissing()
                .load();

        this.username = dotenv.get("SMTP_USERNAME");
        this.password = dotenv.get("SMTP_PASSWORD");
        String smtpHost = dotenv.get("SMTP_HOST", "smtp.gmail.com");
        String smtpPortStr = dotenv.get("SMTP_PORT", "587");
        this.baseUrl = dotenv.get("APP_BASE_URL", "http://localhost:9090/UniAcad_war");
        String fileStoragePath = dotenv.get("FILE_STORAGE_PATH");

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

        this.fileService = context != null ? new FileService(context, "/WEB-INF/resources") : new FileService(fileStoragePath);

        logger.info("Loaded SMTP configuration: username={}, host={}, port={}, baseUrl={}, fileStoragePath={}",
                username, smtpHost, smtpPort, baseUrl, fileService.getFilePath());

        this.smtpProperties = new Properties();
        smtpProperties.put("mail.smtp.auth", "true");
        smtpProperties.put("mail.smtp.host", smtpHost);
        smtpProperties.put("mail.smtp.port", smtpPort);
        smtpProperties.put("mail.smtp.starttls.enable", "true");

        this.resourceCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofHours(2))
                .build();

        this.emailQueue = new LinkedBlockingQueue<>(100);
        this.executorService = new ThreadPoolExecutor(
                4, 16, 60, TimeUnit.SECONDS, emailQueue,
                new ThreadFactoryBuilder().setNameFormat("email-processor-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        ) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                logThreadPoolMetrics();
            }
        };
        startEmailQueueProcessor();

        Bandwidth limit = Bandwidth.classic(200, Refill.greedy(200, Duration.ofMinutes(1)));
        this.rateLimiter = Bucket.builder().addLimit(limit).build();

        FileTemplateResolver templateResolver = new FileTemplateResolver();
        templateResolver.setPrefix(fileService.getFilePath() + "templates" + File.separator);
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
    }

    private void logThreadPoolMetrics() {
        logger.debug("ThreadPool metrics: activeThreads={}, queueSize={}, completedTasks={}",
                executorService.getActiveCount(), executorService.getQueue().size(), executorService.getCompletedTaskCount());
    }

    public Map<String, String> sendPersonalizedWithAttachments(
            String templateName, String subject, Map<String, Map<String, Object>> variables,
            Map<String, String> imageMap, List<String> attachmentPaths, int batchSize) throws IOException {
        validateTemplate(templateName);
        List<String> validAttachmentPaths = validateAttachments(attachmentPaths);
        List<Attachment> attachments = loadAttachments(validAttachmentPaths);
        return sendPersonalized(templateName, subject, variables, imageMap, attachments, batchSize);
    }

    public Map<String, String> sendPersonalized(
            String templateName, String subject, Map<String, Map<String, Object>> variables,
            Map<String, String> imageMap, List<Attachment> attachments, int batchSize) throws IOException {
        if (variables == null || variables.isEmpty()) {
            throw new IllegalArgumentException("Variables map cannot be null or empty");
        }

        batchSize = batchSize <= 0 ? DEFAULT_BATCH_SIZE : batchSize;
        List<List<String>> recipientBatches = partitionRecipients(variables.keySet(), batchSize);
        Map<String, String> failedRecipients = new ConcurrentHashMap<>();

        for (List<String> batch : recipientBatches) {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (String recipient : batch) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        rateLimiter.asBlocking().consume(1);
                        InternetAddress address = new InternetAddress(recipient);
                        address.validate();

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

                        MimeMultipart multipart = new MimeMultipart("related");
                        String emailContent = personalizeContent(templateName, variables.getOrDefault(recipient, Map.of()));
                        MimeBodyPart htmlPart = new MimeBodyPart();
                        htmlPart.setContent(emailContent, "text/html; charset=utf-8");
                        multipart.addBodyPart(htmlPart);

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

                        int maxRetries = 3;
                        for (int attempt = 1; attempt <= maxRetries; attempt++) {
                            try {
                                message.setContent(multipart);
                                Transport.send(message);
                                logger.info("Successfully sent email to {}", recipient);
                                break;
                            } catch (MessagingException e) {
                                if (attempt == maxRetries) {
                                    String reason = "Failed after " + maxRetries + " attempts: " + e.getMessage();
                                    logger.error("Failed to send email to {}: {}", recipient, reason, e);
                                    failedRecipients.put(recipient, reason);
                                } else {
                                    try {
                                        Thread.sleep(2000L * attempt);
                                    } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                        throw new RuntimeException(ie);
                                    }
                                }
                            }
                        }
                    } catch (AddressException e) {
                        String reason = "Invalid email address: " + e.getMessage();
                        logger.warn("Invalid email address: {}", recipient, e);
                        failedRecipients.put(recipient, reason);
                    } catch (MessagingException | IOException e) {
                        String reason = "Failed to process email: " + e.getMessage();
                        logger.error("Failed to send email to {}: {}", recipient, reason, e);
                        failedRecipients.put(recipient, reason);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }, executorService);
                futures.add(future);
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
        return failedRecipients;
    }

    private void validateTemplate(String templateName) throws IOException {
        String templatePath = fileService.getFilePath(templateName + ".html", "template");
        File templateFile = new File(templatePath);
        if (!templateFile.exists()) {
            logger.error("Template not found: {}", templatePath);
            throw new IOException("Template not found: " + templatePath);
        }
        try {
            templateEngine.process(templateName, new Context());
        } catch (Exception e) {
            logger.error("Invalid template: {}", templateName, e);
            throw new IOException("Invalid template: " + templateName, e);
        }
    }

    private List<String> validateAttachments(List<String> attachmentPaths) throws IOException {
        if (attachmentPaths == null || attachmentPaths.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> validPaths = new ArrayList<>();
        for (String path : attachmentPaths) {
            if (path == null || path.trim().isEmpty()) {
                logger.warn("Skipping empty attachment path");
                continue;
            }

            try {
                String fileName = Paths.get(path).getFileName().toString();
                String fileType = getFileType(path);
                String filePath = fileService.getFilePath(fileName, fileType);
                File file = new File(filePath);
                if (!file.exists()) {
                    logger.error("Attachment not found: {}", filePath);
                    throw new IOException("Attachment not found: " + filePath);
                }
                validPaths.add(filePath);
                logger.info("Validated attachment: {} (type: {})", filePath, fileType);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid attachment type: {} - {}", path, e.getMessage());
            }
        }
        if (validPaths.isEmpty() && !attachmentPaths.isEmpty()) {
            logger.warn("No valid attachments found from provided paths");
        }
        return validPaths;
    }

    private List<Attachment> loadAttachments(List<String> attachmentPaths) throws IOException {
        if (attachmentPaths == null || attachmentPaths.isEmpty()) {
            return Collections.emptyList();
        }

        List<Attachment> attachments = new ArrayList<>();
        for (String path : attachmentPaths) {
            try {
                String fileName = Paths.get(path).getFileName().toString();
                String fileType = getFileType(path);
                try (InputStream stream = fileService.getFileInputStream(fileName, fileType)) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] data = new byte[8192];
                    int nRead;
                    while ((nRead = stream.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    byte[] fileBytes = buffer.toByteArray();
                    validateResource(path, fileBytes);
                    String mimeType = getMimeType(path);
                    attachments.add(new Attachment(fileName, fileBytes, mimeType));
                    logger.info("Loaded attachment: {} (type: {})", path, mimeType);
                }
            } catch (IOException e) {
                logger.warn("Skipping attachment due to error: {} - {}", path, e.getMessage());
            }
        }
        if (attachments.isEmpty() && !attachmentPaths.isEmpty()) {
            logger.warn("No valid attachments loaded from provided paths");
        }
        return attachments;
    }

    private byte[] loadResource(String resourcePath) throws IOException {
        String fileName = Paths.get(resourcePath).getFileName().toString();
        String fileType = getFileType(resourcePath);

        byte[] cachedContent = resourceCache.getIfPresent(resourcePath);
        if (cachedContent != null) {
            return cachedContent;
        }

        try (InputStream stream = fileService.getFileInputStream(fileName, "img")) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int nRead;
            while ((nRead = stream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            byte[] content = buffer.toByteArray();
            validateResource(resourcePath, content);
            resourceCache.put(resourcePath, content);
            return content;
        } catch (IOException e) {
            logger.error("Failed to load resource: {}", resourcePath, e);
            throw new IOException("Failed to load resource: " + resourcePath, e);
        }
    }

    private void validateResource(String resourcePath, byte[] content) throws IOException {
        if (content.length > MAX_ATTACHMENT_SIZE_BYTES) {
            throw new IOException("Resource too large: " + resourcePath);
        }
        if (content.length == 0) {
            throw new IOException("Empty resource: " + resourcePath);
        }

        String lowerCasePath = resourcePath.toLowerCase();
        if (lowerCasePath.endsWith(".pdf")) {
            if (content.length < 4 || !new String(content, 0, 4, StandardCharsets.UTF_8).startsWith("%PDF")) {
                throw new IOException("Invalid PDF file: " + resourcePath);
            }
        } else if (lowerCasePath.endsWith(".docx") || lowerCasePath.endsWith(".xlsx")) {
            if (content.length < 2 || content[0] != 0x50 || content[1] != 0x4B) {
                throw new IOException("Invalid " + (lowerCasePath.endsWith(".docx") ? "DOCX" : "XLSX") + " file: " + resourcePath);
            }
        } else if (lowerCasePath.matches(".*\\.(png|jpg|jpeg|gif)$")) {
            try {
                ImageIO.read(new ByteArrayInputStream(content));
            } catch (Exception e) {
                throw new IOException("Invalid image file: " + resourcePath, e);
            }
        } else if (lowerCasePath.endsWith(".html")) {
            // No additional validation for HTML beyond template engine
        } else {
            throw new IOException("Unsupported resource type: " + resourcePath);
        }
    }

    private byte[] compressImageIfNeeded(byte[] imageBytes) throws IOException {
        if (imageBytes.length <= MAX_IMAGE_SIZE_BYTES) {
            return imageBytes;
        }
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image == null) {
            throw new IOException("Unable to read image for compression");
        }
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", compressed);
        return compressed.toByteArray();
    }

    private String personalizeContent(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariable("baseUrl", baseUrl);
        variables.forEach(context::setVariable);
        return templateEngine.process(templateName, context);
    }

    private String getMimeType(String filePath) {
        String lowerCasePath = filePath.toLowerCase();
        if (lowerCasePath.endsWith(".png")) return "image/png";
        if (lowerCasePath.endsWith(".jpg") || lowerCasePath.endsWith(".jpeg")) return "image/jpeg";
        if (lowerCasePath.endsWith(".gif")) return "image/gif";
        if (lowerCasePath.endsWith(".pdf")) return "application/pdf";
        if (lowerCasePath.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lowerCasePath.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return "application/octet-stream";
    }

    private String getFileType(String filePath) {
        String lowerCasePath = filePath.toLowerCase();
        if (lowerCasePath.endsWith(".xlsx")) return "xlsx";
        if (lowerCasePath.endsWith(".docx")) return "docx";
        if (lowerCasePath.endsWith(".pdf")) return "pdf";
        if (lowerCasePath.matches(".*\\.(png|jpg|jpeg|gif)$")) return "img";
        if (lowerCasePath.endsWith(".html")) return "template";
        throw new IllegalArgumentException("Unsupported file type: " + filePath);
    }

    private void validateAttachment(Attachment attachment) throws IOException {
        if (attachment.content == null || attachment.content.length == 0) {
            throw new IOException("Empty attachment content: " + attachment.fileName);
        }
        if (attachment.mimeType == null || attachment.mimeType.isEmpty()) {
            throw new IOException("Invalid MIME type for attachment: " + attachment.fileName);
        }
    }

    private List<List<String>> partitionRecipients(Set<String> recipients, int batchSize) {
        List<String> recipientList = new ArrayList<>(recipients);
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < recipientList.size(); i += batchSize) {
            batches.add(recipientList.subList(i, Math.min(i + batchSize, recipientList.size())));
        }
        return batches;
    }

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

    public void shutdown() {
        logger.info("Shutting down MailService ThreadPool...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("ThreadPool did not terminate within timeout, forcing shutdown...");
                executorService.shutdownNow();
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.error("ThreadPool did not terminate after forced shutdown");
                }
            }
        } catch (InterruptedException e) {
            logger.error("Shutdown interrupted", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("MailService ThreadPool shutdown complete");
    }

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

    private static class ThreadFactoryBuilder {
        private String nameFormat;

        public ThreadFactoryBuilder setNameFormat(String nameFormat) {
            this.nameFormat = nameFormat;
            return this;
        }

        public ThreadFactory build() {
            return new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);

                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName(String.format(nameFormat, threadNumber.getAndIncrement()));
                    return thread;
                }
            };
        }
    }

    /**
     * Builds a map of image file paths to their corresponding Content-IDs.
     * Checks if each image file exists, throwing an IOException if not.
     *
     * @param imagePaths List of relative image paths (e.g., "img/filename.jpg")
     * @param imageContentIds List of Content-IDs (e.g., "emailIcon")
     * @return Map of full file paths to Content-IDs
     * @throws IOException If an image file does not exist or paths/IDs lists mismatch
     */
    private Map<String, String> buildImageMap(List<String> imagePaths, List<String> imageContentIds) throws IOException {
        if (imagePaths.size() != imageContentIds.size()) {
            throw new IllegalArgumentException("imagePaths and imageContentIds must have the same size");
        }

        Map<String, String> imageMap = new HashMap<>();
        IntStream.range(0, imagePaths.size()).forEach(i -> {
            String imagePath = imagePaths.get(i);
            String fileName = Paths.get(imagePath).getFileName().toString();
            String filePath = fileService.getFilePath(fileName, "img");
            File file = new File(filePath);
            try {
                if (!file.exists()) {
                    LoggerFactory.getLogger(MailService.class).error("Image not found: {}", filePath);
                    throw new IOException("Image not found: " + filePath);
                }
                imageMap.put(filePath, imageContentIds.get(i));
            } catch (IOException e) {
                LoggerFactory.getLogger(MailService.class).error("Error validating image: {}", filePath, e);
                throw new RuntimeException(e);
            }
        });
        return imageMap;
    }

    public static void main(String[] args) {
        try {
            MailService mailService = new MailService();

            Map<String, Map<String, Object>> variables = new HashMap<>();
            variables.put("khainhce182286@fpt.edu.vn", Map.of(
                    "name", "Nguyen Hoang Khai",
                    "token", "abc123",
                    "verificationLink", mailService.baseUrl + "/verify?token=abc123",
                    "items", List.of("Item 1", "Item 2")
            ));
            variables.put("khai1234sd@gmail.com", Map.of(
                    "name", "Nguyen Hoang Kha",
                    "token", "abc124",
                    "verificationLink", mailService.baseUrl + "/verify?token=abc124",
                    "items", List.of("Item 3", "Item 4")
            ));

            List<String> imagePaths = List.of(
                    "img/f8d71b6c42f7300871f9e091c6a737e3.jpg",
                    "img/4d3b20f647cbdeb288013a15cce39fdf.jpg",
                    "img/1cd2ff272e2531b8041264de38db1b5f.png",
                    "img/51a2644c1491853d60a9688ed8f4fa9e.png",
                    "img/7575b9251670cd15f3423fd911239179.png",
                    "img/948015252763872ed01b79cbbbb7c68b.png"
            );
            List<String> imageContentIds = List.of(
                    "emailIcon", "textIcon", "xIcon", "instagramIcon", "facebookIcon", "bannerIcon"
            );
            Map<String, String> imageMap = mailService.buildImageMap(imagePaths, imageContentIds);

            List<String> attachmentPaths = List.of(
                    "pdf/demo.pdf",
                    "docx/sample.docx",
                    "xlsx/data.xlsx"
            );

            Map<String, String> failedRecipients = mailService.sendPersonalizedWithAttachments(
                    "verification-email",
                    "Welcome to Our Service123",
                    variables,
                    imageMap,
                    attachmentPaths,
                    10
            );

            if (failedRecipients.isEmpty()) {
                System.out.println("All emails sent successfully!");
            } else {
                System.out.println("Failed to send emails to:");
                failedRecipients.forEach((email, reason) -> System.out.println(email + ": " + reason));
            }

            mailService.shutdown();
        } catch (IOException | IllegalStateException e) {
            logger.error("Demo failed", e);
            System.out.println("Demo failed: " + e.getMessage());
        }
    }
}