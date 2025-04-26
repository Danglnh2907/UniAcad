package controller.servlet.error;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Servlet for handling error requests at /error.
 * Forwards to error.html with error message from query parameter.
 */
@WebServlet(name = "ErrorServlet", value = "/error")
public class ErrorServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ErrorServlet.class);

    /**
     * Handle GET requests by forwarding to error.html with error message.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Get error message from query parameter
        String message = request.getParameter("message");
        if (message == null || message.isEmpty()) {
            message = "An error occurred. Please try again.";
        }

        // Log the error message
        logger.debug("Processing error request with message: {}", message);

        // Set message as request attribute for error.html
        request.setAttribute("message", message);

        try {
            // Forward to error.html
            request.getRequestDispatcher("/error.html").forward(request, response);
        } catch (ServletException | IOException e) {
            logger.error("Error forwarding to error.html: {}", e.getMessage(), e);
            // Fallback response if forwarding fails
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("""
                    <html>
                    <body>
                        <h1>Internal Server Error</h1>
                        <p>Unable to display error page. Please contact support.</p>
                    </body>
                    </html>
                    """);
        }
    }

    /**
     * Handle POST requests by redirecting to GET.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("POST request received for /error, redirecting to GET");
        doGet(request, response);
    }
}