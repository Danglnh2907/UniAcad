
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MailService nâng cao với khả năng xử lý tệp đính kèm (PDF, Word, Excel) và cấu hình từ save.env.
 */
public class MailService {
    private static final Logger logger = LoggerFactory.getLogger(MailService.class);
    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int MAX_IMAGE_SIZE_BYTES = 2 * 1024 * 1024; // 2MB
    private static final long MAX_ATTACHMENT_SIZE_BYTES = 10 * 1024 * 1024; // 10MB cho tất cả tệp đính kèm

    private final String username;
    private final String password;
    private final Properties smtpProperties;
    private final Cache<String, byte[]> resourceCache;
    private final ThreadPoolExecutor executorService;
    private final BlockingQueue<Runnable> emailQueue;
    private final Bucket rateLimiter;
    private final TemplateEngine templateEngine;
    private final String baseUrl;

    /**
     * Khởi tạo MailService với cấu hình từ save.env và ThreadPool tối ưu.
     */
    public MailService() {
        // Tải cấu hình từ save.env
        Dotenv dotenv = Dotenv.configure()
                .filename("save.env")
                .ignoreIfMissing()
                .load();

        this.username = dotenv.get("SMTP_USERNAME");
        this.password = dotenv.get("SMTP_PASSWORD");
        String smtpHost = dotenv.get("SMTP_HOST", "smtp.gmail.com");
        String smtpPortStr = dotenv.get("SMTP_PORT", "587");
        this.baseUrl = dotenv.get("APP_BASE_URL", "http://localhost:9090/UniAcad_war");

        // Kiểm tra cấu hình bắt buộc
        if (this.username == null || this.username.trim().isEmpty()) {
            throw new IllegalStateException("SMTP_USERNAME là bắt buộc trong save.env");
        }
        if (this.password == null || this.password.trim().isEmpty()) {
            throw new IllegalStateException("SMTP_PASSWORD là bắt buộc trong save.env");
        }

        int smtpPort;
        try {
            smtpPort = Integer.parseInt(smtpPortStr);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("SMTP_PORT không hợp lệ trong save.env: " + smtpPortStr, e);
        }

        // Ghi log cấu hình đã tải (che mật khẩu)
        logger.info("Đã tải cấu hình SMTP: username={}, host={}, port={}, baseUrl={}",
                username, smtpHost, smtpPort, baseUrl);

        // Cấu hình SMTP
        this.smtpProperties = new Properties();
        smtpProperties.put("mail.smtp.auth", "true");
        smtpProperties.put("mail.smtp.host", smtpHost);
        smtpProperties.put("mail.smtp.port", smtpPort);
        smtpProperties.put("mail.smtp.starttls.enable", "true");

        // Bộ đệm tài nguyên với kích thước lớn hơn và dọn dẹp thủ công
        this.resourceCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofHours(2))
                .build();

