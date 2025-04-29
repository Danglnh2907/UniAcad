package controller.api.student.mark;

import com.google.gson.Gson;
import dao.MarkReportDAO;
import dao.StudentDAO;
import model.database.Student;
import model.datasupport.MarkReport;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(urlPatterns = "/api/student/markReport")
public class MarkReportApiServlet extends HttpServlet {

    private final MarkReportDAO dao = new MarkReportDAO();
    private final StudentDAO studentDAO = new StudentDAO(); // Không cần tạo lại trong mỗi request
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        if (email == null || email.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Email is required\"}");
            return;
        }

        Student student = studentDAO.getStudentByEmail(email);
        if (student == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"error\": \"Student not found\"}");
            return;
        }

        List<MarkReport> reports = dao.getMarkReportsByEmail(email);

        if (reports == null || reports.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"message\": \"No mark report found.\"}");
        } else {
            String json = gson.toJson(reports);
            resp.getWriter().write(json);
        }
    }
}

