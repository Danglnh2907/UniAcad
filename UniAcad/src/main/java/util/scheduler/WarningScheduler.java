package util.scheduler;

import jakarta.ejb.Singleton;
import jakarta.ejb.Schedule;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.warning.WarningService;
import util.service.email.EmailTemplateService;
import jakarta.servlet.ServletContext;
import java.util.List;
import model.datasupport.WarningInfo;

@Singleton
public class WarningScheduler {

    private static final Logger logger = LoggerFactory.getLogger(WarningScheduler.class);

    @Inject
    private WarningService warningService;

    @Inject
    private ServletContext servletContext;  // inject servlet context để dùng trong EmailTemplateService

    @Schedule(hour = "*", minute = "0", persistent = false) // mỗi giờ quét một lần
    public void checkAndSendWarnings() {
        logger.info("[WarningScheduler] Running scheduled task to check warnings...");

        try {
            List<WarningInfo> warningList = warningService.getWarnings();
            if (warningList.isEmpty()) {
                logger.info("[WarningScheduler] No warning detected. Nothing to send.");
                return;
            }

            EmailTemplateService emailService = new EmailTemplateService(servletContext);
            emailService.sendWarningEmails(warningList);
            emailService.shutdown();

            logger.info("[WarningScheduler] Sent warning emails successfully to {} students.", warningList.size());
        } catch (Exception e) {
            logger.error("[WarningScheduler] Error during scheduled warning task: {}", e.getMessage(), e);
        }
    }
}