        // ThreadPoolExecutor tối ưu
        this.emailQueue = new LinkedBlockingQueue<>(100); // Dung lượng hàng đợi
        this.executorService = new ThreadPoolExecutor(
                4, // corePoolSize
                16, // maximumPoolSize
                60, TimeUnit.SECONDS, // keepAliveTime
                this.emailQueue,
                new ThreadFactoryBuilder().setNameFormat("email-processor-%d").build(), // Đặt tên thread
                new ThreadPoolExecutor.CallerRunsPolicy() // Chính sách từ chối
        ) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                logThreadPoolMetrics();
            }
        };
        startEmailQueueProcessor();

        // Giới hạn tốc độ: 200 email mỗi phút
        Bandwidth limit = Bandwidth.classic(200, Refill.greedy(200, Duration.ofMinutes(1)));
        this.rateLimiter = Bucket.builder().addLimit(limit).build();

        // Khởi tạo công cụ mẫu Thymeleaf
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
    }

    /**
     * Ghi log số liệu ThreadPool để giám sát.
     */
    private void logThreadPoolMetrics() {
        logger.debug("Số liệu ThreadPool: activeThreads={}, queueSize={}, completedTasks={}",
                executorService.getActiveCount(),
                executorService.getQueue().size(),
                executorService.getCompletedTaskCount());
    }

    /**
     * Gửi email HTML cá nhân hóa với các tệp đính kèm được chỉ định bởi đường dẫn tài nguyên.
     *
     * @param templatePath Đường dẫn tới mẫu Thymeleaf trong tài nguyên
     * @param subject      Chủ đề email
     * @param variables    Map của email người nhận tới các biến cụ thể của họ
     * @param imageMap     Map của đường dẫn tài nguyên hình ảnh tới Content-ID của chúng
     * @param attachmentPaths Danh sách đường dẫn tài nguyên của tệp đính kèm (PDF, Word, Excel)
     * @param batchSize    Số lượng email mỗi lô
     * @return Map của các người nhận thất bại với lý do thất bại
     * @throws IOException Nếu tải mẫu hoặc tài nguyên thất bại
     */
    public Map<String, String> sendPersonalizedWithAttachments(
            String templatePath,
            String subject,
            Map<String, Map<String, Object>> variables,
            Map<String, String> imageMap,
            List<String> attachmentPaths,
            int batchSize) throws IOException {

        // Tải và kiểm tra tệp đính kèm
        List<Attachment> attachments = loadAttachments(attachmentPaths);

        // Ủy quyền cho phương thức sendPersonalized chính
        return sendPersonalized(templatePath, subject, variables, imageMap, attachments, batchSize);
    }

    /**
     * Gửi email HTML cá nhân hóa tới người nhận với hình ảnh nhúng và tệp đính kèm.
     */
    public Map<String, String> sendPersonalized(
            String templatePath,
            String subject,
            Map<String, Map<String, Object>> variables,
            Map<String, String> imageMap,
            List<Attachment> attachments,
            int batchSize) throws IOException {

        if (variables == null || variables.isEmpty()) {
            throw new IllegalArgumentException("Map biến không được rỗng hoặc null");
        }

        // Kiểm tra mẫu
        validateTemplate(templatePath);

        // Chia người nhận thành các lô
        batchSize = batchSize <= 0 ? DEFAULT_BATCH_SIZE : batchSize;
        List<List<String>> recipientBatches = partitionRecipients(variables.keySet(), batchSize);
        Map<String, String> failedRecipients = new ConcurrentHashMap<>();

        // Xử lý từng lô
        for (List<String> batch : recipientBatches) {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (String recipient : batch) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        // Áp dụng giới hạn tốc độ
                        rateLimiter.asBlocking().consume(1);

                        // Kiểm tra email người nhận
                        InternetAddress address = new InternetAddress(recipient);
                        address.validate();

                        // Tạo email cá nhân hóa
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

                        // Tạo nội dung đa phần
                        MimeMultipart multipart = new MimeMultipart("related");

                        // Cá nhân hóa nội dung HTML với Thymeleaf
                        String emailContent = personalizeContent(templatePath, variables.getOrDefault(recipient, Map.of()));
                        MimeBodyPart htmlPart = new MimeBodyPart();
                        htmlPart.setContent(emailContent, "text/html; charset=utf-8");
                        multipart.addBodyPart(htmlPart);

                        // Nhúng hình ảnh đã nén
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

                        // Thêm tệp đính kèm
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

                        // Cơ chế thử lại
                        int maxRetries = 3;
                        for (int attempt = 1; attempt <= maxRetries; attempt++) {
                            try {
                                message.setContent(multipart);
                                Transport.send(message);
                                logger.info("Gửi email thành công tới {}", recipient);
                                break;
                            } catch (MessagingException e) {
                                if (attempt == maxRetries) {
                                    String reason = "Thất bại sau " + maxRetries + " lần thử: " + e.getMessage();
                                    logger.error("Không thể gửi email tới {}: {}", recipient, reason, e);
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
                        String reason = "Địa chỉ email không hợp lệ: " + e.getMessage();
                        logger.warn("Địa chỉ email không hợp lệ: {}", recipient, e);
                        failedRecipients.put(recipient, reason);
                    } catch (MessagingException | IOException e) {
                        String reason = "Không thể xử lý email: " + e.getMessage();
                        logger.error("Không thể gửi email tới {}: {}", recipient, reason, e);
                        failedRecipients.put(recipient, reason);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }, executorService);
                futures.add(future);
            }

            // Chờ hoàn thành lô
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }

        return failedRecipients;
    }

    /**
     * Tải và kiểm tra tệp (PDF, Word, Excel) từ đường dẫn tài nguyên.
     */
    private List<Attachment> loadAttachments(List<String> attachmentPaths) throws IOException {
        if (attachmentPaths == null || attachmentPaths.isEmpty()) {
            return Collections.emptyList();
        }

        List<Attachment> attachments = new ArrayList<>();
        for (String path : attachmentPaths) {
            if (path == null || path.trim().isEmpty()) {
                logger.warn("Bỏ qua đường dẫn tệp đính kèm rỗng");
                continue;
            }

            try {
                byte[] fileBytes = loadResource(path);
                String fileName = path.substring(path.lastIndexOf('/') + 1);
                String mimeType = getMimeType(path);
                attachments.add(new Attachment(fileName, fileBytes, mimeType));
                logger.info("Đã tải tệp đính kèm: {} (loại: {})", path, mimeType);
            } catch (IOException e) {
                logger.warn("Bỏ qua tệp đính kèm do lỗi: {} - {}", path, e.getMessage());
                // Tiếp tục với các tệp đính kèm khác thay vì thất bại
            }
        }
        if (attachments.isEmpty() && !attachmentPaths.isEmpty()) {
            logger.warn("Không tải được tệp đính kèm hợp lệ nào từ các đường dẫn cung cấp");
        }
        return attachments;
    }

    /**
     * Kiểm tra nội dung tệp đính kèm dựa trên loại tệp.
     */
    private void validateAttachmentContent(String path, byte[] content) throws IOException {
        if (content.length > MAX_ATTACHMENT_SIZE_BYTES) {
            throw new IOException("Tệp đính kèm quá lớn: " + path + " (" + content.length + " bytes, tối đa " + MAX_ATTACHMENT_SIZE_BYTES + ")");
        }
        if (content.length == 0) {
            throw new IOException("Tệp đính kèm rỗng: " + path);
        }

        String lowerCasePath = path.toLowerCase();
        if (lowerCasePath.endsWith(".pdf")) {
            if (content.length < 4 || !new String(content, 0, 4, StandardCharsets.UTF_8).startsWith("%PDF")) {
                throw new IOException("Tệp PDF không hợp lệ: " + path);
            }
        } else if (lowerCasePath.endsWith(".docx") || lowerCasePath.endsWith(".xlsx")) {
            // Kiểm tra cơ bản: Kiểm tra tiêu đề ZIP (DOCX và XLSX dựa trên ZIP)
            if (content.length < 2 || content[0] != 0x50 || content[1] != 0x4B) {
                throw new IOException("Tệp " + (lowerCasePath.endsWith(".docx") ? "DOCX" : "XLSX") + " không hợp lệ: " + path);
            }
        } else {
            throw new IOException("Loại tệp đính kèm không được hỗ trợ: " + path);
        }
    }

    /**
     * Kiểm tra mẫu Thymeleaf.
     */
    private void validateTemplate(String templatePath) throws IOException {
        try {
            templateEngine.process(templatePath, new Context());
        } catch (Exception e) {
            throw new IOException("Mẫu không hợp lệ: " + templatePath + ", " + e.getMessage(), e);
        }
    }

    /**
     * Tải và lưu trữ tài nguyên từ classpath.
     */
    private byte[] loadResource(String resourcePath) throws IOException {
        byte[] cachedContent = resourceCache.getIfPresent(resourcePath);
        if (cachedContent != null) {
            return cachedContent;
        }
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IOException("Không tìm thấy tài nguyên: " + resourcePath);
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192]; // Kích thước bộ đệm 8KB
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
     * Kiểm tra nội dung tài nguyên (ví dụ: kiểm tra loại tệp).
     */
    private void validateResource(String resourcePath, byte[] content) throws IOException {
        String lowerCasePath = resourcePath.toLowerCase();
        if (lowerCasePath.endsWith(".pdf") || lowerCasePath.endsWith(".docx") || lowerCasePath.endsWith(".xlsx")) {
            validateAttachmentContent(resourcePath, content);
        } else if (lowerCasePath.matches(".*\\.(png|jpg|jpeg|gif)$")) {
            try {
                ImageIO.read(new ByteArrayInputStream(content));
            } catch (Exception e) {
                throw new IOException("Tệp hình ảnh không hợp lệ: " + resourcePath, e);
            }
        }
    }

    /**
     * Nén hình ảnh nếu vượt quá giới hạn kích thước.
     */
    private byte[] compressImageIfNeeded(byte[] imageBytes) throws IOException {
        if (imageBytes.length <= MAX_IMAGE_SIZE_BYTES) {
            return imageBytes;
        }
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image == null) {
            throw new IOException("Không thể đọc hình ảnh để nén");
        }
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", compressed);
        return compressed.toByteArray();
    }

    /**
     * Cá nhân hóa nội dung bằng Thymeleaf.
     */
    private String personalizeContent(String templatePath, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariable("baseUrl", baseUrl);
        variables.forEach(context::setVariable);
        return templateEngine.process(templatePath, context);
    }

    /**
     * Xác định loại MIME dựa trên phần mở rộng tệp.
     */
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

    /**
     * Kiểm tra nội dung tệp đính kèm.
     */
    private void validateAttachment(Attachment attachment) throws IOException {
        if (attachment.content == null || attachment.content.length == 0) {
            throw new IOException("Nội dung tệp đính kèm rỗng: " + attachment.fileName);
        }
        if (attachment.mimeType == null || attachment.mimeType.isEmpty()) {
            throw new IOException("Loại MIME không hợp lệ cho tệp đính kèm: " + attachment.fileName);
        }
    }

    /**
     * Chia người nhận thành các lô.
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
     * Bắt đầu một bộ xử lý cho hàng đợi email.
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
                    logger.error("Lỗi khi xử lý tác vụ hàng đợi email", e);
                }
            }
        });
    }

    /**
     * Tắt dịch vụ một cách nhẹ nhàng.
     */
    public void shutdown() {
        logger.info("Đang tắt ThreadPool của MailService...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(15, TimeUnit.SECONDS)) {
                logger.warn("ThreadPool không kết thúc trong thời gian chờ, buộc tắt...");
                executorService.shutdownNow();
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    logger.error("ThreadPool không kết thúc sau khi buộc tắt");
                }
            }
        } catch (InterruptedException e) {
            logger.error("Tắt bị gián đoạn", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Hoàn tất tắt ThreadPool của MailService");
    }

    /**
     * Biểu diễn một tệp đính kèm email.
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
     * ThreadFactory tùy chỉnh để đặt tên thread.
     */
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
     * Phương thức demo để thể hiện việc gửi email cá nhân hóa với tệp đính kèm.
     */
    public static void main(String[] args) {
        try {
            MailService mailService = new MailService();

            // Biến cá nhân hóa cho người nhận
            Map<String, Map<String, Object>> variables = new HashMap<>();
            variables.put("khainhce182286@fpt.edu.vn", Map.of(
                    "name", "Nguyen Hoang Khai",
                    "token", "abc123",
                    "verificationLink", mailService.baseUrl + "/verify?token=abc123",
                    "items", List.of("Item 1", "Item 2")
            ));
            variables.put("khai1234sd@gmail.com", Map.of(
                    "name", "Nguyen Hoang Khaiii",
                    "token", "abc124",
                    "verificationLink", mailService.baseUrl + "/verify?token=abc124",
                    "items", List.of("Item 1", "Item 2")
            ));

            // Hình ảnh nhúng
            Map<String, String> imageMap = Map.of(
                    "img/1cd2ff272e2531b8041264de38db1b5f.png", "banner"
            );

            // Đường dẫn tệp đính kèm (PDF, Word, Excel)
            // Lưu ý: Đảm bảo các tệp này tồn tại trong thư mục tài nguyên (ví dụ: src/main/resources/pdf/demo.pdf)
            List<String> attachmentPaths = List.of(
                    "pdf/demo.pdf",
                    // Thêm các tệp .docx và .xlsx hợp lệ vào tài nguyên và bỏ ghi chú dưới đây
                    "docx/sample.docx",
                    "xlsx/data.xlsx"
            );

            // Gửi email với kích thước lô là 10
            Map<String, String> failedRecipients = mailService.sendPersonalizedWithAttachments(
                    "email",
                    "Chào mừng đến với Dịch vụ của chúng tôi",
                    variables,
                    imageMap,
                    attachmentPaths,
                    10
            );

            // Báo cáo kết quả
            if (failedRecipients.isEmpty()) {
                System.out.println("Tất cả email đã được gửi thành công!");
            } else {
                System.out.println("Không thể gửi email tới: ");
                failedRecipients.forEach((email, reason) -> System.out.println(email + ": " + reason));
            }

            // Dọn dẹp
            mailService.shutdown();
        } catch (IOException | IllegalStateException e) {
            logger.error("Demo thất bại", e);
            System.out.println("Demo thất bại: " + e.getMessage());
        }
    }
}