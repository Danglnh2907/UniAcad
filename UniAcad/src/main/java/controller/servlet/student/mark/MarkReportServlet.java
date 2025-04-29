package controller.servlet.student.mark;

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
        name = "MarkReportServlet",
        value = "/student/grades"
)
public class MarkReportServlet extends HttpServlet {


    /**
     * Handle GET requests.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/student/MarkReport.html").forward(request, response);
    }

    /**
     * Handle POST requests.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    /**
     * Clean up resources.
     */
    @Override
    public void destroy() {
    }
}