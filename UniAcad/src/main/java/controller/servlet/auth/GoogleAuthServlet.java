package controller.servlet.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Servlet for handling login page requests at /google-oauth.
 * Forwards to Login.html, which contains the Google OAuth login button.
 */
@WebServlet(name = "GoogleAuthServlet", value = "/google-oauth")
public class GoogleAuthServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthServlet.class);
    private static final String LOGIN_PAGE = "Login.html"; // Đặt trong WEB-INF để bảo mật

    /**
     * Handle GET requests by forwarding to Login.html.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Forwarding to {} for /google-oauth", LOGIN_PAGE);
        try {
            request.getRequestDispatcher(LOGIN_PAGE).forward(request, response);
        } catch (ServletException | IOException e) {
            logger.error("Error forwarding to {}: {}", LOGIN_PAGE, e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to load login page");
        }
    }

    /**
     * Handle POST requests by redirecting to GET for simplicity.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("POST request received for /google-oauth, redirecting to GET");
        doGet(request, response);
    }
}