package controller.api.warning;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import util.service.email.EmailTemplateService;
import util.service.warning.WarningAutomationService;
import util.service.warning.WarningService;

import java.io.IOException;

/**
 * API để gửi cảnh báo học vụ thủ công qua HTTP (GET).
 */
@WebServlet("/api/warning/trigger")
public class WarningTriggerServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain;charset=UTF-8");

        try {
            WarningService warningService = new WarningService();
            EmailTemplateService emailService = new EmailTemplateService(req.getServletContext());

            WarningAutomationService automation = new WarningAutomationService();
            automation.setWarningService(warningService);
            automation.setEmailTemplateService(emailService);

            automation.processWarnings();
            emailService.shutdown();

            resp.getWriter().println("Warnings processed successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().println("Failed to process warnings: " + e.getMessage());
        }
    }
}
