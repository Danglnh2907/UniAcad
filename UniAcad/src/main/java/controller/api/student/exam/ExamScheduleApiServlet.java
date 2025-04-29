package controller.api.student.exam;

import com.google.gson.*;
import dao.ExamDAO;
import dao.StudentDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.database.Student;
import model.datasupport.ExamSchedule;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@WebServlet(urlPatterns = "/api/student/examSchedule")
public class ExamScheduleApiServlet extends HttpServlet {

    private final ExamDAO examDAO = new ExamDAO();
    private final StudentDAO studentDAO = new StudentDAO();

    // ✅ Gson hỗ trợ LocalDateTime + Duration
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.toString()))
            .registerTypeAdapter(Duration.class, (JsonSerializer<Duration>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.toString()))
            .create();

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
        Student student = studentDAO.getStudentByEmail(email);

        if (student == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"error\": \"Student not found\"}");
            return;
        }

        List<ExamSchedule> exams = examDAO.getExamScheduleByStudentId(student.getStudentID());

        if (exams.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"message\": \"No exams scheduled.\"}");
        } else {
            String json = gson.toJson(exams);
            resp.getWriter().write(json);
        }
    }
}
