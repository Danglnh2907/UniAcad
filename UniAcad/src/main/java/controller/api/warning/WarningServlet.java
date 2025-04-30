package controller.api.warning;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.datasupport.WarningInfo;
import dao.WarningDAO;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/api/warnings")
public class WarningServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(WarningServlet.class.getName());

    private final WarningDAO warningDAO = new WarningDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("email") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("text/plain");
            resp.getWriter().write("❌ Unauthorized: Please log in.");
            return;
        }

        String email = session.getAttribute("email").toString();
        LOGGER.info("🔎 Fetching warnings for: " + email);

        List<WarningInfo> warnings;
        try {
            warnings = warningDAO.getWarningsByEmail(email);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Failed to fetch warnings", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain");
            resp.getWriter().write("❌ Server error: " + e.getMessage());
            return;
        }

        resp.setContentType("application/json;charset=UTF-8");
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(resp.getOutputStream(), warnings);
    }
}
