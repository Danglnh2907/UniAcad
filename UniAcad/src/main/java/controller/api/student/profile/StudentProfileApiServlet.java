package controller.api.student.profile;

import com.google.gson.*;
import dao.StudentDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.database.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@WebServlet(name = "StudentProfileApiServlet", urlPatterns = {"/api/student/profile"})
public class StudentProfileApiServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(StudentProfileApiServlet.class);

    private final Gson gson;
    private final StudentDAO studentDAO = new StudentDAO();

    public StudentProfileApiServlet() {
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>)
                        (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()))
                .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>)
                        (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()))
                .registerTypeAdapter(LocalTime.class, (JsonSerializer<LocalTime>)
                        (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()))
                .create();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.info("Processing GET request for student profile API");
        response.setContentType("application/json");

        try {
            String studentEmail = (String) request.getSession().getAttribute("email");

            if (studentEmail == null) {
                sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: Please login first.");
                return;
            }

            Student student = studentDAO.getStudentByEmail(studentEmail);
            if (student == null) {
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Student profile not found.");
                return;
            }

            JsonObject successJson = new JsonObject();
            successJson.addProperty("error", 0);
            successJson.addProperty("message", "success");
            successJson.add("data", gson.toJsonTree(student));

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(successJson));

        } catch (Exception e) {
            logger.error("Error in StudentProfileApiServlet: ", e);
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
