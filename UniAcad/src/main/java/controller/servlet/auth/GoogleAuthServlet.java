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
 * Forwards to index1.html, which contains the Google OAuth login button.
 */
@WebServlet(name = "GoogleAuthServlet", value = "/google-oauth")
public class GoogleAuthServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthServlet.class);

    /**
     * Handle GET requests by forwarding to index1.html.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Forwarding to index1.html for /google-oauth");
        request.getRequestDispatcher("/index1.html").forward(request, response);
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