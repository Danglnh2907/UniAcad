package controller.api.student.mark;

import com.google.gson.Gson;
import dao.MarkReportDAO;
import dao.StudentDAO;
import model.database.Student;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.datasupport.MarkReport;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

@WebServlet(urlPatterns = "/api/student/markReport")
public class MarkReportApiServlet extends HttpServlet {

    private final MarkReportDAO dao = new MarkReportDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        if (session == null || session.getAttribute("email") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"error\": \"User is not logged in\"}");
            return;
        }

        String email = session.getAttribute("email").toString();

        if (email.isBlank()) {
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
        String termId = req.getParameter("termId");
        if (termId == null || termId.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Term ID is required\"}");
            return;
        }

        List<MarkReport> reports = dao.getMarkReportsByTermId(student.getStudentID(), termId);

        if (reports == null || reports.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"message\": \"No mark report found for term: " + termId + "\"}");
        } else {
            String json = gson.toJson(reports);
            resp.getWriter().write(json);
        }
    }

//    private String getCurrentTermID() {
//        Calendar cal = Calendar.getInstance();
//        int month = cal.get(Calendar.MONTH) + 1;
//        int year = cal.get(Calendar.YEAR) % 100;
//
//        if (month >= 1 && month <= 4) {
//            return "SP" + year;
//        } else if (month >= 5 && month <= 8) {
//            return "SU" + year;
//        } else if (month >= 9 && month <= 12) {
//            return "FA" + year;
//        }
//        return null;
//    }
}
