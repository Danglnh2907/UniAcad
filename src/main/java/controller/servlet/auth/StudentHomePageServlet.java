package controller.servlet.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * A custom Servlet for handling HTTP requests.
 * Mapped to /homepageservlet by default.
 */
@WebServlet(
        name = "HomePageServlet",
        value = "/student/homepage"
)
public class StudentHomePageServlet extends HttpServlet {
    /**
     * Handle GET requests.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("accound.jsp").forward(request, response);
    }
}