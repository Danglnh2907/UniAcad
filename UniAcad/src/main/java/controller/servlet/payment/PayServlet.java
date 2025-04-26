package controller.servlet.payment;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * A custom Servlet for handling HTTP requests.
 * Mapped to /payservlet by default.
 */
@WebServlet(
    name = "PayServlet",
    value = "/pay"
)
public class PayServlet extends HttpServlet {

    /**
     * Initialize the servlet.
     */
    @Override
    public void init() throws ServletException {
    }

    /**
     * Handle GET requests.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("Payment/index.html").forward(request, response);
    }

    /**
     * Handle POST requests.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("Payment/index.html").forward(request, response);
    }

    /**
     * Clean up resources.
     */
    @Override
    public void destroy() {
    }
}