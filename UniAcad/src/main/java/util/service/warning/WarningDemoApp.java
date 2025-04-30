package util.service.warning;

import util.service.email.EmailTemplateService;

public class WarningDemoApp {
    public static void main(String[] args) {
        try {
            // Khởi tạo thủ công các service
            WarningService warningService = new WarningService(); // JDBC-based service
            EmailTemplateService emailTemplateService = new EmailTemplateService(null); // null vì không cần ServletContext

            // Khởi tạo lớp xử lý chính
            WarningAutomationService automation = new WarningAutomationService();

            // Dùng reflection để inject 2 dependency (do không có container)
            inject(automation, "warningService", warningService);
            inject(automation, "emailTemplateService", emailTemplateService);

            // Gửi email cảnh báo
            automation.processWarnings();

            // Đóng service nếu cần (ví dụ: shutdown mail executor)
            automation.shutdown();

        } catch (Exception e) {
            System.err.println("Lỗi khi gửi cảnh báo học vụ: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Dùng reflection để inject trường private cho EJB (demo ngoài container)
     */
    private static void inject(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
