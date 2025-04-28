package controller.api.student.attendance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dao.AttendanceReportDAO;
import dao.StudentDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.database.Student;
import model.datasupport.AttendanceReport;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet(name = "StudentAttendanceApiServlet", urlPatterns = {"/api/student/attendance"})
public class StudentAttendanceApiServlet extends HttpServlet {

    private final AttendanceReportDAO attendanceDAO = new AttendanceReportDAO();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        StudentDAO studentDAO = new StudentDAO();
        String email = (String) request.getSession().getAttribute("email");

        if (email == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": -1, \"message\": \"Unauthorized\"}");
            return;
        }

        Student student = studentDAO.getStudentByEmail(email);
        if (student == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"error\": -1, \"message\": \"Student not found\"}");
            return;
        }

        List<AttendanceReport> report = attendanceDAO.getAttendanceReportsByEmail(student.getStudentEmail());

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(gson.toJson(report));

    }
}
