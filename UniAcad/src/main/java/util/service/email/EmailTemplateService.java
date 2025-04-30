package util.service.email;

import dao.StudentDAO;
import model.datasupport.WarningInfo;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.servlet.ServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service to send email templates like Welcome emails and Warning emails.
 */
public class EmailTemplateService {
    private static final Logger logger = LoggerFactory.getLogger(EmailTemplateService.class);
    private final MailService mailService;
    private final StudentDAO studentDAO;
    private final String appURL;

    public EmailTemplateService(ServletContext context) {
        this.mailService = new MailService(context);
        this.studentDAO = new StudentDAO();
        Dotenv dotenv = Dotenv.configure()
                .filename("save.env")
                .ignoreIfMissing()
                .load();
        this.appURL = dotenv.get("APP_BASE_URL", "http://localhost:9090/UniAcad_war");
    }

    /**
     * Send Welcome Emails
     */
    public Map<String, String> sendWelcomeEmails(List<String> emails) throws IOException {
        if (emails == null || emails.isEmpty()) {
            throw new IllegalArgumentException("Email list cannot be null or empty");
        }

        Map<String, Map<String, Object>> variables = new HashMap<>();
        AtomicInteger tokenCounter = new AtomicInteger(1);

        for (String email : emails) {
            if (!studentDAO.checkEmailExists(email)) {
                logger.warn("Skipping email {}: not found in Student table", email);
                continue;
            }

            String fullName = studentDAO.getNameByEmail(email);
            if (fullName == null || fullName.isBlank()) {
                logger.warn("Skipping email {}: could not retrieve student name", email);
                continue;
            }

            String token = "WELCOME-" + tokenCounter.getAndIncrement() + "-" + UUID.randomUUID().toString().substring(0, 8);
            String verificationLink = appURL + "/verify?token=" + token;

            Map<String, Object> emailVars = new HashMap<>();
            emailVars.put("name", fullName);
            emailVars.put("token", token);
            emailVars.put("verificationLink", verificationLink);

            variables.put(email, emailVars);
        }

        if (variables.isEmpty()) {
            logger.error("No valid students found to send welcome emails");
            return Map.of("error", "No valid student emails found");
        }

        Map<String, String> failedRecipients = mailService.sendPersonalized(
                "welcome",
                "Welcome to UniAcad!",
                variables,
                null,
                null,
                10
        );

        if (failedRecipients.isEmpty()) {
            logger.info("Welcome emails sent successfully to {} recipients", variables.size());
        } else {
            logger.warn("Some welcome emails failed: {}", failedRecipients);
        }

        return failedRecipients;
    }

    /**
     * Send Academic Warning Emails
     */
    public Map<String, String> sendWarningEmails(List<WarningInfo> warningInfos) throws IOException {
        if (warningInfos == null || warningInfos.isEmpty()) {
            throw new IllegalArgumentException("Warning list cannot be null or empty");
        }

        Map<String, Map<String, Object>> variables = new HashMap<>();

        for (WarningInfo warning : warningInfos) {
            String email = studentDAO.getEmailById(warning.getStudentId());
            if (email == null || email.isBlank()) {
                logger.warn("No email found for StudentID: {}", warning.getStudentId());
                continue;
            }

            Map<String, Object> emailVars = new HashMap<>();
            emailVars.put("name", warning.getStudentName());
            emailVars.put("warningType", warning.getWarningType());

            // Nếu có tên môn học (học lực hoặc vắng mặt)
            if (warning.getSubjectName() != null && !warning.getSubjectName().isBlank()) {
                emailVars.put("subject", warning.getSubjectName());
            }

            // Nếu có tỷ lệ vắng mặt > 0
            if (warning.getAbsentRate() > 0) {
                emailVars.put("absentRate", String.format("%.2f%%", warning.getAbsentRate() * 100));
            }

            // Nếu có học phí còn nợ
            if ("Unpaid Tuition".equals(warning.getWarningType()) && warning.getFeeMoney() != null && warning.getFeeMoney() > 0) {
                emailVars.put("feeAmount", String.format("$%.2f", warning.getFeeMoney()));
            }

            variables.put(email, emailVars);
        }

        if (variables.isEmpty()) {
            logger.error("No valid students to send warning emails");
            return Map.of("error", "No valid student warnings");
        }

        Map<String, String> failedRecipients = mailService.sendPersonalized(
                "warning", // template name (e.g., warning.html)
                "Academic Warning Notification", // subject
                variables,
                null,
                null,
                10 // max threads
        );

        if (failedRecipients.isEmpty()) {
            logger.info("Warning emails sent successfully to {} students", variables.size());
        } else {
            logger.warn("Some warning emails failed: {}", failedRecipients);
        }

        return failedRecipients;
    }

    /**
     * Shutdown Mail Service
     */
    public void shutdown() {
        mailService.shutdown();
    }

    public static void main(String[] args) {
        try {
            EmailTemplateService emailService = new EmailTemplateService(null);

            // Gửi email chào mừng
            ArrayList<String> emails = new ArrayList<>();
            emails.add("khainhce182286@fpt.edu.vn");
            emails.add("khai1234sd@gmail.com");
            emailService.sendWelcomeEmails(emails);

            // Nếu muốn test gửi cảnh báo, bỏ comment đoạn dưới:
            /*
            List<WarningInfo> warnings = new ArrayList<>();
            warnings.add(new WarningInfo(
                    "SE123456",
                    "Nguyen Van A",
                    "Database Systems",
                    8,
                    20,
                    0.4,
                    4.2,
                    "Banned from Exam"
            ));
            emailService.sendWarningEmails(warnings);
            */

            emailService.shutdown();

        } catch (IOException e) {
            System.err.println("⚠️ Error sending emails: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
