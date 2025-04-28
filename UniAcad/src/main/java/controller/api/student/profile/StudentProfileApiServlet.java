package controller.api.student.profile;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dao.StudentDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.database.Student;

import java.io.IOException;

@WebServlet(name = "StudentProfileApiServlet", urlPatterns = {"/api/student/profile"})
public class StudentProfileApiServlet extends HttpServlet {

    private final Gson gson = new Gson();
    private final StudentDAO studentDAO = new StudentDAO(); // JDBC thuần nha

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        try {
            // 1. Lấy email từ session
            String studentEmail = (String) request.getSession().getAttribute("studentEmail");

            if (studentEmail == null) {
                sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: Please login first.");
                return;
            }

            // 2. Query database tìm student
            Student student = studentDAO.getStudentByEmail(studentEmail);
            if (student == null) {
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Student profile not found.");
                return;
            }

            // 3. Trả về JSON student
            JsonObject successJson = new JsonObject();
            successJson.addProperty("error", 0);
            successJson.addProperty("message", "success");
            successJson.add("data", gson.toJsonTree(student));

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(successJson));

        } catch (Exception e) {
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    private void sendError(HttpServletResponse response, int statusCode, String message) throws IOException {
        JsonObject errorJson = new JsonObject();
        errorJson.addProperty("error", -1);
        errorJson.addProperty("message", message);
        errorJson.add("data", null);

        response.setStatus(statusCode);
        response.getWriter().write(gson.toJson(errorJson));
    }
}
