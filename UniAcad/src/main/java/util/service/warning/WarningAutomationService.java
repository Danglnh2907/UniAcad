package util.service.warning;

import model.datasupport.WarningInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.email.EmailTemplateService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dịch vụ tự động gửi cảnh báo học vụ.
 * Viết thuần Java, không phụ thuộc JPA hoặc CDI.
 */
public class WarningAutomationService {

    private static final Logger logger = LoggerFactory.getLogger(WarningAutomationService.class);

    private WarningService warningService;
    private EmailTemplateService emailTemplateService;

    public WarningAutomationService() {
        // Khởi tạo rỗng để cho phép inject bằng tay sau
    }

    public WarningAutomationService(WarningService warningService, EmailTemplateService emailTemplateService) {
        this.warningService = warningService;
        this.emailTemplateService = emailTemplateService;
    }

    public void setWarningService(WarningService warningService) {
        this.warningService = warningService;
    }

    public void setEmailTemplateService(EmailTemplateService emailTemplateService) {
        this.emailTemplateService = emailTemplateService;
    }

    /**
     * Xử lý cảnh báo và gửi email theo từng loại
     */
    public void processWarnings() {
        try {
            List<WarningInfo> warnings = warningService.getWarnings();

            if (warnings.isEmpty()) {
                logger.info("No warnings to process.");
                return;
            }

            Map<String, List<WarningInfo>> grouped = warnings.stream()
                    .collect(Collectors.groupingBy(WarningInfo::getWarningType));

            for (Map.Entry<String, List<WarningInfo>> entry : grouped.entrySet()) {
                String type = entry.getKey();
                List<WarningInfo> list = entry.getValue();

                if (list.isEmpty()) continue;

                logger.info("Sending {} warning emails for type: {}", list.size(), type);
                emailTemplateService.sendWarningEmails(list);
            }

        } catch (Exception e) {
            logger.error("Error processing warnings: {}", e.getMessage(), e);
        }
    }

    public void shutdown() {
        if (emailTemplateService != null) {
            emailTemplateService.shutdown();
        }
    }
}
