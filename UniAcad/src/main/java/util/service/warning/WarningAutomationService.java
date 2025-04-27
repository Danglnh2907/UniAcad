package util.service.warning;

import jakarta.ejb.Stateless;
import util.service.email.EmailTemplateService;
import model.datasupport.WarningInfo;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class WarningAutomationService {
    private final WarningService warningService;
    private final EmailTemplateService emailTemplateService;

    public WarningAutomationService() {
        this.warningService = new WarningService();
        this.emailTemplateService = new EmailTemplateService(null); // ServletContext nếu có
    }

    public void processWarnings() {
        List<WarningInfo> warnings = warningService.getWarnings();

        // Chia ra: chỉ gửi mail cho những bạn bị "Banned from Exam"
        List<WarningInfo> bannedWarnings = warnings.stream()
                .filter(w -> "Banned from Exam".equals(w.getWarningType()))
                .collect(Collectors.toList());

        try {
            if (!bannedWarnings.isEmpty()) {
                emailTemplateService.sendWarningEmails(bannedWarnings);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        emailTemplateService.shutdown();
    }
}
