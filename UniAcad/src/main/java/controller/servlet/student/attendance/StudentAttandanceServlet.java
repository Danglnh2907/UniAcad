package controller.servlet.student.attendance;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * A custom Servlet for handling HTTP requests.
 * Mapped to /studentattandanceservlet by default.
 */
@WebServlet(
        name = "StudentAttandanceServlet",
        value = "/student/attendanceReport"
)
public class StudentAttandanceServlet extends HttpServlet {

    /**
     * Handle GET requests.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/student/StudentAttendance.html").forward(request, response);
    }

    /**
     * Handle POST requests.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}