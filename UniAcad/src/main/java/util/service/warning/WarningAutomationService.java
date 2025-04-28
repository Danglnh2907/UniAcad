package util.service.warning;

import jakarta.ejb.Stateless;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.email.EmailTemplateService;
import model.datasupport.WarningInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@Stateless
public class WarningAutomationService {
    private static final Logger logger = LoggerFactory.getLogger(WarningAutomationService.class);
    private final WarningService warningService;
    private final EmailTemplateService emailTemplateService;

    public WarningAutomationService() {
        this.warningService = new WarningService();
        this.emailTemplateService = new EmailTemplateService(null); // ServletContext nếu có
    }

    public void processWarnings() {
        List<WarningInfo> warnings = warningService.getWarnings();

        // Chia ra: chỉ gửi mail cho những bạn bị "Banned from Exam"
        List<WarningInfo> bannedWarnings = new ArrayList<>();
        for (WarningInfo w : warnings) {
            if ("Banned from Exam".equals(w.getWarningType())) {
                bannedWarnings.add(w);
            }
        }
        try {
            if (!bannedWarnings.isEmpty()) {
                emailTemplateService.sendWarningEmails(bannedWarnings);
            }
        } catch (Exception e) {
            logger.error("Error sending warning emails: {}", e.getMessage(), e);
        }
    }

    public void shutdown() {
        emailTemplateService.shutdown();
    }
}
