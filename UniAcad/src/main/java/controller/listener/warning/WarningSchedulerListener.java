package controller.listener.warning;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import util.service.email.EmailTemplateService;
import util.service.warning.WarningAutomationService;
import util.service.warning.WarningService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Tự động gửi cảnh báo học vụ mỗi 10 phút khi ứng dụng web chạy.
 */
@WebListener
public class WarningSchedulerListener implements ServletContextListener {

    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        scheduler = Executors.newSingleThreadScheduledExecutor();

        // Tạo các service theo Java thuần (JDBC)
        WarningService warningService = new WarningService();
        EmailTemplateService emailService = new EmailTemplateService(null);

        // Khởi tạo và inject vào automation service
        WarningAutomationService automation = new WarningAutomationService();
        automation.setWarningService(warningService);
        automation.setEmailTemplateService(emailService);

        // Lên lịch chạy mỗi 10 phút
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("⏰ Đang xử lý cảnh báo học vụ (tự động)...");
                automation.processWarnings();
            } catch (Exception e) {
                System.err.println("❌ Lỗi khi gửi cảnh báo học vụ: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 10, TimeUnit.MINUTES);

        System.out.println("✅ WarningSchedulerListener đã khởi động (mỗi 10 phút).");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            System.out.println("🛑 WarningSchedulerListener đã dừng.");
        }
    }
}
