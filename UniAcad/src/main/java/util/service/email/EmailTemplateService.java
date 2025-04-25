package util.service.email;

import dao.StudentDAO;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.servlet.ServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A service for sending email templates, specifically welcome emails to students.
 * Uses MailService for email delivery and StudentDAO for retrieving student information.
 */
public class EmailTemplateService {
    private static final Logger logger = LoggerFactory.getLogger(EmailTemplateService.class);
    private final MailService mailService;
    private final StudentDAO studentDAO;
    private String appURL;

    /**
     * Constructor that initializes EmailTemplateService with a ServletContext.
     *
     * @param context the ServletContext for accessing web application resources
     */
    public EmailTemplateService(ServletContext context) {
        this.mailService = new MailService(context);
        this.studentDAO = new StudentDAO();
        Dotenv dotenv = Dotenv.configure()
                .filename("save.env")
                .ignoreIfMissing()
                .load();
        appURL = dotenv.get("APP_BASE_URL");
    }

    /**
     * Sends welcome emails to a list of student emails using the "welcome" template.
     * Each email includes the student's full name and a unique verification token.
     *
     * @param emails a list of student email addresses
     * @return a map of failed email addresses to their failure reasons; empty if all emails are sent successfully
     * @throws IOException if the template is invalid or resources cannot be loaded
     * @throws IllegalArgumentException if the email list is null or empty
     */
    public Map<String, String> sendWelcomeEmails(List<String> emails) throws IOException {
        if (emails == null || emails.isEmpty()) {
            throw new IllegalArgumentException("Email list cannot be null or empty");
        }

        // Map of email to template variables (name, token, verificationLink)
        Map<String, Map<String, Object>> variables = new HashMap<>();
        AtomicInteger tokenCounter = new AtomicInteger(1);

        // Process each email
        for (String email : emails) {
            // Check if email exists in Student table
            if (!studentDAO.checkEmailExists(email)) {
                logger.warn("Skipping email {}: not found in Student table", email);
                continue;
            }

            // Get student name
            String fullName = studentDAO.getNameByEmail(email);
            if (fullName == null || fullName.trim().isEmpty()) {
                logger.warn("Skipping email {}: could not retrieve student name", email);
                continue;
            }

            // Generate unique token (simple counter-based for demo; use UUID in production)
            String token = "WELCOME-" + tokenCounter.getAndIncrement() + "-" + UUID.randomUUID().toString().substring(0, 8);
            String verificationLink = appURL + "/verify?token=" + token;

            // Add variables for this email
            Map<String, Object> emailVariables = new HashMap<>();
            emailVariables.put("name", fullName);
            emailVariables.put("token", token);
            emailVariables.put("verificationLink", verificationLink);
            variables.put(email, emailVariables);

            logger.debug("Prepared welcome email for {}: name={}, token={}", email, fullName, token);
        }

        if (variables.isEmpty()) {
            logger.error("No valid recipients for welcome emails");
            return Map.of("error", "No valid student emails provided");
        }

        // Send emails using MailService
        Map<String, String> failedRecipients = mailService.sendPersonalized(
                "welcome", // Template name
                "Welcome to UniAcad!", // Email subject
                variables,
                null, // No inline images
                null, // No attachments
                10 // Batch size
        );

        if (failedRecipients.isEmpty()) {
            logger.info("Successfully sent welcome emails to {} recipients", variables.size());
        } else {
            logger.warn("Failed to send welcome emails to {} recipients: {}", failedRecipients.size(), failedRecipients);
        }

        return failedRecipients;
    }

    /**
     * Shuts down the underlying MailService, releasing resources.
     */
    public void shutdown() {
        mailService.shutdown();
    }

    /**
     * Demo main method for testing the EmailTemplateService.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            EmailTemplateService emailService = new EmailTemplateService(null);
            List<String> emails = List.of(
                    "khainhce182286@fpt.edu.vn",
                    "khai1234sd@gmail.com"
            );
            emailService.sendWelcomeEmails(emails);
            emailService.shutdown();
        } catch (IOException e) {
            logger.error("Failed to send welcome emails", e);
            System.out.println("Error: " + e.getMessage());
        }
    }
}